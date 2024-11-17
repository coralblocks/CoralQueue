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
	
	private final static int DEFAULT_CAPACITY = 1024;

	private final int capacity;
	private final int capacityMinusOne;
	private final E[] data;
	private long lastOfferedSeq = 0;
	private long lastPolledSeq = 0;
	private long pollCount = 0;
	private long maxSeqBeforeWrapping;
	private final PaddedAtomicLong offerSequence = new PaddedAtomicLong(0);
	private final PaddedAtomicLong pollSequence = new PaddedAtomicLong(0);
	
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
		lastPolledSeq = 0;
		pollCount = 0;
		offerSequence.set(lastOfferedSeq);
		pollSequence.set(lastPolledSeq);
		maxSeqBeforeWrapping = calcMaxSeqBeforeWrapping();
	}
	
	private final long calcMaxSeqBeforeWrapping() {
		return pollSequence.get() + capacity;
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
	public final long availableToPoll() {
		return offerSequence.get() - lastPolledSeq;
	}
	
	@Override
	public final E poll() {
		pollCount++;
		return data[calcIndex(++lastPolledSeq)];
	}
	
	@Override
	public final void replace(E newVal) {
		data[calcIndex(lastPolledSeq)] = newVal;
	}
	
	@Override
	public final E peek() {
		return data[calcIndex(lastPolledSeq)];
	}

	@Override
	public final void donePolling(boolean lazySet) {
		if (lazySet) {
			pollSequence.lazySet(lastPolledSeq);
		} else {
			pollSequence.set(lastPolledSeq);
		}
		pollCount = 0;
	}
	
	@Override
	public final void rollBack() {
		rollBack(pollCount);
	}
	
	@Override
	public final void rollBack(long count) {
		if (count < 0 || count > pollCount) {
			throw new RuntimeException("Invalid rollback request! polled=" + pollCount + " requested=" + count);
		}
		lastPolledSeq -= count;
		pollCount -= count;
	}
	
	@Override
	public final void donePolling() {
		// don't call donePolling(false) to save one method call (more performance)
		pollSequence.set(lastPolledSeq); // no lazySet by default...
		pollCount = 0;
	}
}