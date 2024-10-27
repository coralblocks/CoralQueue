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

import java.util.Random;
import java.util.concurrent.locks.LockSupport;

import com.coralblocks.coralqueue.util.Builder;
import com.coralblocks.coralqueue.util.MathUtils;

public class SynchronizedBroadcaster<E> implements Broadcaster<E> {

	private final static int DEFAULT_CAPACITY = 1024 * 16;

	private final int capacity;
	private final int capacityMinusOne;
	private final E[] data;
	private long lastOfferedSeq = -1;
	private long maxSeqBeforeWrapping;
	private long offerSequence = -1;
	private final Cursor[] cursors;
	private final Random random = new Random();
	private final Consumer<E>[] consumers;

	@SuppressWarnings("unchecked")
	public SynchronizedBroadcaster(int capacity, Builder<E> builder, int numberOfConsumers) {
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
	
	@Override
	public final void clear() {
		lastOfferedSeq = -1;
		offerSequence = -1;
		// initialize cursors
		for(int i = 0; i < cursors.length; i++) {
			cursors[i].clear();
		}
		maxSeqBeforeWrapping = calcMaxSeqBeforeWrapping();
	}

	public SynchronizedBroadcaster(Builder<E> builder, int numberOfConsumers) {
		this(DEFAULT_CAPACITY, builder, numberOfConsumers);
	}
	
	public SynchronizedBroadcaster(Class<E> klass, int numberOfConsumers) {
		this(Builder.createBuilder(klass), numberOfConsumers);
	}
	
	public SynchronizedBroadcaster(int capacity, Class<E> klass, int numberOfConsumers) {
		this(capacity, Builder.createBuilder(klass), numberOfConsumers);
	}
	
	private final long minCursorPollSeq() {
		long min = Long.MAX_VALUE;
		for(int i = 0; i < cursors.length; i++) {
			min = Math.min(cursors[i].getPollSequence(), min);
		}
		return min;
	}
	
	@Override
	public final Consumer<E> getConsumer(int index) {
		if (index >= consumers.length) {
			throw new RuntimeException("Tried to get a consumer with a bad index: " + index);
		}
		return consumers[index];
	}
	
	@Override
	public synchronized final void disableConsumer(int index) {
		cursors[index].setPollSequenceToMax();
	}
	
	private final long calcMaxSeqBeforeWrapping() {
		return minCursorPollSeq() + capacity;
	}

	@Override
	public synchronized final E nextToDispatch() {
		if (random.nextInt(4) == 2) LockSupport.parkNanos(1000);
		if (++lastOfferedSeq > maxSeqBeforeWrapping) {
			// this would wrap the buffer... calculate the new one...
			this.maxSeqBeforeWrapping = calcMaxSeqBeforeWrapping();
			if (lastOfferedSeq > maxSeqBeforeWrapping) {
				lastOfferedSeq--;
				return null;				
			}
		}
		return data[(int) (lastOfferedSeq & capacityMinusOne)];
	}

	@Override
	public synchronized final void flush(boolean lazySet) {
		offerSequence = lastOfferedSeq;
	}
	
	@Override
	public synchronized final void flush() {
		flush(false);
	}

	@Override
	public synchronized final long availableToPoll(int consumer) {
		return offerSequence- cursors[consumer].getLastPolledSeq();
	}

	@Override
	public synchronized final E poll(int consumer) {
		if (random.nextInt(4) == 2) LockSupport.parkNanos(1000);
		Cursor cursor = cursors[consumer];
		cursor.incrementPollCount();
		return data[(int) (cursor.incrementLastPolledSeq() & capacityMinusOne)];
	}
	
	@Override
	public synchronized final E peek(int consumer) {
		if (random.nextInt(4) == 2) LockSupport.parkNanos(1000);
		return data[(int) (cursors[consumer].getLastPolledSeq() & capacityMinusOne)];
	}

	@Override
	public synchronized final void donePolling(int consumer, boolean lazySet) {
		Cursor cursor = cursors[consumer];
		cursor.updatePollSequence(lazySet);
		cursor.resetPollCount();
	}
	
	@Override
	public synchronized final void rollBack(int consumer) {
		rollBack(consumer, cursors[consumer].getPollCount());
	}
	
	@Override
	public synchronized final void rollBack(int consumer, long count) {
		Cursor cursor = cursors[consumer];
		if (count < 0 || count > cursor.getPollCount()) {
			throw new RuntimeException("Invalid rollback request! polled=" + cursor.getPollCount() + " requested=" + count);
		}
		cursor.decrementLastPolledSeq(count);
		cursor.decrementPollCount(count);
	}
	
	@Override
	public synchronized final void donePolling(int consumer) {
		donePolling(consumer, false);
	}
	
	@Override
	public final int getNumberOfConsumers() {
		return cursors.length;
	}
}