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
package com.coralblocks.coralqueue.multiplexer;

import com.coralblocks.coralqueue.queue.AtomicQueue;
import com.coralblocks.coralqueue.queue.Queue;
import com.coralblocks.coralqueue.util.Builder;

/**
 * An implementation of {@link Multiplexer} that uses <i>memory barriers</i> to synchronize producers and consumer sequences.
 *
 * @param <E> The mutable transfer object to be used by this multiplexer
 */
public class AtomicMultiplexer<E> implements Multiplexer<E> {
	
	private final int numberOfProducers;
	private final Queue<E>[] queues;
	private final long[] avail;
	private int producerIndex = 0;
	private int currProducerIndex = 0;
	private final Producer<E>[] producers;
	
	/**
	 * Creates an <code>AtomicMultiplexer</code> with the given capacity and number of producers using the given {@link Builder} to populate it.
	 * 
	 * @param capacity the capacity of the <code>AtomicMultiplexer</code>
	 * @param builder the {@link Builder} used to populate the <code>AtomicMultiplexer</code>
	 * @param numberOfProducers the number of producers that will use this <code>AtomicMultiplexer</code>
	 */
	@SuppressWarnings("unchecked")
    public AtomicMultiplexer(int capacity, Builder<E> builder, int numberOfProducers) {
		this.numberOfProducers = numberOfProducers;
		this.queues = (Queue<E>[]) new AtomicQueue[numberOfProducers];
		this.producers = (Producer<E>[]) new Producer[numberOfProducers];
		this.avail = new long[numberOfProducers];
		for(int i = 0; i < numberOfProducers; i++) {
			queues[i] = new AtomicQueue<E>(capacity, builder);
			producers[i] = new Producer<E>(this, i);
			avail[i] = -1;
		}
	}
	
	/**
	 * Creates an <code>AtomicMultiplexer</code> with the given capacity and number of producers using the given class to populate it.
	 * 
	 * @param capacity the capacity of the <code>AtomicMultiplexer</code>
	 * @param klass the class used to populate the <code>AtomicMultiplexer</code>
	 * @param numberOfProducers the number of producers that will use this <code>AtomicMultiplexer</code>
	 */
	public AtomicMultiplexer(int capacity, Class<E> klass, int numberOfProducers) {
		this(capacity, Builder.createBuilder(klass), numberOfProducers);
	}
	
	@Override
	public final void clear() {
		producerIndex = 0;
		for(int i = 0; i < queues.length; i++) {
			queues[i].clear();
		}
		for(int i = 0; i < avail.length; i++) {
			avail[i] = -1;
		}
	}

	@Override
    public final E nextToDispatch(int producer) {
	    return queues[producer].nextToDispatch();
    }
	
	@Override
    public final E nextToDispatch(int producer, E swap) {
	    E val = queues[producer].nextToDispatch(swap);
	    if (val == null) return null;
	    return val;
    }

	@Override
    public final void flush(int producer, boolean lazySet) {
		queues[producer].flush(lazySet);
    }

	@Override
    public final void flush(int producer) {
		queues[producer].flush();
    }

	@Override
    public final long availableToPoll() {
		long total = 0;
		for(int i = 0; i < numberOfProducers; i++) {
			long x = queues[i].availableToPoll();
			if (x == 0) {
				avail[i] = -1;
			} else {
				total += (avail[i] = x);
			}
		}
		return total;
    }

	@Override
    public final E poll() {
		for(int i = 0; i < numberOfProducers; i++) {
			int index = producerIndex++;
			if (producerIndex == numberOfProducers) producerIndex = 0;
			if (avail[index] > 0) {
				avail[index]--;
				return queues[index].poll();
			}
		}
		return null;
	}

	@Override
    public final void donePolling(boolean lazySet) {
		for(int i = 0; i < numberOfProducers; i++) {
			if (avail[i] != -1) {
				queues[i].donePolling(lazySet);
			}
		}
    }

	@Override
    public final void donePolling() {
		donePolling(false);
    }

	@Override
    public final int getNumberOfProducers() {
	    return numberOfProducers;
    }
	
	@Override
	public final Producer<E> nextProducer() {
		synchronized(producers) {
			if (currProducerIndex == numberOfProducers) return null;
			return producers[currProducerIndex++];
		}
	}
	
	@Override
	public final Producer<E> getProducer(int index) {
		if (index >= numberOfProducers) {
			throw new RuntimeException("Tried to get a producer with a bad index: " + index);
		}
		return producers[index];
	}
}
