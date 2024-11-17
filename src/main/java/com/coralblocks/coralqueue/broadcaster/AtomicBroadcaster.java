/* 
 * Copyright 2024 (c) CoralBlocks - http://www.coralblocks.com
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

/**
 * An implementation of a {@link Broadcaster} that uses <i>memory barriers</i> to synchronize producer and consumers sequences.
 * All messages are delivered to all consumers in the exact same order that they are sent by the producer.
 *
 * @param <E> The data transfer mutable object to be used by this broadcaster
 */
public class AtomicBroadcaster<E> implements Broadcaster<E> {

	private final static int DEFAULT_CAPACITY = 1024;

	private final int capacity;
	private final int capacityMinusOne;
	private final E[] data;
	private long lastOfferedSeq = 0;
	private long maxSeqBeforeWrapping;
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
		if (index >= consumers.length) {
			throw new RuntimeException("Tried to get a consumer with a bad index: " + index);
		}
		return consumers[index];
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
	
	private final long minCursorPollSeq() {
		long min = Long.MAX_VALUE;
		for(int i = 0; i < cursors.length; i++) {
			min = Math.min(cursors[i].getPollSequence(), min);
		}
		return min;
	}
	
	@Override
	public final void disableConsumer(int index) {
		cursors[index].setPollSequenceToMax();
	}
	
	private final long calcMaxSeqBeforeWrapping() {
		return minCursorPollSeq() + capacity;
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
	public final long availableToPoll(int consumer) {
		return offerSequence.get() - cursors[consumer].getLastPolledSeq();
	}

	@Override
	public final E poll(int consumer) {
		Cursor cursor = cursors[consumer];
		cursor.incrementPollCount();
		return data[calcIndex(cursor.incrementLastPolledSeq())];
	}
	
	@Override
	public final E peek(int consumer) {
		return data[calcIndex(cursors[consumer].getLastPolledSeq())];
	}

	@Override
	public final void donePolling(int consumer, boolean lazySet) {
		Cursor cursor = cursors[consumer];
		cursor.updatePollSequence(lazySet);
		cursor.resetPollCount();
	}
	
	@Override
	public final void rollBack(int consumer) {
		rollBack(consumer, cursors[consumer].getPollCount());
	}
	
	@Override
	public final void rollBack(int consumer, long count) {
		Cursor cursor = cursors[consumer];
		if (count < 0 || count > cursor.getPollCount()) {
			throw new RuntimeException("Invalid rollback request! polled=" + cursor.getPollCount() + " requested=" + count);
		}
		cursor.decrementLastPolledSeq(count);
		cursor.decrementPollCount(count);
	}
	
	@Override
	public final void donePolling(int consumer) {
		donePolling(consumer, false);
	}
	
	@Override
	public final int getNumberOfConsumers() {
		return cursors.length;
	}
}