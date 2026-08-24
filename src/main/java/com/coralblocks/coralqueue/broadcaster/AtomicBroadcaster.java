/* 
 * Copyright 2015-2024 (c) CoralBlocks LLC - http://www.coralblocks.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package com.coralblocks.coralqueue.broadcaster;

import com.coralblocks.coralqueue.util.Builder;
import com.coralblocks.coralqueue.util.MathUtils;
import com.coralblocks.coralqueue.util.PaddedAtomicLong;

/*
 * The producer owns these fields while consumers access unrelated fields declared in AtomicBroadcaster. If they
 * occupied the same cache line, producer writes would invalidate the consumers' copies and consumer reads would
 * force the line to be shared again. This is false sharing. On HotSpot with 64-byte cache lines, the class hierarchy
 * and 56-byte padding on both sides keep those unrelated fields out of the producer's cache lines.
 */
abstract class AtomicBroadcasterProducerLhsPadding {
	long p01, p02, p03, p04, p05, p06, p07;
}

abstract class AtomicBroadcasterProducerFields extends AtomicBroadcasterProducerLhsPadding {
	long lastOfferedSeq = 0;
	long maxSeqBeforeWrapping;
}

abstract class AtomicBroadcasterProducerRhsPadding extends AtomicBroadcasterProducerFields {
	long p08, p09, p10, p11, p12, p13, p14;
}

/**
 * An implementation of a {@link Broadcaster} that uses <i>memory barriers</i> to synchronize producer and consumers sequences.
 * All messages are delivered to all consumers in the exact same order that they are sent by the producer.
 *
 * @param <E> The data transfer mutable object to be used by this broadcaster
 */
public class AtomicBroadcaster<E> extends AtomicBroadcasterProducerRhsPadding implements Broadcaster<E> {

	public static final int DEFAULT_CAPACITY = 1024;

	private final int capacity;
	private final int capacityMinusOne;
	private final E[] data;
	private final PaddedAtomicLong offerSequence = new PaddedAtomicLong(0);
	private final Cursor[] cursors;
	private final Consumer<E>[] consumers;

	/**
	 * Creates an <code>AtomicBroadcaster</code> with the given capacity and number of consumers using the given {@link Builder} to populate it.
	 * 
	 * @param capacity the capacity of the <code>AtomicBroadcaster</code>
	 * @param builder the {@link Builder} used to populate the <code>AtomicBroadcaster</code>
	 * @param numberOfConsumers the number of consumers that will use this <code>AtomicBroadcaster</code>
	 */
	@SuppressWarnings("unchecked")
	public AtomicBroadcaster(int capacity, Builder<E> builder, int numberOfConsumers) {
		MathUtils.ensurePowerOfTwo(capacity);
		if (numberOfConsumers <= 0) {
			throw new IllegalArgumentException("numberOfConsumers must be positive: " + numberOfConsumers);
		}
		this.capacity = capacity;
		this.capacityMinusOne = capacity - 1;
		this.data = (E[]) new Object[capacity];
		for (int i = 0; i < capacity; i++) {
			this.data[i] = builder.newInstance();
		}

		this.cursors = new Cursor[numberOfConsumers];
		this.consumers = (Consumer<E>[]) new Consumer[numberOfConsumers];
		
		// initialize cursors
		for(int i = 0; i < numberOfConsumers; i++) {
			cursors[i] = new Cursor();
			consumers[i] = new Consumer<E>(this, i);
		}
		
		this.maxSeqBeforeWrapping = calcMaxSeqBeforeWrapping();
	}

	/**
	 * Creates an <code>AtomicBroadcaster</code> with the default capacity (1024) and number of consumers using the given {@link Builder} to populate it.
	 * 
	 * @param builder the {@link Builder} used to populate the <code>AtomicBroadcaster</code>
	 * @param numberOfConsumers the number of consumers that will use this <code>AtomicBroadcaster</code>
	 */
	public AtomicBroadcaster(Builder<E> builder, int numberOfConsumers) {
		this(DEFAULT_CAPACITY, builder, numberOfConsumers);
	}
	
	/**
	 * Creates an <code>AtomicBroadcaster</code> with the default capacity (1024) and number of consumers using the given class to populate it.
	 * 
	 * @param klass the class used to populate the <code>AtomicBroadcaster</code>
	 * @param numberOfConsumers the number of consumers that will use this <code>AtomicBroadcaster</code>
	 */
	public AtomicBroadcaster(Class<E> klass, int numberOfConsumers) {
		this(Builder.createBuilder(klass), numberOfConsumers);
	}
	
	/**
	 * Creates an <code>AtomicBroadcaster</code> with the given capacity and number of consumers using the given class to populate it.
	 * 
	 * @param capacity the capacity of the <code>AtomicBroadcaster</code>
	 * @param klass the class used to populate the <code>AtomicBroadcaster</code>
	 * @param numberOfConsumers the number of consumers that will use this <code>AtomicBroadcaster</code>
	 */
	public AtomicBroadcaster(int capacity, Class<E> klass, int numberOfConsumers) {
		this(capacity, Builder.createBuilder(klass), numberOfConsumers);
	}
	
	@Override
	public final Consumer<E> getConsumer(int index) {
		checkConsumerIndex(index);
		return consumers[index];
	}

	private final void checkConsumerIndex(int index) {
		if (index < 0 || index >= consumers.length) {
			throw new IndexOutOfBoundsException("consumerIndex=" + index + ", numberOfConsumers=" + consumers.length);
		}
	}

	private final Cursor getCursor(int index) {
		checkConsumerIndex(index);
		return cursors[index];
	}
	
	@Override
	public final void clear() {
		lastOfferedSeq = 0;
		offerSequence.set(lastOfferedSeq);
		for(int i = 0; i < cursors.length; i++) {
			cursors[i].clear();
		}
		maxSeqBeforeWrapping = calcMaxSeqBeforeWrapping();
	}
	
	private final long minCursosFetchSeq() {
		long min = Long.MAX_VALUE;
		for(int i = 0; i < cursors.length; i++) {
			min = Math.min(cursors[i].getFetchSequence(), min);
		}
		return min;
	}
	
	@Override
	public final void disableConsumer(int index) {
		getCursor(index).disable();
	}
	
	private final long calcMaxSeqBeforeWrapping() {
		long minFetchSeq = minCursosFetchSeq();
		return minFetchSeq == Long.MAX_VALUE ? Long.MAX_VALUE : minFetchSeq + capacity;
	}
	
	private final int calcIndex(long value) {
		return (int) ((value - 1) & capacityMinusOne);
	}

	@Override
	public final E nextToDispatch() {
		if (++lastOfferedSeq > maxSeqBeforeWrapping) {
			// this would wrap the buffer... calculate the new one...
			this.maxSeqBeforeWrapping = calcMaxSeqBeforeWrapping();
			if (lastOfferedSeq > maxSeqBeforeWrapping) {
				lastOfferedSeq--;
				return null;				
			}
		}
		return data[calcIndex(lastOfferedSeq)];
	}

	@Override
	public final void flush(boolean lazySet) {
		if (lazySet) {
			offerSequence.lazySet(lastOfferedSeq);
		} else {
			offerSequence.set(lastOfferedSeq);
		}
	}
	
	@Override
	public final void flush() {
		flush(false);
	}

	@Override
	public final long availableToFetch(int consumer) {
		Cursor cursor = getCursor(consumer);
		if (cursor.isDisabled()) return 0;
		return offerSequence.get() - cursor.getLastFetchedSeq();
	}

	@Override
	public final E fetch(int consumer, boolean remove) {
		Cursor cursor = getCursor(consumer);
		cursor.ensureEnabled();
		if (remove) {
			cursor.incrementFetchCount();
			return data[calcIndex(cursor.incrementLastFetchedSeq())];
		} else {
			return data[calcIndex(cursor.getLastFetchedSeq() + 1)];
		}
	}
	
	@Override
	public final E fetch(int consumer) {
		return fetch(consumer, true);
	}

	@Override
	public final void doneFetching(int consumer, boolean lazySet) {
		Cursor cursor = getCursor(consumer);
		cursor.updateFetchSequence(lazySet);
		cursor.resetFetchCount();
	}
	
	@Override
	public final void rollBack(int consumer) {
		Cursor cursor = getCursor(consumer);
		rollBack(cursor, cursor.getFetchCount());
	}
	
	@Override
	public final void rollBack(int consumer, long count) {
		rollBack(getCursor(consumer), count);
	}

	private final void rollBack(Cursor cursor, long count) {
		cursor.ensureEnabled();
		if (count < 0 || count > cursor.getFetchCount()) {
			throw new IllegalArgumentException("Invalid rollback request! fetched=" + cursor.getFetchCount() + " requested=" + count);
		}
		cursor.decrementLastFetchedSeq(count);
		cursor.decrementFetchCount(count);
	}
	
	@Override
	public final void doneFetching(int consumer) {
		doneFetching(consumer, false);
	}
	
	@Override
	public final int getNumberOfConsumers() {
		return cursors.length;
	}
}
