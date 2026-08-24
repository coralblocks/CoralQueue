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
package com.coralblocks.coralqueue.multiplexer;

import com.coralblocks.coralqueue.queue.AtomicQueue;
import com.coralblocks.coralqueue.queue.Queue;
import com.coralblocks.coralqueue.util.Builder;

/**
 * An implementation of {@link Multiplexer} that uses <i>memory barriers</i> to synchronize producers and consumer sequences.
 * Consumer-side batch operations inspect one internal queue per producer. Therefore,
 * <code>availableToFetch()</code> and <code>doneFetching()</code> scale linearly with the number of producers,
 * and <code>fetch()</code> may inspect up to that many queues to find data.
 *
 * @param <E> The data transfer mutable object to be used by this multiplexer
 */
public class AtomicMultiplexer<E> implements Multiplexer<E> {
	
	public static final int DEFAULT_CAPACITY = 1024;
	
	private final int numberOfProducers;
	private final Queue<E>[] queues;
	private final long[] avail;
	private final boolean[] needsDoneFetching;
	private int producerIndex = 0;
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
		if (numberOfProducers <= 0) {
			throw new IllegalArgumentException("numberOfProducers must be positive: " + numberOfProducers);
		}
		this.numberOfProducers = numberOfProducers;
		this.queues = (Queue<E>[]) new AtomicQueue[numberOfProducers];
		this.producers = (Producer<E>[]) new Producer[numberOfProducers];
		this.avail = new long[numberOfProducers];
		this.needsDoneFetching = new boolean[numberOfProducers];
		for(int i = 0; i < numberOfProducers; i++) {
			queues[i] = new AtomicQueue<E>(capacity, builder);
			producers[i] = new Producer<E>(this, i);
		}
	}
	
	/**
	 * Creates an <code>AtomicMultiplexer</code> with the default capacity (1024) and number of producers using the given {@link Builder} to populate it.
	 * 
	 * @param builder the {@link Builder} used to populate the <code>AtomicMultiplexer</code>
	 * @param numberOfProducers the number of producers that will use this <code>AtomicMultiplexer</code>
	 */
	public AtomicMultiplexer(Builder<E> builder, int numberOfProducers) {
		this(DEFAULT_CAPACITY, builder, numberOfProducers);
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
	
	/**
	 * Creates an <code>AtomicMultiplexer</code> with the default capacity (1024) and number of producers using the given class to populate it.
	 * 
	 * @param klass the class used to populate the <code>AtomicMultiplexer</code>
	 * @param numberOfProducers the number of producers that will use this <code>AtomicMultiplexer</code>
	 */
	public AtomicMultiplexer(Class<E> klass, int numberOfProducers) {
		this(DEFAULT_CAPACITY, klass, numberOfProducers);
	}
	
	@Override
	public final void clear() {
		producerIndex = 0;
		for(int i = 0; i < queues.length; i++) {
			queues[i].clear();
		}
		for(int i = 0; i < avail.length; i++) {
			avail[i] = 0;
			needsDoneFetching[i] = false;
		}
	}

	@Override
    public final E nextToDispatch(int producer) {
	    return getQueue(producer).nextToDispatch();
    }
	
	@Override
    public final E nextToDispatch(int producer, E swap) {
	    E val = getQueue(producer).nextToDispatch(swap);
	    if (val == null) return null;
	    return val;
    }

	@Override
    public final void flush(int producer, boolean lazySet) {
		getQueue(producer).flush(lazySet);
    }

	@Override
    public final void flush(int producer) {
		getQueue(producer).flush();
    }

	@Override
    public final long availableToFetch() {
		long total = 0;
		for(int i = 0; i < numberOfProducers; i++) {
			long x = queues[i].availableToFetch();
			total += (avail[i] = x);
		}
		return total;
    }

	@Override
    public final E fetch() {
		for(int i = 0; i < numberOfProducers; i++) {
			int index = producerIndex++;
			if (producerIndex == numberOfProducers) producerIndex = 0;
			if (avail[index] > 0) {
				avail[index]--;
				E e = queues[index].fetch();
				needsDoneFetching[index] = true;
				return e;
			}
		}
		return null;
	}

	@Override
    public final void doneFetching(boolean lazySet) {
		for(int i = 0; i < numberOfProducers; i++) {
			if (needsDoneFetching[i]) {
				queues[i].doneFetching(lazySet);
				needsDoneFetching[i] = false;
			}
		}
    }

	@Override
    public final void doneFetching() {
		doneFetching(false);
    }

	@Override
    public final int getNumberOfProducers() {
	    return numberOfProducers;
    }
	
	@Override
	public final Producer<E> getProducer(int index) {
		checkProducerIndex(index);
		return producers[index];
	}

	private final void checkProducerIndex(int index) {
		if (index < 0 || index >= numberOfProducers) {
			throw new IndexOutOfBoundsException("producerIndex=" + index + ", numberOfProducers=" + numberOfProducers);
		}
	}

	private final Queue<E> getQueue(int producerIndex) {
		checkProducerIndex(producerIndex);
		return queues[producerIndex];
	}
}
