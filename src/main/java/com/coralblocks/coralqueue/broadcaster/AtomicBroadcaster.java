/*
* Copyright (c) CoralBlocks LLC (c) 2017
 */
package com.coralblocks.coralqueue.broadcaster;

import com.coralblocks.coralqueue.util.Builder;
import com.coralblocks.coralqueue.util.MathUtils;
import com.coralblocks.coralqueue.util.PaddedAtomicLong;

public class AtomicBroadcaster<E> implements Broadcaster<E> {

	private final static int DEFAULT_CAPACITY = 1024;

	private final int capacity;
	private final int capacityMinusOne;
	private final E[] data;
	private long lastOfferedSeq = -1;
	private long maxSeqBeforeWrapping;
	private final PaddedAtomicLong offerSequence = new PaddedAtomicLong(-1);
	private final Cursor[] cursors;
	private int currConsumerIndex = 0;
	private final Consumer<E>[] consumers;

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

	public AtomicBroadcaster(Builder<E> builder, int numberOfConsumers) {
		this(DEFAULT_CAPACITY, builder, numberOfConsumers);
	}
	
	public AtomicBroadcaster(Class<E> klass, int numberOfConsumers) {
		this(Builder.createBuilder(klass), numberOfConsumers);
	}
	
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
	public final Consumer<E> nextConsumer() {
		synchronized(consumers) {
			if (currConsumerIndex == consumers.length) return null;
			return consumers[currConsumerIndex++];
		}
	}
	
	@Override
	public final void clear() {
		lastOfferedSeq = -1;
		offerSequence.set(-1);
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
		return data[(int) (lastOfferedSeq & capacityMinusOne)];
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
		return data[(int) (cursor.incrementLastPolledSeq() & capacityMinusOne)];
	}
	
	@Override
	public final E peek(int consumer) {
		return data[(int) (cursors[consumer].getLastPolledSeq() & capacityMinusOne)];
	}

	@Override
	public final void donePolling(int consumer, boolean lazySet) {
		Cursor cursor = cursors[consumer];
		cursor.updatePollSequence(lazySet);
		cursor.resetPollCount();
	}
	
	@Override
	public final void rollback(int consumer) {
		rollback(consumer, cursors[consumer].getPollCount());
	}
	
	@Override
	public final void rollback(int consumer, long count) {
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
	
	static {
		
		try {
			Class.forName("com.coralblocks.coralqueue.AtomicQueue");
		} catch(Exception e) {
			// NOOP
		}
	}
}