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
package com.coralblocks.coralqueue;

/**
 * <p>A Queue API that allows offering and polling objects to and from the queue. Implementations should naturally/natively support batching (for speed) and pooling (for zero garbage).
 * The objects must be mutable to act like <i>transfer objects</i>.
 * The circular queue is fully populated with these objects at startup.</p>
 * 
 *  <p>So to offer to the queue, you first get a mutable object from the queue by calling {@link #nextToDispatch()}, modify this object and then call {@link #flush(boolean)} or {@link #flush()}.
 *  That allows the producer to send in batches if it wants to.</p>
 *  
 *  <p>And to poll you first call {@link #availableToPoll()} to know how many objects you can safely poll, call {@link #poll()} in a loop and when done call {@link #donePolling(boolean)} or {@link #donePolling()}.
 *  That allows the consumer to receive in batches if it wants to.</p>
 *  
 *  <p><b>NOTE:</b> This queue is intended to be used by only one producer thread and by only one consumer thread (i.e one-to-one). For other thread scenarios you should check Demux, Mux, Mpmc, etc.</p>
 *
 * @param <E> The mutable transfer object to be used by this queue
 */
public interface Queue<E> {
	
	/**
	 * <p>Clear the queue, so that it can be re-used.</p>
	 * 
	 * <p>Make sure you only call this method when the queue is idle, in other words, when you are sure
	 * there are currently no threads accessing the queue. Also note that all consumer threads must be dead or you
	 * might run into visibility problems.</p>
	 */
	public void clear();
	

	/**
	 * <p>Return the next mutable object that can be used by the producer to dispatch data to the queue.</p>
	 * 
	 * <p>If no object is currently available (i.e. the queue is full) this method returns null.</p>
	 * 
	 * @return the next mutable object that can be used by the producer or null if the queue is full
	 */
	public E nextToDispatch();
	
	/**
	 * <p>Same as {@link #nextToDispatch()} but it replaces/swaps the object that is returned by the given <code>swap</code> object, inside the circular queue.</p>
	 * 
	 * @param swap the object that will replace the returned object inside the circular queue
	 * @return the object that was in the queue and was replaced by the given object
	 */
	public E nextToDispatch(E swap);
	
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
	 * <p>Return the number of objects that can be safely polled from this queue.</p>
	 * 
	 * <p>If the queue is empty, this method returns 0.</p>
	 * 
	 * @return number of objects that can be polled
	 */
	public long availableToPoll();

	/**
	 * <p>Poll an object from the queue. You can only call this method after calling {@link #availableToPoll()} so you
	 * know for sure what is the maximum number of times you can call this method.</p>
	 * 
	 * <p><b>NOTE:</b> You must <b>never</b> keep your own reference to the mutable object returned by this method.
	 * Read what you need to read from the object and release its reference.
	 * The object returned should be treated as a <i>data transfer object</i> therefore you should read what you need from it and let it go.</p>
	 * 
	 * @return a data transfer object from the queue
	 */
	public E poll();
	
	/**
	 * aaaa
	 * 
	 * @param newVal bbb
	 */
	public void replace(E newVal);

	/**
	 * <p>Must be called to indicate that all polling have been concluded, in other words, 
	 * you poll what you can/want to poll and call this method to signal the producer that you are done.</p>
	 * 
	 * @param lazySet true to notify the producer in a lazy way or false to notify the producer <b>immediately</b>
	 */
	public void donePolling(boolean lazySet);
	
	/**
	 * <p>That's the same as calling <code>donePolling(false)</code>, in other words, the producer will be notified <b>immediately</b> that polling is done.</p>
	 */
	public void donePolling();
	
	/**
	 * <p>Return the next object to be polled without actually polling it.</p>
	 * 
	 * @return the next object to be polled from the queue, without actually polling it
	 */
	public E peek();
	
	/**
	 * <p>Pretend you never polled any objects since you last called {@link #donePolling()}. This method cancels (i.e. rolls back) any polling operations you have done.</p>
	 * <p>You can call this method as many times as you want before you call {@link #donePolling()} to roll back any polling operations (zero, one or more) you have done.</p>
	 */
	public void rollBack();
	
	/**
	 * <p>Same as {@link #rollBack()} but allows you to specify how many previous polls you want to roll back, instead of all of them (i.e. all previous ones).</p>
	 * 
	 * @param items how many polls to roll back
	 */
	public void rollBack(long items);
}