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

/**
 * <p>The MpMcBroadcaster (Multiple Producers / Multiple Consumers) API that allows multiple consumer threads receiving messages from the mpmc and multiple producer threads sending messages to the mpmc.
 * All consumers receive all messages sent by the producers.</p>
 * 
 * <p><b>NOTE:</b> A mpmc broadcaster must have a <b>fixed</b> number of consumers and a fixed number of producers specified by its constructor.</p>
 *
 * @param <E> The data transfer mutable object to be used by this mpmc broadcaster
 */
public interface MpMcBroadcaster<E> {
	
	/**
	 * <p>Clear the mpmc broadcaster, so that it can be re-used.</p>
	 * 
	 * <p>Make sure you only call this method when the mpmc broadcaster is idle, in other words, when you are sure
	 * there are currently no threads accessing the mpmc broadcaster. Also note that all consumer threads must be dead or you
	 * might run into visibility problems.</p>
	 */
	public void clear();
	
	/**
	 * <p>Return the next mutable object that can be used by the given producer to dispatch data to the mpmc broadcaster. The producer thread calling this method must pass its producer index.</p>
	 * 
	 * <p>If no object is currently available (i.e. the mpmc broadcaster is full) this method returns null.</p>
	 * 
	 * @param producerIndex the index of the producer to use
	 * @return the next mutable object that can be used by the given producer or null if the mpmc broadcaster is full
	 */
	public E nextToDispatch(int producerIndex);
	
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
	 * <p>Return the number of objects that can be safely fetched from this mpmc broadcaster consumer. The consumer thread calling this method must pass its consumer index.</p>
	 * 
	 * <p>If the mpmc broadcaster is empty, this method returns 0.</p>
	 * 
	 * @param consumerIndex the index of the consumer thread calling this method
	 * @return number of objects that can be fetched
	 */
	public long availableToFetch(int consumerIndex);
	
	/**
	 * <p>Fetch an object from the mpmc broadcaster. You can only call this method after calling {@link #availableToFetch(int)} so you
	 * know for sure what is the maximum number of times you can call this method. The consumer thread calling this method must pass its consumer index.</p>
	 * 
	 * <p><b>NOTE:</b> You must <b>never</b> keep your own reference to the mutable object returned by this method.
	 * Read what you need to read from the object and release its reference.
	 * The object returned should be treated as a <i>data transfer mutable object</i> therefore you should read what you need from it and let it go.</p>
	 * 
	 * @param consumerIndex the index of the consumer thread calling this method
	 * @return a data transfer mutable object from the mpmc broadcaster
	 */
	public E fetch(int consumerIndex);
	
	/**
	 * <p>Must be called to indicate that all fetching has been concluded, in other words, 
	 * you fetch what you can/want to fetch and call this method to signal the producer threads that you are done.
	 * The consumer thread calling this method must pass its consumer index.</p>
	 * 
	 * @param consumerIndex he index of the consumer thread calling this method
	 * @param lazySet true to notify the producers in a lazy way or false to notify the producers <b>immediately</b>
	 */
	public void doneFetching(int consumerIndex, boolean lazySet);
	
	/**
	 * <p>That's the same as calling <code>doneFetching(consumerIndex, false)</code>, in other words, the producers will be notified <b>immediately</b> that fetching is done.
	 * The consumer thread calling this method must pass its consumer index.</p>
	 * 
	 * @param consumerIndex the index of the consumer thread calling this method
	 */
	public void doneFetching(int consumerIndex);
	
	/**
	 * <p>This method disables a consumer and allows the mpmc broadcaster to continue to operate and make progress without getting full.
	 * This is useful for when a consumer has a problem and stops fetching the mpmc broadcaster. In that situation
	 * the mpmc broadcaster will get full unless you disable the consumer.</p>
	 * 
	 * @param consumerIndex the index of the consumer that you want to disable
	 */
	public void disableConsumer(int consumerIndex);
	
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
	 * Return the fixed number of consumers that this mpmc broadcaster has.
	 * 
	 * @return the fixed number of consumers
	 */
	public int getNumberOfConsumers();
	
	/**
	 * Return the fixed number of producers that this mpmc broadcaster has.
	 * 
	 * @return the fixed number of producers
	 */
	public int getNumberOfProducers();
}
