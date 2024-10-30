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
 * @param <E> The data transfer mutable object to be used by this mpmc
 */
public interface MpMc<E> {
	
	/**
	 * <p>Clear the mpmc, so that it can be re-used.</p>
	 * 
	 * <p>Make sure you only call this method when the mpmc is idle, in other words, when you are sure
	 * there are currently no threads accessing the mpmc. Also note that all consumer threads must be dead or you
	 * might run into visibility problems.</p>
	 */
	public void clear();
	
	/**
	 * <p>Return the next mutable object that can be used by the given producer to dispatch data to the mpmc. The producer thread calling this method must pass its producer index.</p>
	 * 
	 * <p>If no object is currently available (i.e. the mpmc is full) this method returns null.</p>
	 * 
	 * @param producerIndex the index of the producer to use
	 * @return the next mutable object that can be used by the given producer or null if the mpmc is full
	 */
	public E nextToDispatch(int producerIndex);
	
	/**
	 * <p>Return the next mutable object that can be used by the given producer to dispatch data to the mpmc. The producer thread calling this method must pass its producer index.
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
	 * <p>Dispatch/Flush all previously obtained objects through the {@link #nextToDispatch(int)} method to the consumers. The producer thread calling this method must pass its producer index.</p>
	 * 
	 * @param producerIndex the index of the producer to use
	 * @param lazySet true to flush (i.e. notify the consumers) in a lazy way or false to flush <b>immediately</b>
	 */
	public void flush(int producerIndex, boolean lazySet);
	
	/**
	 * <p>Dispatch <b>immediately</b> all previously obtained objects through the {@link #nextToDispatch(int)} method to the consumers. The producer thread calling this method must pass its producer index.
	 * Note that this is the same as calling <code>flush(producerIndex, false)</code>.</p>
	 * 
	 * @param producerIndex the index of the producer to use
	 */
	public void flush(int producerIndex);
	
	/**
	 * <p>Return the number of objects that can be safely polled from this mpmc consumer. The consumer thread calling this method must pass its consumer index.</p>
	 * 
	 * <p>If the mpmc is empty, this method returns 0.</p>
	 * 
	 * @param consumerIndex the index of the consumer thread calling this method
	 * @return number of objects that can be polled
	 */
	public long availableToPoll(int consumerIndex);
	
	/**
	 * <p>Poll an object from the mpmc. You can only call this method after calling {@link #availableToPoll(int)} so you
	 * know for sure what is the maximum number of times you can call this method. The consumer thread calling this method must pass its consumer index.</p>
	 * 
	 * <p><b>NOTE:</b> You must <b>never</b> keep your own reference to the mutable object returned by this method.
	 * Read what you need to read from the object and release its reference.
	 * The object returned should be treated as a <i>data transfer mutable object</i> therefore you should read what you need from it and let it go.</p>
	 * 
	 * @param consumerIndex the index of the consumer thread calling this method
	 * @return a data transfer mutable object from the mpmc
	 */
	public E poll(int consumerIndex);
	
	/**
	 * <p>Must be called to indicate that all polling has been concluded, in other words, 
	 * you poll what you can/want to poll and call this method to signal the producer threads that you are done.
	 * The consumer thread calling this method must pass its consumer index.</p>
	 * 
	 * @param consumerIndex he index of the consumer thread calling this method
	 * @param lazySet true to notify the producers in a lazy way or false to notify the producers <b>immediately</b>
	 */
	public void donePolling(int consumerIndex, boolean lazySet);
	
	/**
	 * <p>That's the same as calling <code>donePolling(consumerIndex, false)</code>, in other words, the producers will be notified <b>immediately</b> that polling is done.
	 * The consumer thread calling this method must pass its consumer index.</p>
	 * 
	 * @param consumerIndex the index of the consumer thread calling this method
	 */
	public void donePolling(int consumerIndex);
	
	/**
	 * Return the producer corresponding to the given index. If a bad index is given this method throws a <code>RuntimeException</code>.
	 * 
	 * @param index the producer index
	 * @return the producer for the given index
	 */
	public Producer<E> getProducer(int index);
	
	/**
	 * Return the consumer corresponding to the given index. If a bad index is given this method throws a <code>RuntimeException</code>.
	 * 
	 * @param index the consumer index
	 * @return the consumer for the given index
	 */
	public Consumer<E> getConsumer(int index);
	
	/**
	 * Return the fixed number of consumers that this mpmc has.
	 * 
	 * @return the fixed number of consumers
	 */
	public int getNumberOfConsumers();
	
	/**
	 * Return the fixed number of producers that this mpmc has.
	 * 
	 * @return the fixed number of producers
	 */
	public int getNumberOfProducers();
}
