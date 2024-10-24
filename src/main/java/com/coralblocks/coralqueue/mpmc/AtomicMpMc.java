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
	
	private final Demultiplexer<E>[] demuxes;
	private final Producer<E>[] producers;
	private final Consumer<E>[] consumers;
	private int currProducerIndex = 0;
	private int currConsumerIndex = 0;
	
	public AtomicMpMc(int capacity, Class<E> klass, int numberOfProducers, int numberOfConsumers) {
		this(capacity, Builder.createBuilder(klass), numberOfProducers, numberOfConsumers);
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
}
