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
package com.coralblocks.coralqueue.demultiplexer;

import com.coralblocks.coralqueue.queue.AtomicQueue;
import com.coralblocks.coralqueue.util.Builder;
import com.coralblocks.coralqueue.util.MathUtils;

/**
 * An implementation of {@link Demultiplexer} that uses <i>memory barriers</i> to synchronize producer and consumers sequences.
 * Two different consumers will never poll the same message.
 *
 * @param <E> The mutable transfer object to be used by this demultiplexer
 */
public class AtomicDemultiplexer<E> implements Demultiplexer<E> {
	
	private final static int DEFAULT_CAPACITY = 1024;

	private final AtomicQueue<E>[] queues;
	private final int numberOfConsumers;
	private int currQueueToDispatch = 0;
	private boolean[] needsToFlush;
	private int currConsumerIndex = 0;
	private final Consumer<E>[] consumers;

	/**
	 * Creates an <code>AtomicDemultiplexer</code> with the given capacity and number of consumers using the given {@link Builder} to populate it.
	 * 
	 * @param capacity the capacity of the <code>AtomicDemultiplexer</code>
	 * @param builder the {@link Builder} used to populate the <code>AtomicDemultiplexer</code>
	 * @param numberOfConsumers the number of consumers that will use this <code>AtomicDemultiplexer</code>
	 */
	@SuppressWarnings("unchecked")
	public AtomicDemultiplexer(int capacity, Builder<E> builder, int numberOfConsumers) {
		MathUtils.ensurePowerOfTwo(capacity);
		this.numberOfConsumers = numberOfConsumers;
		this.queues = new AtomicQueue[numberOfConsumers];
		this.needsToFlush = new boolean[numberOfConsumers];
		this.consumers = (Consumer<E>[]) new Consumer[numberOfConsumers];
		for(int i = 0; i < queues.length; i++) {
			this.queues[i] = new AtomicQueue<E>(capacity, builder);
			this.needsToFlush[i] = false;
			this.consumers[i] = new Consumer<E>(this, i);
		}
	}

	/**
	 * Creates an <code>AtomicDemultiplexer</code> with the default capacity (1024) and number of consumers using the given {@link Builder} to populate it.
	 * 
	 * @param builder the {@link Builder} used to populate the <code>AtomicDemultiplexer</code>
	 * @param numberOfConsumers the number of consumers that will use this <code>AtomicDemultiplexer</code>
	 */
	public AtomicDemultiplexer(Builder<E> builder, int numberOfConsumers) {
		this(DEFAULT_CAPACITY, builder, numberOfConsumers);
	}
	
	/**
	 * Creates an <code>AtomicDemultiplexer</code> with the given capacity and number of consumers using the given class to populate it.
	 * 
	 * @param capacity the capacity of the <code>AtomicDemultiplexer</code>
	 * @param klass the class used to populate the <code>AtomicDemultiplexer</code>
	 * @param numberOfConsumers the number of consumers that will use this <code>AtomicDemultiplexer</code>
	 */
	public AtomicDemultiplexer(int capacity, Class<E> klass, int numberOfConsumers) {
		this(capacity, Builder.createBuilder(klass), numberOfConsumers);
	}
	
	/**
	 * Creates an <code>AtomicDemultiplexer</code> with the default capacity (1024) and number of consumers using the given class to populate it.
	 * 
	 * @param klass the class used to populate the <code>AtomicDemultiplexer</code>
	 * @param numberOfConsumers the number of consumers that will use this <code>AtomicDemultiplexer</code>
	 */
	public AtomicDemultiplexer(Class<E> klass, int numberOfConsumers) {
		this(Builder.createBuilder(klass), numberOfConsumers);
	}
	
	@Override
	public Consumer<E> nextConsumer() {
		synchronized(consumers) {
			if (currConsumerIndex == numberOfConsumers) return null;
			return consumers[currConsumerIndex++];
		}
	}
	
	@Override
	public Consumer<E> getConsumer(int index) {
		if (index >= numberOfConsumers) {
			throw new RuntimeException("Tried to get a consumer with a bad index: " + index);
		}
		return consumers[index];
	}
	
	@Override
	public final void clear() {
		currQueueToDispatch = 0;
		for(int i = 0; i < queues.length; i++) {
			queues[i].clear();
		}
		for(int i = 0; i < needsToFlush.length; i++) {
			needsToFlush[i] = false;
		}
	}
	
	@Override
	public final E nextToDispatch() {
		int count = 0;
		while(count++ < numberOfConsumers) {
			E e = queues[currQueueToDispatch].nextToDispatch();
			if (e != null) {
				needsToFlush[currQueueToDispatch] = true;
				if (++currQueueToDispatch == numberOfConsumers) currQueueToDispatch = 0;
				return e;
			} else {
				if (++currQueueToDispatch == numberOfConsumers) currQueueToDispatch = 0;
				// try the next one until tried numberOfConsumers...
			}
		}
		return null;
	}
	
	@Override
	public final E nextToDispatch(int consumerIndex) {
		
		if (consumerIndex < 0) return nextToDispatch(); // fall back to regular implementation...
		
		if (consumerIndex >= numberOfConsumers) {
			throw new RuntimeException("Bad consumerIndex: " + consumerIndex + " numberOfConsumers=" + numberOfConsumers);
		}
		
		E e = queues[consumerIndex].nextToDispatch();
		if (e != null) {
			needsToFlush[consumerIndex] = true;
			return e;
		}
		return null;
	}
	
	@Override
	public final void flush(boolean lazySet) {
		for(int i = 0; i < numberOfConsumers; i++) {
			if (needsToFlush[i]) {
				queues[i].flush(lazySet);
				needsToFlush[i] = false;
			}
		}
	}
	
	@Override
	public final void flush() {
		flush(false);
	}

	@Override
	public final long availableToPoll(int consumer) {
		return queues[consumer].availableToPoll();
	}
	
	@Override
	public final E poll(int consumer) {
		return queues[consumer].poll();
	}
	
	@Override
	public final void replace(int consumer, E newVal) {
		queues[consumer].replace(newVal);
	}
	
	@Override
	public final void donePolling(int consumer, boolean lazySet) {
		queues[consumer].donePolling(lazySet);
	}
	
	@Override
	public final void donePolling(int consumer) {
		queues[consumer].donePolling(false);
	}

	@Override
    public int getNumberOfConsumers() {
	    return numberOfConsumers;
    }
}