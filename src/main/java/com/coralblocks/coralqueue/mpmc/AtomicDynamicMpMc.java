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
package com.coralblocks.coralqueue.mpmc;

import java.util.ArrayList;
import java.util.List;

import com.coralblocks.coralqueue.demultiplexer.AtomicDynamicDemultiplexer;
import com.coralblocks.coralqueue.demultiplexer.DynamicDemultiplexer;
import com.coralblocks.coralqueue.util.Builder;
import com.coralblocks.coralqueue.util.IdentityMap;
import com.coralblocks.coralqueue.util.LinkedObjectPool;
import com.coralblocks.coralqueue.util.MutableLong;
import com.coralblocks.coralqueue.util.ObjectPool;

public class AtomicDynamicMpMc<E> implements DynamicMpMc<E> {
	
	private final static int DEFAULT_CAPACITY = 1024;
	
	private final List<DynamicDemultiplexer<E>> demuxes;
	private final ObjectPool<DynamicDemultiplexer<E>> demuxPool;
	private final IdentityMap<Thread, DynamicDemultiplexer<E>> producerToDemuxes;
	private final IdentityMap<Thread, List<MutableLong>> consumerToLongs;
	private final ObjectPool<List<MutableLong>> listPool;
	
	public AtomicDynamicMpMc(Class<E> klass, int initialNumberOfProducers, int initialNumberOfConsumers) {
		this(DEFAULT_CAPACITY, klass, initialNumberOfProducers, initialNumberOfConsumers);
	}
	
	public AtomicDynamicMpMc(int capacity, Class<E> klass, int initialNumberOfProducers, int initialNumberOfConsumers) {
		this(capacity, Builder.createBuilder(klass), initialNumberOfProducers, initialNumberOfConsumers);
	}
	
	public AtomicDynamicMpMc(Builder<E> builder, int initialNumberOfProducers, int initialNumberOfConsumers) {
		this(DEFAULT_CAPACITY, builder, initialNumberOfProducers, initialNumberOfConsumers);
	}
	
    public AtomicDynamicMpMc(int capacity, Builder<E> builder, int initialNumberOfProducers, int initialNumberOfConsumers) {

		Builder<DynamicDemultiplexer<E>> demuxBuilder = new Builder<DynamicDemultiplexer<E>>() {
			@Override
			public DynamicDemultiplexer<E> newInstance() {
				return new AtomicDynamicDemultiplexer<E>(capacity, builder, initialNumberOfConsumers);
			}
		};
		
		demuxPool = new LinkedObjectPool<DynamicDemultiplexer<E>>(initialNumberOfProducers * 2, demuxBuilder); // time 2 to avoid late allocation
		
		final int extraFactor = 5;
		
		Builder<List<MutableLong>> listBuilder = new Builder<List<MutableLong>>() {
			@Override
			public List<MutableLong> newInstance() {
				return new ArrayList<MutableLong>(initialNumberOfProducers * extraFactor);
			}
		};
		
		listPool = new LinkedObjectPool<List<MutableLong>>(initialNumberOfConsumers * extraFactor, listBuilder);
				
		producerToDemuxes = new IdentityMap<Thread, DynamicDemultiplexer<E>>(initialNumberOfProducers * extraFactor);
		consumerToLongs = new IdentityMap<Thread, List<MutableLong>>(initialNumberOfConsumers * extraFactor);
		
		demuxes = new ArrayList<DynamicDemultiplexer<E>>(initialNumberOfProducers * extraFactor);
	}
	
	private synchronized final DynamicDemultiplexer<E> getDemux(Thread t) {
		DynamicDemultiplexer<E> demux = producerToDemuxes.get(t);
		if (demux == null) {
			demux = demuxPool.get();
			producerToDemuxes.put(t, demux);
			demuxes.add(demux);
		}
		return demux;
	}
	
	@Override
	public final E nextToDispatch() {
		return getDemux(Thread.currentThread()).nextToDispatch();
	}
	
	@Override
	public final E nextToDispatch(int toConsumerIndex) {
		return getDemux(Thread.currentThread()).nextToDispatch(toConsumerIndex);
	}
	
	@Override
	public final void flush(boolean lazySet) {
		getDemux(Thread.currentThread()).flush(lazySet);
	}
	
	@Override
	public final void flush() {
		getDemux(Thread.currentThread()).flush();
	}
	
	@Override
	public synchronized final long availableToPoll() {
		long total = 0;
		Thread t = Thread.currentThread();
		List<MutableLong> avail = consumerToLongs.get(t);
		if (avail == null) {
			avail = listPool.get();
			consumerToLongs.put(t, avail);
		}
		int size = demuxes.size();
		for(int i = 0; i < size; i++) {
			long x = demuxes.get(i).availableToPoll();
			if (avail.size() == i) {
				avail.add(new MutableLong(x));
			} else {
				avail.get(i).set(x == 0 ? -1 : x);
			}
			total += x;
		}
		return total;
	}
	
	@Override
	public synchronized final E poll() {
		Thread t = Thread.currentThread();
		List<MutableLong> avail = consumerToLongs.get(t);
		int size = demuxes.size();
		if (size > avail.size()) size = avail.size();
		for(int i = 0; i < size; i++) {
			MutableLong ml = avail.get(i);
			long value = ml.get();
			if (value > 0) {
				E e = demuxes.get(i).poll();
				ml.set(value - 1);
				return e;
			}
		}
		throw new IllegalStateException("Tried to return null!");
	}
	
	@Override
	public synchronized final void donePolling(boolean lazySet) {
		Thread t = Thread.currentThread();
		List<MutableLong> avail = consumerToLongs.get(t);
		int size = demuxes.size();
		if (size > avail.size()) size = avail.size();
		for(int i = 0; i < size; i++) {
			MutableLong ml = avail.get(i);
			if (ml.get() >= 0) { // even if you did not poll everything you need to call donePolling()
				demuxes.get(i).donePolling(lazySet);
			}
		}
	}
	
	@Override
	public final void donePolling() {
		donePolling(false);
	}
	
	@Override
	public synchronized final int getNumberOfConsumers() {
		return consumerToLongs.size();
	}
	
	@Override
	public synchronized final int getNumberOfProducers() {
		return producerToDemuxes.size();
	}
}
