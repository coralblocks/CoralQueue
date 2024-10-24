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

public class AtomicMpMc<E> implements MpMc<E> {
	
	private final static int DEFAULT_CAPACITY = 1024;
	
	private final Demultiplexer<E>[] demuxes;
	private final Producer<E>[] producers;
	private final Consumer<E>[] consumers;
	private int currProducerIndex = 0;
	private int currConsumerIndex = 0;
	
	public AtomicMpMc(Class<E> klass, int numberOfProducers, int numberOfConsumers) {
		this(DEFAULT_CAPACITY, klass, numberOfProducers, numberOfConsumers);
	}
	
	public AtomicMpMc(int capacity, Class<E> klass, int numberOfProducers, int numberOfConsumers) {
		this(capacity, Builder.createBuilder(klass), numberOfProducers, numberOfConsumers);
	}
	
	public AtomicMpMc(Builder<E> builder, int numberOfProducers, int numberOfConsumers) {
		this(DEFAULT_CAPACITY, builder, numberOfProducers, numberOfConsumers);
	}
	
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
			for(Demultiplexer<E> demux : this.demuxes) {
				c[index++] = demux.nextConsumer();
			}
			this.consumers[i] = new Consumer<E>(c, i);
		}
	}
	
	@Override
	public void clear() {
		for(int i = 0; i < demuxes.length; i++) {
			demuxes[i].clear();
		}
		currProducerIndex = 0;
		currConsumerIndex = 0;
	}
	
	@Override
	public E nextToDispatch(int producerIndex) {
		Producer<E> producer = getProducer(producerIndex);
		return producer.nextToDispatch();
	}
	
	@Override
	public E nextToDispatch(int producerIndex, int toConsumerIndex) {
		Producer<E> producer = getProducer(producerIndex);
		return producer.nextToDispatch(toConsumerIndex);
	}
	
	@Override
	public void flush(int producerIndex, boolean lazySet) {
		Producer<E> producer = getProducer(producerIndex);
		producer.flush(lazySet);
	}
	
	@Override
	public void flush(int producerIndex) {
		Producer<E> producer = getProducer(producerIndex);
		producer.flush();
	}
	
	@Override
	public long availableToPoll(int consumerIndex) {
		Consumer<E> consumer = getConsumer(consumerIndex);
		return consumer.availableToPoll();
	}
	
	@Override
	public E poll(int consumerIndex) {
		Consumer<E> consumer = getConsumer(consumerIndex);
		return consumer.poll();
	}
	
	@Override
	public void donePolling(int consumerIndex, boolean lazySet) {
		Consumer<E> consumer = getConsumer(consumerIndex);
		consumer.donePolling(lazySet);
	}
	
	@Override
	public void donePolling(int consumerIndex) {
		Consumer<E> consumer = getConsumer(consumerIndex);
		consumer.donePolling();
	}
	
	@Override
	public Producer<E> nextProducer() {
		synchronized(producers) {
			if (currProducerIndex == producers.length) return null;
			return producers[currProducerIndex++];
		}
	}
	
	@Override
	public Producer<E> getProducer(int index) {
		if (index >= producers.length) {
			throw new RuntimeException("Tried to get a producer with a bad index: " + index);
		}
		return producers[index];
	}
	
	@Override
	public Consumer<E> nextConsumer() {
		synchronized(consumers) {
			if (currConsumerIndex == consumers.length) return null;
			return consumers[currConsumerIndex++];
		}
	}
	
	@Override
	public Consumer<E> getConsumer(int index) {
		if (index >= consumers.length) {
			throw new RuntimeException("Tried to get a consumer with a bad index: " + index);
		}
		return consumers[index];
	}
	
	@Override
	public int getNumberOfConsumers() {
		return consumers.length;
	}
	
	@Override
	public int getNumberOfProducers() {
		return producers.length;
	}
}
