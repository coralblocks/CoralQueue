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
package com.coralblocks.coralqueue.queue;

import com.coralblocks.coralqueue.util.Builder;
import com.coralblocks.coralqueue.util.MathUtils;
import com.coralblocks.coralqueue.util.PaddedAtomicLong;

/**
 * An implementation of {@link Queue} that uses <i>memory barriers</i> to synchronize producer and consumer sequences.
 *
 * @param <E> The data transfer mutable object to be used by this queue
 */
public class AtomicQueue<E> implements Queue<E> {
	
	private static final int DEFAULT_CAPACITY = 1024;

	private final int capacity;
	private final int capacityMinusOne;
	private final E[] data;
	private long lastOfferedSeq = 0;
	private long lastFetchedSeq = 0;
	private long fetchCount = 0;
	private long maxSeqBeforeWrapping;
	private final PaddedAtomicLong offerSequence = new PaddedAtomicLong(0);
	private final PaddedAtomicLong fetchSequence = new PaddedAtomicLong(0);
	
	private final Builder<E> builder;

	/**
	 * Creates an <code>AtomicQueue</code> with the given capacity using the given {@link Builder} to populate it.
	 * 
	 * @param capacity the capacity of the <code>AtomicQueue</code>
	 * @param builder the {@link Builder} used to populate the <code>AtomicQueue</code>
	 */
	@SuppressWarnings("unchecked")
	public AtomicQueue(int capacity, Builder<E> builder) {
		MathUtils.ensurePowerOfTwo(capacity);
		this.capacity = capacity;
		this.capacityMinusOne = capacity - 1;
		this.data = (E[]) new Object[capacity];
		for (int i = 0; i < capacity; i++) {
			this.data[i] = builder.newInstance();
		}
		this.maxSeqBeforeWrapping = calcMaxSeqBeforeWrapping();
		this.builder = builder;
	}

	/**
	 * Creates an <code>AtomicQueue</code> with the default capacity (1024) using the given {@link Builder} to populate it.
	 * 
	 * @param builder the {@link Builder} used to populate the <code>AtomicQueue</code>
	 */
	public AtomicQueue(Builder<E> builder) {
		this(DEFAULT_CAPACITY, builder);
	}

	/**
	 * Creates an <code>AtomicQueue</code> with the given capacity using the given class to populate it.
	 * 
	 * @param capacity the capacity of the <code>AtomicQueue</code>
	 * @param klass the class used to populate the <code>AtomicQueue</code>
	 */
	public AtomicQueue(int capacity, Class<E> klass) {
		this(capacity, Builder.createBuilder(klass));
	}
	
	/**
	 * Creates an <code>AtomicQueue</code> with the default capacity (1024) using the given class to populate it.
	 * 
	 * @param klass the class used to populate the <code>AtomicQueue</code>
	 */
	public AtomicQueue(Class<E> klass) {
		this(Builder.createBuilder(klass));
	}
	
	@Override
	public final void clear() {
		lastOfferedSeq = 0;
		lastFetchedSeq = 0;
		fetchCount = 0;
		offerSequence.set(lastOfferedSeq);
		fetchSequence.set(lastFetchedSeq);
		maxSeqBeforeWrapping = calcMaxSeqBeforeWrapping();
	}
	
	private final long calcMaxSeqBeforeWrapping() {
		return fetchSequence.get() + capacity;
	}
	
	public final Builder<E> getBuilder() {
		return builder;
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
	public final E nextToDispatch(E swap) {
		E val = nextToDispatch();
		if (val == null) return null;
		data[calcIndex(lastOfferedSeq)] = swap;
		return val;
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
		// don't call flush(false) to save one method call (more performance)
		offerSequence.set(lastOfferedSeq); // no lazySet by default...
	}

	@Override
	public final long availableToFetch() {
		return offerSequence.get() - lastFetchedSeq;
	}
	
	@Override
	public final E fetch(boolean remove) {
		if (remove) {
			fetchCount++;
			return data[calcIndex(++lastFetchedSeq)];
		} else {
			return data[calcIndex(lastFetchedSeq + 1)];
		}
	}
	
	@Override
	public final E fetch() {
		return fetch(true);
	}
	
	@Override
	public final void replace(E newVal) {
		data[calcIndex(lastFetchedSeq)] = newVal;
	}
	
	@Override
	public final void doneFetching(boolean lazySet) {
		if (lazySet) {
			fetchSequence.lazySet(lastFetchedSeq);
		} else {
			fetchSequence.set(lastFetchedSeq);
		}
		fetchCount = 0;
	}
	
	@Override
	public final void rollBack() {
		rollBack(fetchCount);
	}
	
	@Override
	public final void rollBack(long count) {
		if (count < 0 || count > fetchCount) {
			throw new RuntimeException("Invalid rollback request! fetched=" + fetchCount + " requested=" + count);
		}
		lastFetchedSeq -= count;
		fetchCount -= count;
	}
	
	@Override
	public final void doneFetching() {
		// don't call doneFetching(false) to save one method call (more performance)
		fetchSequence.set(lastFetchedSeq); // no lazySet by default...
		fetchCount = 0;
	}
}