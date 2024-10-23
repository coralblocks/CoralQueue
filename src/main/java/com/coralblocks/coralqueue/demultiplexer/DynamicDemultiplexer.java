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
package com.coralblocks.coralqueue.demultiplexer;

/**
 * <p>The DynamicDemultiplexer API that allows multiple consumer threads receiving messages from the demultiplexer and a single producer thread sending messages to the demultiplexer.
 * Two different consumers will never poll the same message.</p>
 * 
 * <p><b>NOTE:</b> A dynamic demultiplexer allows new consumers to join the party at any moment and should use an <code>IdentityMap + Thread.currentThread()</code> to maintain the consumers.</p>
 *
 * @param <E> The mutable transfer object to be used by this demultiplexer
 */
public interface DynamicDemultiplexer<E> {
	
	/**
	 * <p>Clear the demultiplexer, so that it can be re-used.</p>
	 * 
	 * <p>Make sure you only call this method when the demultiplexer is idle, in other words, when you are sure
	 * there are currently no threads accessing the demultiplexer. Also note that all consumer threads must be dead or you
	 * might run into visibility problems.</p>
	 */
	public void clear();

	/**
	 * <p>Return the next mutable object that can be used by the producer to dispatch data to the demultiplexer.</p>
	 * 
	 * <p>If no object is currently available (i.e. the demultiplexer is full) this method returns null.</p>
	 * 
	 * @return the next mutable object that can be used by the producer or null if the demultiplexer is full
	 */
	public E nextToDispatch();
	
	/**
	 * <p>Return the next mutable object that can be used by the producer to dispatch data to the demultiplexer.
	 * This method allows you to specify the consumer that you want to receive the message.</p>
	 * 
	 * <p>If no object is currently available (i.e. the demultiplexer is full) this method returns null.</p>
	 * 
	 * @param consumerIndex the consumer that you want to receive the message
	 * @return the next mutable object that can be used by the producer or null if the demultiplexer is full
	 */
	public E nextToDispatch(int consumerIndex);

	/**
	 * <p>Dispatch/Flush all previously obtained objects through the {@link #nextToDispatch()} method to the consumer.</p>
	 * 
	 * @param lazySet true to flush (i.e. notify the consumer) in a lazy way or false to flush <b>immediately</b>
	 */
	public void flush(boolean lazySet);
	
	/**
	 * <p>Dispatch <b>immediately</b> all previously obtained objects through the {@link #nextToDispatch()} method to the consumer.
	 * Note that this is the same as calling <code>flush(false)</code>.</p>
	 */
	public void flush();
	
	/**
	 * <p>Return the number of objects that can be safely polled from this demultiplexer.</p>
	 * 
	 * <p>If the demultiplexer is empty, this method returns 0.</p>
	 * 
	 * @return number of objects that can be polled
	 */
	public long availableToPoll();
	
	/**
	 * <p>Poll an object from the demultiplexer. You can only call this method after calling {@link #availableToPoll()} so you
	 * know for sure what is the maximum number of times you can call this method.</p>
	 * 
	 * <p><b>NOTE:</b> You must <b>never</b> keep your own reference to the mutable object returned by this method.
	 * Read what you need to read from the object and release its reference.
	 * The object returned should be treated as a <i>data transfer object</i> therefore you should read what you need from it and let it go.</p>
	 * 
	 * @return a data transfer object from the demultiplexer
	 */
	public E poll();
	
	/**
	 * <p>Replace the last polled object by this consumer by the given one.</p>
	 * 
	 * @param newVal the new object to replace the last polled object from this demultiplexer
	 */
	public void replace(E newVal);

	/**
	 * <p>Must be called to indicate that all polling has been concluded, in other words, 
	 * you poll what you can/want to poll and call this method to signal the producer thread that you are done.</p>
	 * 
	 * @param lazySet true to notify the producer in a lazy way or false to notify the producer <b>immediately</b>
	 */
	public void donePolling(boolean lazySet);
	
	/**
	 * <p>That's the same as calling <code>donePolling(consumerIndex, false)</code>, in other words, the producers will be notified <b>immediately</b> that polling is done.</p>
	 */
	public void donePolling();
	
	/**
	 * The current number of consumers that this demultiplexer has.
	 *  
	 * @return the current number of consumers
	 */
	public int getNumberOfConsumers();
}