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

/**
 * <p>The MpMc (Multiple Producers / Multiple Consumers) API that allows multiple consumer threads receiving messages from the mpmc and multiple producer threads sending messages to the mpmc.
 * Two different consumers will never poll the same message.</p>
 * 
 * <p><b>NOTE:</b> A mpmc must have a <b>fixed</b> number of consumers and a fixed number of producers specified by its constructor.</p>
 *
 * @param <E> The mutable transfer object to be used by this mpmc
 */
public interface MpMc<E> {
	
	/**
	 * <p>Return the next mutable object that can be used by the given producer to dispatch data to the mpmc.</p>
	 * 
	 * <p>If no object is currently available (i.e. the mpmc is full) this method returns null.</p>
	 * 
	 * @param producerIndex the index of the producer to use
	 * @return the next mutable object that can be used by the given producer or null if the mpmc is full
	 */
	public E nextToDispatch(int producerIndex);
	
	/**
	 * <p>Return the next mutable object that can be used by the given producer to dispatch data to the mpmc.
	 * This method allows you to specify the consumer that you want to receive the message.</p>
	 * 
	 * <p>If no object is currently available (i.e. the mpmc is full) this method returns null.</p>
	 * 
	 * @param producerIndex the index of the producer to use
	 * @param toConsumerIndex the consumer that you want to receive the message
	 * @return the next mutable object that can be used by the given producer or null if the mpmc is full
	 */
	public E nextToDispatch(int producerIndex, int toConsumerIndex);

	/**
	 * <p>Dispatch/Flush all previously obtained objects through the {@link #nextToDispatch(int)} method to the consumers.</p>
	 * 
	 * @param producerIndex the index of the producer to use
	 * @param lazySet true to flush (i.e. notify the consumers) in a lazy way or false to flush <b>immediately</b>
	 */
	public void flush(int producerIndex, boolean lazySet);
	
	/**
	 * <p>Dispatch <b>immediately</b> all previously obtained objects through the {@link #nextToDispatch(int)} method to the consumers.
	 * Note that this is the same as calling <code>flush(producerIndex, false)</code>.</p>
	 * 
	 * @param producerIndex the index of the producer to use
	 */
	public void flush(int producerIndex);
	
	/**
	 * Return the next producer that can be used or null if all producers were already returned.
	 * 
	 * @return the next produced to be used or null
	 */
	public Producer<E> nextProducer();
	
	/**
	 * Return the producer corresponding to the given index. If a bad index is given this method throws a <code>RuntimeException</code>.
	 * 
	 * @param index the producer index
	 * @return the producer for the given index
	 */
	public Producer<E> getProducer(int index);
	
	/**
	 * Return the next consumer that can be used or null if all consumers were already returned.
	 * 
	 * @return the next consumer to be used or null
	 */
	public Consumer<E> nextConsumer();
	
	/**
	 * Return the consumer corresponding to the given index. If a bad index is given this method throws a <code>RuntimeException</code>.
	 * 
	 * @param index the consumer index
	 * @return the consumer for the given index
	 */
	public Consumer<E> getConsumer(int index);
}
