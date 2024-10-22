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
 * A queue API that allows batching and pooling of objects.
 * 
 *  So to offer to the queue, you first get a mutable object from the queue by calling <code>nextToDispatch()</code>, alter this object and call <code>flush(boolean lazySet)</code>.
 *  That allows the queue objects to be pooled, avoiding any garbage collection.<br/>
 *  <br/>
 *  And to poll you first call <i>availableToPoll()</i> to know how many objects you can safely poll, call <i>poll()</i> in a loop and when done call <i>donePolling(boolean lazySet)</i>.
 *  That allows polling to be batched, so you pay a synchronization price only when you call <i>available()</i> and NOT when you call <i>poll()</i>.<br/>
 *
 * @param <E>
 */
public interface Queue<E> {
	
	/**
	 * Clear the queue. Make sure you only call this method when the queue is idle, in other words, when you are sure
	 * there are currently no threads accessing the queue. Also note that all consumer threads must be dead or you
	 * might run into visibility problems.
	 */
	public void clear();
	

	/**
	 * Return the next pooled mutable object that can be used by the producer to dispatch data to the queue.
	 * 
	 * If no object is currently available (i.e. queue is full) this method returns null.
	 * 
	 * @return the next mutable object that can be used by the producer.
	 */
	public E nextToDispatch();
	
	public E nextToDispatch(E swap);
	
	/**
	 * Dispatch all previously obtained objects through the <i>nextToDispatch()</i> method to the queue.
	 * 
	 * @param lazySet flush (i.e. notify the consumer) in a lazy way or flush immediately
	 */
	public void flush(boolean lazySet);
	
	/**
	 * Dispatch *immediately* all previously obtained objects through the <i>nextToDispatch()</i> method to the queue.
	 */
	public void flush();
	
	/**
	 * Return the number of objects that can be safely polled from this queue.
	 * 
	 * If the queue is empty, this method returns 0.
	 * 
	 * @return number of objects that can be polled.
	 */
	public long availableToPoll();

	/**
	 * Poll a object from the queue. You can only call this method after calling <i>availableToPoll()</i> so you
	 * know what is the maximum times you can call it.
	 * 
	 * NOTE: You should NEVER keep your own reference to this mutable object. Read what you need to read from it and release its reference.
	 * The object returned should be a data transfer object.
	 * 
	 * @return a data transfer object from the queue.
	 */
	public E poll();
	
	public void replace(E newVal);

	/**
	 * Called to indicate that all polling have been concluded.
	 * 
	 * @param lazySet notify the producer in a lazy way or notify the producer immediately
	 */
	public void donePolling(boolean lazySet);
	
	/**
	 * Called to indicate that all polling have been concluded.
	 */
	public void donePolling();
	
	/**
	 * Pretend you never polled the last object you polled since the last time you called donePolling().
	 * You can call this as many times as you want before you call donePolling() and rollback any poll() you have done.
	 * This is unaffected by availableToPoll(). Only donePolling() reset the counter of the last polled objects.
	 * Because rollback() reset the counter of the last polled objects, you can even call it twice in a row and the second
	 * rollback() will have no effect since you have polled anything.
	 */
	public void rollback();
	
	/**
	 * Same as rollback but allows you to specify how many previous polls you want to rollback
	 * 
	 * @param items how many polls to rollback
	 */
	public void rollback(long items);
	
	/**
	 * Return the element from the pool without removing it.
	 * 
	 * @return the next available object to be polled from the queue.
	 */
	public E peek();
}