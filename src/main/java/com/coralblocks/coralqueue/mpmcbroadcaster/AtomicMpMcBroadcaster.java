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
package com.coralblocks.coralqueue.mpmcbroadcaster;

import com.coralblocks.coralqueue.broadcaster.AtomicBroadcaster;
import com.coralblocks.coralqueue.broadcaster.Broadcaster;
import com.coralblocks.coralqueue.util.Builder;

/**
 * An implementation of {@link MpMcBroadcaster} that uses <i>memory barriers</i> to synchronize producer and consumer threads.
 * All consumers receive all messages.
 *
 * @param <E> The data transfer mutable object to be used by this mpmc broadcaster
 */
public class AtomicMpMcBroadcaster<E> implements MpMcBroadcaster<E> {
	
	private final static int DEFAULT_CAPACITY = 1024;
	
	private final Broadcaster<E>[] broadcasters;
	private final Producer<E>[] producers;
	private final Consumer<E>[] consumers;
	
	/**
	 * Creates an <code>AtomicMpMcBroadcaster</code> with the default capacity (1024) and number of consumers and producers using the given class to populate it.
	 * 
	 * @param klass the class used to populate the <code>AtomicMpMcBroadcaster</code>
	 * @param numberOfProducers the number of producers that will use this <code>AtomicMpMcBroadcaster</code>
	 * @param numberOfConsumers the number of consumers that will use this <code>AtomicMpMcBroadcaster</code>
	 */
	public AtomicMpMcBroadcaster(Class<E> klass, int numberOfProducers, int numberOfConsumers) {
		this(DEFAULT_CAPACITY, klass, numberOfProducers, numberOfConsumers);
	}
	
	/**
	 * Creates an <code>AtomicMpMcBroadcaster</code> with the given capacity and number of consumers and producers using the given class to populate it.
	 * 
	 * @param capacity the capacity of the <code>AtomicMpMcBroadcaster</code>
	 * @param klass the class used to populate the <code>AtomicMpMcBroadcaster</code>
	 * @param numberOfProducers the number of producers that will use this <code>AtomicMpMcBroadcaster</code>
	 * @param numberOfConsumers the number of consumers that will use this <code>AtomicMpMcBroadcaster</code>
	 */
	public AtomicMpMcBroadcaster(int capacity, Class<E> klass, int numberOfProducers, int numberOfConsumers) {
		this(capacity, Builder.createBuilder(klass), numberOfProducers, numberOfConsumers);
	}
	
	/**
	 * Creates an <code>AtomicMpMcBroadcaster</code> with the default capacity (1024) and number of consumers and producers using the given {@link Builder} to populate it.
	 * 
	 * @param builder the {@link Builder} used to populate the <code>AtomicMpMcBroadcaster</code>
	 * @param numberOfProducers the number of producers that will use this <code>AtomicMpMcBroadcaster</code>
	 * @param numberOfConsumers the number of consumers that will use this <code>AtomicMpMcBroadcaster</code>
	 */
	public AtomicMpMcBroadcaster(Builder<E> builder, int numberOfProducers, int numberOfConsumers) {
		this(DEFAULT_CAPACITY, builder, numberOfProducers, numberOfConsumers);
	}
	
	/**
	 * Creates an <code>AtomicMpMcBroadcaster</code> with the given capacity and number of consumers and producers using the given {@link Builder} to populate it.
	 * 
	 * @param capacity the capacity of the <code>AtomicMpMcBroadcaster</code>
	 * @param builder the {@link Builder} used to populate the <code>AtomicMpMcBroadcaster</code>
	 * @param numberOfProducers the number of producers that will use this <code>AtomicMpMcBroadcaster</code>
	 * @param numberOfConsumers the number of consumers that will use this <code>AtomicMpMcBroadcaster</code>
	 */
	@SuppressWarnings("unchecked")
    public AtomicMpMcBroadcaster(int capacity, Builder<E> builder, int numberOfProducers, int numberOfConsumers) {
		this.broadcasters = (Broadcaster<E>[]) new Broadcaster[numberOfProducers];
		this.producers = (Producer<E>[]) new Producer[numberOfProducers];
		this.consumers = (Consumer<E>[]) new Consumer[numberOfConsumers];
		
		for(int i = 0; i < numberOfProducers; i++) {
			this.broadcasters[i] = new AtomicBroadcaster<E>(builder, numberOfConsumers);
			this.producers[i] = new Producer<E>(this.broadcasters[i], i);
		}
		
		for(int i = 0; i < numberOfConsumers; i++) {
			com.coralblocks.coralqueue.broadcaster.Consumer<E>[] c = (com.coralblocks.coralqueue.broadcaster.Consumer<E>[]) new com.coralblocks.coralqueue.broadcaster.Consumer[numberOfProducers];
			int index = 0;
			for(int j = 0; j < this.broadcasters.length; j++) {
				c[index++] = this.broadcasters[j].getConsumer(i);
			}
			this.consumers[i] = new Consumer<E>(c, i);
		}
	}
	
	@Override
	public final void clear() {
		for(int i = 0; i < broadcasters.length; i++) {
			broadcasters[i].clear();
		}
	}
	
	@Override
	public final E nextToDispatch(int producerIndex) {
		Producer<E> producer = getProducer(producerIndex);
		return producer.nextToDispatch();
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
	public final void disableConsumer(int consumerIndex) {
		for(int i = 0; i < broadcasters.length; i++) {
			broadcasters[i].disableConsumer(consumerIndex);
		}
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
