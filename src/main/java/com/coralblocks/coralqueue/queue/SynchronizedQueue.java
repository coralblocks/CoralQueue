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

import java.util.concurrent.locks.LockSupport;

import com.coralblocks.coralqueue.util.Builder;
import com.coralblocks.coralqueue.util.MathUtils;

/**
 * This synchronized implementation of {@link Queue} is provided for illustrative purposes and for performance comparison. It uses two locks instead of <i>memory barriers</i>,
 * one for the consumer and one for the producer, to maintain the sequence numbers.
 * 
 * @param <E> The data transfer mutable object to be used by this queue
 */
public class SynchronizedQueue<E> implements Queue<E> {
	
	private final static int DEFAULT_CAPACITY = 1024;

	private final int capacity;
	private final E[] data;
	private long nextOfferValue = -1;
	private long nextPollValue = -1;
	private long pollCounter = 0;
	private long offeringSequence = -1;
	private long pollingSequence = -1;
	private final Object offeringLock = new Object();
	private final Object pollingLock = new Object();
	private long maxSeq;

	/**
	 * Creates a <code>SynchronizedQueue</code> with the given capacity using the given {@link Builder} to populate it.
	 * 
	 * @param capacity the capacity of the <code>SynchronizedQueue</code>
	 * @param builder the {@link Builder} used to populate the <code>SynchronizedQueue</code>
	 */
	@SuppressWarnings("unchecked")
	public SynchronizedQueue(int capacity, Builder<E> builder) {
		MathUtils.ensurePowerOfTwo(capacity);
		this.capacity = capacity;
		this.data = (E[]) new Object[capacity];
		for (int i = 0; i < capacity; i++) {
			this.data[i] = builder.newInstance();
		}
		this.maxSeq = findMaxSeqBeforeWrapping();
	}
	
	/**
	 * Creates a <code>SynchronizedQueue</code> with the default capacity (1024) using the given {@link Builder} to populate it.
	 * 
	 * @param builder the {@link Builder} used to populate the <code>SynchronizedQueue</code>
	 */
	public SynchronizedQueue(Builder<E> builder) {
		this(DEFAULT_CAPACITY, builder);
	}
	
	/**
	 * Creates a <code>SynchronizedQueue</code> with the given capacity using the given class to populate it.
	 * 
	 * @param capacity the capacity of the <code>SynchronizedQueue</code>
	 * @param klass the class used to populate the <code>SynchronizedQueue</code>
	 */
	public SynchronizedQueue(int capacity, Class<E> klass) {
		this(capacity, Builder.createBuilder(klass));
	}
	
	/**
	 * Creates a <code>SynchronizedQueue</code> with the default capacity (1024) using the given class to populate it.
	 * 
	 * @param klass the class used to populate the <code>SynchronizedQueue</code>
	 */
	public SynchronizedQueue(Class<E> klass) {
		this(Builder.createBuilder(klass));
	}

	@Override
	public final void clear() {
		nextOfferValue = -1;
		nextPollValue = -1;
		pollCounter = 0;
		offeringSequence = -1;
		pollingSequence = -1;
		maxSeq = findMaxSeqBeforeWrapping();
	}
	
	private final long findMaxSeqBeforeWrapping() {
		synchronized(pollingLock) {
			return capacity + pollingSequence;
		}
	}

	@Override
	public final E nextToDispatch() {
		LockSupport.parkNanos(10000); // make this slower on purpose...
		long index = ++nextOfferValue;
		if (index <= maxSeq) {
			// we are safe
		} else {
			// recalculate max sequence...
			this.maxSeq = findMaxSeqBeforeWrapping();
			if (index > maxSeq) {
				// we are still ahead... nothing to do for now...
				nextOfferValue--;
				return null;
			}
		}
		return data[(int) (index & capacity - 1)];
	}
	
	@Override
	public final E nextToDispatch(E swap) {
		E val = nextToDispatch();
		if (val == null) return null;
		data[(int) (nextOfferValue & capacity - 1)] = swap;
		return val;
	}
	
	@Override
	public final void flush(boolean lazySet) {
		synchronized (offeringLock) {
			offeringSequence = nextOfferValue;
		}
	}

	@Override
	public final long availableToPoll() {
		synchronized (offeringLock) {
			return offeringSequence - nextPollValue;
		}
	}
	
	@Override
	public final void flush() {
		flush(false);
	}

	@Override
	public final E poll() {
		LockSupport.parkNanos(10000);
		pollCounter++;
		return data[(int) (++nextPollValue & capacity - 1)];
	}
	
	@Override
	public final void replace(E newVal) {
		data[(int) (nextPollValue & capacity - 1)] = newVal;
	}
	
	@Override
	public final E peek() {
		LockSupport.parkNanos(10000);
		return data[(int) (nextPollValue & capacity - 1)];
	}

	@Override
	public final void donePolling(boolean lazySet) {
		synchronized (pollingLock) {
			pollingSequence = nextPollValue;
		}
		pollCounter = 0;
	}
	
	@Override
	public final void donePolling() {
		donePolling(false);
	}
	
	@Override
	public final void rollBack() {
		rollBack(pollCounter);
	}
	
	@Override
	public final void rollBack(long count) {
		if (count < 0 || count > pollCounter) {
			throw new RuntimeException("Invalid rollback request! polled=" + pollCounter + " requested=" + count);
		}
		nextPollValue -= count;
		pollCounter -= count;
	}
}