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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.coralblocks.coralqueue.queue.AtomicQueue;
import com.coralblocks.coralqueue.queue.Queue;
import com.coralblocks.coralqueue.util.Builder;
import com.coralblocks.coralqueue.util.IdentityMap;
import com.coralblocks.coralqueue.util.LinkedObjectPool;
import com.coralblocks.coralqueue.util.MathUtils;
import com.coralblocks.coralqueue.util.ObjectPool;

/**
 * An implementation of {@link DynamicDemultiplexer} that uses <i>memory barriers</i> to synchronize producer and consumers sequences.
 * 
 * It uses an <code>IdentityMap + Thread.currentThread()</code> to maintain its consumers, that can be added dynamically.
 *
 * @param <E> The mutable transfer object to be used by this demultiplexer
 */
public class AtomicDynamicDemultiplexer<E> implements DynamicDemultiplexer<E> {
	
	private final static int DEFAULT_CAPACITY = 1024;
	
	private int currQueueToDispatch = 0;
	private List<Boolean> needsToFlush;
	private final List<AtomicQueue<E>> queues;
	private final IdentityMap<Thread, AtomicQueue<E>> threadToQueues;
	private int numberOfConsumers;
	private final ObjectPool<AtomicQueue<E>> queuePool;
	
	/**
	 * Creates an <code>AtomicDynamicDemultiplexer</code> with the given capacity and number of consumers using the given {@link Builder} to populate it.
	 * 
	 * @param capacity the capacity of the <code>AtomicDynamicDemultiplexer</code>
	 * @param builder the {@link Builder} used to populate the <code>AtomicDynamicDemultiplexer</code>
	 * @param initialNumberOfConsumers the initial number of consumers that will use this <code>AtomicDynamicDemultiplexer</code>
	 */
	public AtomicDynamicDemultiplexer(int capacity, Builder<E> builder, int initialNumberOfConsumers) {
		MathUtils.ensurePowerOfTwo(capacity);
		final int safeFactor = 5; // if you grow the number of consumers pass this factor, ArrayList will produce garbage...
		this.queues = new ArrayList<AtomicQueue<E>>(initialNumberOfConsumers * safeFactor);
		this.needsToFlush = new ArrayList<Boolean>(initialNumberOfConsumers * safeFactor);
		this.threadToQueues = new IdentityMap<Thread, AtomicQueue<E>>(initialNumberOfConsumers * safeFactor);
		for(int i = 0; i < initialNumberOfConsumers; i++) {
			this.queues.add(new AtomicQueue<E>(capacity, builder));
		}
		
		Builder<AtomicQueue<E>> poolBuilder = new Builder<AtomicQueue<E>>() {
			@Override
			public AtomicQueue<E> newInstance() {
				return new AtomicQueue<E>(capacity, builder);
			}
		};
		
		this.queuePool = new LinkedObjectPool<AtomicQueue<E>>(initialNumberOfConsumers, poolBuilder);
		
		this.numberOfConsumers = 0; // consumers need to come along later to be counted
	}

	/**
	 * Creates an <code>AtomicDynamicDemultiplexer</code> with the default capacity (1024) and number of consumers using the given {@link Builder} to populate it.
	 * 
	 * @param builder the {@link Builder} used to populate the <code>AtomicDynamicDemultiplexer</code>
	 * @param numberOfConsumers the number of consumers that will use this <code>AtomicDynamicDemultiplexer</code>
	 */
	public AtomicDynamicDemultiplexer(Builder<E> builder, int numberOfConsumers) {
		this(DEFAULT_CAPACITY, builder, numberOfConsumers);
	}
	
	/**
	 * Creates an <code>AtomicDynamicDemultiplexer</code> with the given capacity and number of consumers using the given class to populate it.
	 * 
	 * @param capacity the capacity of the <code>AtomicDynamicDemultiplexer</code>
	 * @param klass the class used to populate the <code>AtomicDynamicDemultiplexer</code>
	 * @param numberOfConsumers the number of consumers that will use this <code>AtomicDynamicDemultiplexer</code>
	 */
	public AtomicDynamicDemultiplexer(int capacity, Class<E> klass, int numberOfConsumers) {
		this(capacity, Builder.createBuilder(klass), numberOfConsumers);
	}
	
	/**
	 * Creates an <code>AtomicDynamicDemultiplexer</code> with the default capacity (1024) and number of consumers using the given class to populate it.
	 * 
	 * @param klass the class used to populate the <code>AtomicDynamicDemultiplexer</code>
	 * @param numberOfConsumers the number of consumers that will use this <code>AtomicDynamicDemultiplexer</code>
	 */
	public AtomicDynamicDemultiplexer(Class<E> klass, int numberOfConsumers) {
		this(Builder.createBuilder(klass), numberOfConsumers);
	}
	
	@Override
	public synchronized final void clear() {
		
		for(int i = 0; i < numberOfConsumers; i++) {
			queues.set(i, null);
		}
		
		for(int i = 0; i < numberOfConsumers; i++) {
			needsToFlush.set(i, Boolean.FALSE);
		}
		
		this.numberOfConsumers = 0;
		this.currQueueToDispatch = 0;
		
		Iterator<AtomicQueue<E>> iter = threadToQueues.iterator();
		while(iter.hasNext()) {
			AtomicQueue<E> queue = iter.next();
			queue.clear();
			queuePool.release(queue);
		}
		
		threadToQueues.clear();
	}
	
	@Override
	public synchronized final E nextToDispatch() {
		
		int count = 0;
		
		while(count++ < numberOfConsumers) {
			E e = queues.get(currQueueToDispatch).nextToDispatch();
			if (e != null) {
				needsToFlush.set(currQueueToDispatch, Boolean.TRUE);
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
	public synchronized final E nextToDispatch(int consumerIndex) {
		
		if (consumerIndex < 0) return nextToDispatch(); // fall back to regular implementation...
		
		if (consumerIndex >= numberOfConsumers) {
			throw new RuntimeException("Bad consumerIndex: " + consumerIndex + " numberOfConsumers=" + numberOfConsumers);
		}
		
		E e = queues.get(consumerIndex).nextToDispatch();
		if (e != null) {
			needsToFlush.set(consumerIndex, Boolean.TRUE);
			return e;
		}
		return null;
	}
	
	@Override
	public synchronized final void flush(boolean lazySet) {
		
		for(int i = 0; i < numberOfConsumers; i++) {
			if (needsToFlush.get(i) == Boolean.TRUE) {
				queues.get(i).flush(lazySet);
				needsToFlush.set(i, Boolean.FALSE);
			}
		}
	}
	
	@Override
	public final void flush() {
		flush(false);
	}
	
	private synchronized final Queue<E> getQueue(Thread t) {
		AtomicQueue<E> queue = threadToQueues.get(t);
		if (queue == null) {
			queue = queuePool.get();
			threadToQueues.put(t,  queue);
			final int newNumberOfConsumers = this.numberOfConsumers + 1;
			if (queues.size() < newNumberOfConsumers) {
				queues.set(newNumberOfConsumers, queue);
			} else {
				queues.add(queue);
			}
			if (needsToFlush.size() < newNumberOfConsumers) {
				needsToFlush.set(newNumberOfConsumers, Boolean.FALSE);
			} else {
				needsToFlush.add(Boolean.FALSE);
			}
			this.numberOfConsumers = newNumberOfConsumers;
		}
		return queue;
	}

	@Override
	public final long availableToPoll() {
		return getQueue(Thread.currentThread()).availableToPoll();
	}
	
	@Override
	public final E poll() {
		return getQueue(Thread.currentThread()).poll();
	}
	
	@Override
	public final void replace(E newVal) {
		getQueue(Thread.currentThread()).replace(newVal);
	}
	
	@Override
	public final void donePolling(boolean lazySet) {
		getQueue(Thread.currentThread()).donePolling(lazySet);
	}
	
	@Override
	public final void donePolling() {
		getQueue(Thread.currentThread()).donePolling(false);
	}

	@Override
    public synchronized int getNumberOfConsumers() {
	    return numberOfConsumers;
    }
}