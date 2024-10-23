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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.coralblocks.coralqueue.queue.AtomicQueue;
import com.coralblocks.coralqueue.queue.Queue;
import com.coralblocks.coralqueue.util.Builder;
import com.coralblocks.coralqueue.util.IdentityMap;
import com.coralblocks.coralqueue.util.LinkedObjectPool;
import com.coralblocks.coralqueue.util.ObjectPool;

/**
 * An implementation of {@link DynamicMultiplexer} that uses <i>memory barriers</i> to synchronize producers and consumer sequences.
 * 
 * It uses an <code>IdentityMap + Thread.currentThread()</code> to maintain its producers, that can be added dynamically.
 * 
 * @param <E>The mutable transfer object to be used by this multiplexer
 */
public class DynamicAtomicMultiplexer<E> implements DynamicMultiplexer<E> {

	private static class AvailHolder<E> {
		long[] avail;
		Queue<E> queue;
	}
	
	private final ObjectPool<Queue<E>> queuePool;
	private final IdentityMap<Thread, Queue<E>> queues;
	private final ObjectPool<AvailHolder<E>> availHolderPool;
	private final List<AvailHolder<E>> availHolders;
	
	/**
	 * Creates a <code>DynamicAtomicMultiplexer</code> with the given capacity and number of initial producers using the given {@link Builder} to populate it.
	 * 
	 * @param capacity the capacity of the <code>DynamicAtomicMultiplexer</code>
	 * @param builder the {@link Builder} used to populate the <code>DynamicAtomicMultiplexer</code>
	 * @param initialNumberOfProducers the number of producers that will use this <code>DynamicAtomicMultiplexer</code>
	 */
    public DynamicAtomicMultiplexer(final int capacity, final Builder<E> builder, final int initialNumberOfProducers) {
		
		Builder<Queue<E>> poolBuilder = new Builder<Queue<E>>() {
			@Override
			public Queue<E> newInstance() {
				return new AtomicQueue<E>(capacity, builder);
			}
		};
		
		Builder<AvailHolder<E>> availBuilder = new Builder<AvailHolder<E>>() {
			@Override
			public AvailHolder<E> newInstance() {
				AvailHolder<E> holder = new AvailHolder<E>();
				holder.avail = new long[1];
				holder.queue = null;
				return holder;
			}
		};
		
		this.queuePool = new LinkedObjectPool<Queue<E>>(initialNumberOfProducers, poolBuilder);
		this.queues = new IdentityMap<Thread, Queue<E>>(initialNumberOfProducers * 2);
		this.availHolderPool = new LinkedObjectPool<AvailHolder<E>>(initialNumberOfProducers, availBuilder);
		this.availHolders = new ArrayList<AvailHolder<E>>(initialNumberOfProducers * 2);
	}
	
	/**
	 * Creates a <code>DynamicAtomicMultiplexer</code> with the given capacity and number of initial producers using the given class to populate it.
	 * 
	 * @param capacity the capacity of the <code>DynamicAtomicMultiplexer</code>
	 * @param klass the class used to populate the <code>DynamicAtomicMultiplexer</code>
	 * @param initialNumberOfProducers the number of producers that will use this <code>DynamicAtomicMultiplexer</code>
	 */
	public DynamicAtomicMultiplexer(final int size, final Class<E> klass, final int initialNumberOfProducers) {
		this(size, Builder.createBuilder(klass), initialNumberOfProducers);
	}
	
	@Override
	public synchronized final void clear() {
		
		for(int i = 0; i < availHolders.size(); i++) {
			AvailHolder<E> holder = availHolders.get(i);
			holder.queue = null;
			availHolderPool.release(holder);
		}
		availHolders.clear();
		
		Iterator<Queue<E>> iter2 = queues.iterator();
		while(iter2.hasNext()) {
			Queue<E> queue = iter2.next();
			queue.clear();
			queuePool.release(queue);
		}
		queues.clear();
	}
	
	private synchronized final Queue<E> getQueue(Thread t) {
		Queue<E> queue = queues.get(t);
		if (queue == null) {
			queue = queuePool.get();
			queues.put(t, queue);
		}
		return queue;
	}

	@Override
    public final E nextToDispatch() {
	    return getQueue(Thread.currentThread()).nextToDispatch();
    }
	
	@Override
    public final E nextToDispatch(E swap) {
	    return getQueue(Thread.currentThread()).nextToDispatch(swap);
    }

	@Override
    public final void flush(boolean lazySet) {
    	getQueue(Thread.currentThread()).flush(lazySet);
    }

	@Override
    public final void flush() {
    	getQueue(Thread.currentThread()).flush();
    }
    
	@Override
    public synchronized final long availableToPoll() {

       	if (!availHolders.isEmpty()) {
    		for(int i = 0; i < availHolders.size(); i++) {
    			AvailHolder<E> holder = availHolders.get(i);
    			holder.queue = null;
    			availHolderPool.release(holder);
    		}
    		availHolders.clear();
    	}
		
    	long total = 0;
		
		Iterator<Queue<E>> iter = queues.iterator();
		while(iter.hasNext()) {
			Queue<E> queue = iter.next();
			long a = queue.availableToPoll();
			if (a > 0) {
				AvailHolder<E> availHolder = availHolderPool.get();
				availHolder.avail[0] = a;
				availHolder.queue = queue;
				availHolders.add(availHolder);
				total += a;
			}
		}
		
		return total;
    }

	@Override
    public final E poll() {
    	
    	final int size = availHolders.size();
    	for(int i = 0; i < size; i++) {
    		AvailHolder<E> holder = availHolders.get(i);
    		if (holder.avail[0] == 0) continue;
			holder.avail[0]--;
			return holder.queue.poll();
    	}
    	return null;
	}

	@Override
    public final void donePolling(boolean lazySet) {
    	final int size = availHolders.size();
    	for(int i = 0; i < size; i++) {
    		AvailHolder<E> holder = availHolders.get(i);
    		holder.queue.donePolling(lazySet);
    		holder.queue = null;
    		availHolderPool.release(holder);
    	}
    	availHolders.clear();
    }

	@Override
    public final void donePolling() {
		donePolling(false);
    }

	@Override
    public synchronized final int getNumberOfProducers() {
	    return queues.size();
    }
}
