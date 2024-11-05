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

import com.coralblocks.coralqueue.demultiplexer.AtomicDemultiplexer;
import com.coralblocks.coralqueue.demultiplexer.Demultiplexer;
import com.coralblocks.coralqueue.util.Builder;

/**
 * An implementation of {@link MpMc} that uses <i>memory barriers</i> to synchronize producers and consumers threads.
 * Two different consumers will never poll the same message.
 *
 * @param <E> The data transfer mutable object to be used by this mpmc
 */
public class AtomicMpMc<E> implements MpMc<E> {
	
	private final static int DEFAULT_CAPACITY = 1024;
	
	private final Demultiplexer<E>[] demuxes;
	private final Producer<E>[] producers;
	private final Consumer<E>[] consumers;
	
	/**
	 * Creates an <code>AtomicMpMc</code> with the default capacity (1024) and number of consumers and producers using the given class to populate it.
	 * 
	 * @param klass the class used to populate the <code>AtomicMpMc</code>
	 * @param numberOfProducers the number of producers that will use this <code>AtomicMpMc</code>
	 * @param numberOfConsumers the number of consumers that will use this <code>AtomicMpMc</code>
	 */
	public AtomicMpMc(Class<E> klass, int numberOfProducers, int numberOfConsumers) {
		this(DEFAULT_CAPACITY, klass, numberOfProducers, numberOfConsumers);
	}
	
	/**
	 * Creates an <code>AtomicMpMc</code> with the given capacity and number of consumers and producers using the given class to populate it.
	 * 
	 * @param capacity the capacity of the <code>AtomicMpMc</code>
	 * @param klass the class used to populate the <code>AtomicMpMc</code>
	 * @param numberOfProducers the number of producers that will use this <code>AtomicMpMc</code>
	 * @param numberOfConsumers the number of consumers that will use this <code>AtomicMpMc</code>
	 */
	public AtomicMpMc(int capacity, Class<E> klass, int numberOfProducers, int numberOfConsumers) {
		this(capacity, Builder.createBuilder(klass), numberOfProducers, numberOfConsumers);
	}
	
	/**
	 * Creates an <code>AtomicMpMc</code> with the default capacity (1024) and number of consumers and producers using the given {@link Builder} to populate it.
	 * 
	 * @param builder the {@link Builder} used to populate the <code>AtomicMpMc</code>
	 * @param numberOfProducers the number of producers that will use this <code>AtomicMpMc</code>
	 * @param numberOfConsumers the number of consumers that will use this <code>AtomicMpMc</code>
	 */
	public AtomicMpMc(Builder<E> builder, int numberOfProducers, int numberOfConsumers) {
		this(DEFAULT_CAPACITY, builder, numberOfProducers, numberOfConsumers);
	}
	
	/**
	 * Creates an <code>AtomicMpMc</code> with the given capacity and number of consumers and producers using the given {@link Builder} to populate it.
	 * 
	 * @param capacity the capacity of the <code>AtomicMpMc</code>
	 * @param builder the {@link Builder} used to populate the <code>AtomicMpMc</code>
	 * @param numberOfProducers the number of producers that will use this <code>AtomicMpMc</code>
	 * @param numberOfConsumers the number of consumers that will use this <code>AtomicMpMc</code>
	 */
	@SuppressWarnings("unchecked")
    public AtomicMpMc(int capacity, Builder<E> builder, int numberOfProducers, int numberOfConsumers) {
		this.demuxes = (Demultiplexer<E>[]) new Demultiplexer[numberOfProducers];
		this.producers = (Producer<E>[]) new Producer[numberOfProducers];
		this.consumers = (Consumer<E>[]) new Consumer[numberOfConsumers];
		
		for(int i = 0; i < numberOfProducers; i++) {
			this.demuxes[i] = new AtomicDemultiplexer<E>(builder, numberOfConsumers);
			this.producers[i] = new Producer<E>(this.demuxes[i], i);
		}
		
		for(int i = 0; i < numberOfConsumers; i++) {
			com.coralblocks.coralqueue.demultiplexer.Consumer<E>[] c = (com.coralblocks.coralqueue.demultiplexer.Consumer<E>[]) new com.coralblocks.coralqueue.demultiplexer.Consumer[numberOfProducers];
			int index = 0;
			for(int j = 0; j < this.demuxes.length; j++) {
				c[index++] = this.demuxes[j].getConsumer(i);
			}
			this.consumers[i] = new Consumer<E>(c, i);
		}
	}
	
	@Override
	public final void clear() {
		for(int i = 0; i < demuxes.length; i++) {
			demuxes[i].clear();
		}
	}
	
	@Override
	public final E nextToDispatch(int producerIndex) {
		Producer<E> producer = getProducer(producerIndex);
		return producer.nextToDispatch();
	}
	
	@Override
	public final E nextToDispatch(int producerIndex, int toConsumerIndex) {
		Producer<E> producer = getProducer(producerIndex);
		return producer.nextToDispatch(toConsumerIndex);
	}
	
	@Override
	public final void flush(int producerIndex, boolean lazySet) {
		Producer<E> producer = getProducer(producerIndex);
		producer.flush(lazySet);
	}
	
	@Override
	public final void flush(int producerIndex) {
		Producer<E> producer = getProducer(producerIndex);
		producer.flush();
	}
	
	@Override
	public final long availableToPoll(int consumerIndex) {
		Consumer<E> consumer = getConsumer(consumerIndex);
		return consumer.availableToPoll();
	}
	
	@Override
	public final E poll(int consumerIndex) {
		Consumer<E> consumer = getConsumer(consumerIndex);
		return consumer.poll();
	}
	
	@Override
	public final void donePolling(int consumerIndex, boolean lazySet) {
		Consumer<E> consumer = getConsumer(consumerIndex);
		consumer.donePolling(lazySet);
	}
	
	@Override
	public final void donePolling(int consumerIndex) {
		Consumer<E> consumer = getConsumer(consumerIndex);
		consumer.donePolling();
	}
	
	@Override
	public final Producer<E> getProducer(int index) {
		if (index >= producers.length) {
			throw new RuntimeException("Tried to get a producer with a bad index: " + index);
		}
		return producers[index];
	}
	
	@Override
	public final Consumer<E> getConsumer(int index) {
		if (index >= consumers.length) {
			throw new RuntimeException("Tried to get a consumer with a bad index: " + index);
		}
		return consumers[index];
	}
	
	@Override
	public final int getNumberOfConsumers() {
		return consumers.length;
	}
	
	@Override
	public final int getNumberOfProducers() {
		return producers.length;
	}
}
