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
package com.coralblocks.coralqueue.broadcaster;

/**
 * <p>The Broadcaster API that is a special demultiplexer that broadcasts (delivers) all messages to all consumers, in other words, all consumers will fetch and receive all messages sent by the producer.</p>
 * 
 * <p><b>NOTE:</b> A broadcaster must have a <b>fixed</b> number of consumers specified by its constructor.</p>
 *
 * @param <E> The data transfer mutable object to be used by this broadcaster
 */
public interface Broadcaster<E> {
	
	/**
	 * <p>Clear the broadcaster, so that it can be re-used.</p>
	 * 
	 * <p>Make sure you only call this method when the broadcaster is idle, in other words, when you are sure
	 * there are currently no threads accessing the broadcaster. Also note that all consumer threads must be dead or you
	 * might run into visibility problems.</p>
	 */
	public void clear();

	/**
	 * <p>Return the next mutable object that can be used by the producer to dispatch data to the broadcaster.</p>
	 * 
	 * <p>If no object is currently available (i.e. the broadcaster is full) this method returns null.</p>
	 * 
	 * @return the next mutable object that can be used by the producer or null if the broadcaster is full
	 */
	public E nextToDispatch();

	/**
	 * <p>Dispatch/Flush all previously obtained objects through the {@link #nextToDispatch()} method to the consumers.</p>
	 * 
	 * @param lazySet true to flush (i.e. notify the consumers) in a lazy way or false to flush <b>immediately</b>
	 */
	public void flush(boolean lazySet);
	
	/**
	 * <p>Dispatch <b>immediately</b> all previously obtained objects through the {@link #nextToDispatch()} method to the consumers.
	 * Note that this is the same as calling <code>flush(false)</code>.</p>
	 */
	public void flush();
	
	/**
	 * <p>Return the number of objects that can be safely fetched from this broadcaster. The consumer thread calling this method must pass its consumer index.</p>
	 * 
	 * <p>If the broadcaster is empty, this method returns 0.</p>
	 * 
	 * @param consumerIndex the index of the consumer thread calling this method
	 * @return number of objects that can be fetched
	 */
	public long availableToFetch(int consumerIndex);

	/**
	 * <p>Fetch an object from the broadcaster. You can only call this method after calling {@link #availableToFetch(int)} so you
	 * know for sure what is the maximum number of times you can call this method. The consumer thread calling this method must pass its consumer index.</p>
	 * 
	 * <p><b>NOTE:</b> You must <b>never</b> keep your own reference to the mutable object returned by this method.
	 * Read what you need to read from the object and release its reference.
	 * The object returned should be treated as a <i>data transfer mutable object</i> therefore you should read what you need from it and let it go.</p>
	 * 
	 * @param consumerIndex the index of the consumer thread calling this method
	 * @return a data transfer mutable object from the broadcaster
	 */
	public E fetch(int consumerIndex);

	/**
	 * <p>Must be called to indicate that all fetching has been concluded, in other words, 
	 * you fetch what you can/want to fetch and call this method to signal the producer thread that you are done.
	 * The consumer thread calling this method must pass its consumer index.</p>
	 * 
	 * @param consumerIndex he index of the consumer thread calling this method
	 * @param lazySet true to notify the producer in a lazy way or false to notify the producer <b>immediately</b>
	 */
	public void doneFetching(int consumerIndex, boolean lazySet);
	
	/**
	 * <p>That's the same as calling <code>doneFetching(consumerIndex, false)</code>, in other words, the producer will be notified <b>immediately</b> that fetching is done.
	 * The consumer thread calling this method must pass its consumer index.</p>
	 * 
	 * @param consumerIndex the index of the consumer thread calling this method
	 */
	public void doneFetching(int consumerIndex);
	
	/**
	 * <p>Pretend you never fetched any objects since you last called {@link #doneFetching(int)}. This method cancels (i.e. rolls back) any fetching operations you have done.
	 * The consumer thread calling this method must pass its consumer index.</p>
	 * 
	 * <p>You can call this method as many times as you want before you call {@link #doneFetching(int)} to roll back any fetching operations (zero, one or more) you have done.</p>
	 * 
	 * @param consumerIndex the index of the consumer thread calling this method
	 */
	public void rollBack(int consumerIndex);
	
	/**
	 * <p>Same as {@link #rollBack(int)} but allows you to specify how many previous fetches you want to roll back, instead of all of them (i.e. all previous ones).
	 * The consumer thread calling this method must pass its consumer index.</p>
	 * 
	 * @param consumerIndex the index of the consumer thread calling this method
	 * @param items how many fetches to roll back
	 */
	public void rollBack(int consumerIndex, long items);
	
	/**
	 * <p>Return the next object to be fetched without actually fetching it.
	 * The consumer thread calling this method must pass its consumer index.</p>

	 * @param consumerIndex the index of the consumer thread calling this method
	 * @return the next object to be fetched from the broadcaster, without actually fetching it
	 */
	public E peek(int consumerIndex);
	
	/**
	 * <p>The (fixed) number of consumers listening on this broadcaster.</p>
	 * 
	 * @return the number of consumers
	 */
	public int getNumberOfConsumers();
	
	/**
	 * <p>This method disables a consumer and allows the broadcaster to continue to operate and make progress without getting blocked
	 * waiting for a slow consumer. This is useful for when a consumer has a problem and stops fetching the broadcaster. In that situation
	 * the broadcaster will get full, causing the producer to block, unless you disable the consumer.</p>
	 * 
	 * @param consumerIndex the index of the consumer that you want to disable
	 */
	public void disableConsumer(int consumerIndex);
	
	/**
	 * Return a consumer by its index. This method throws a <code>RuntimeException</code> if the index is invalid.
	 * 
	 * @param consumerIndex the zero-based index of the consumer to be returned
	 * @return the consumer
	 */
	public Consumer<E> getConsumer(int consumerIndex);
}