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
package com.coralblocks.coralqueue.multiplexer;

/**
 * <p>The Multiplexer API that allows multiple producer threads sending messages to the multiplexer and a single consumer thread receiving messages from the multiplexer.</p>
 * 
 * <p><b>NOTE:</b> A multiplexer must have a <b>fixed</b> number of producers specified by its constructor.</p>
 *
 * @param <E> The mutable transfer object to be used by this multiplexer
 */
public interface Multiplexer<E> {
	
	/**
	 * <p>Clear the multiplexer, so that it can be re-used.</p>
	 * 
	 * <p>Make sure you only call this method when the multiplexer is idle, in other words, when you are sure
	 * there are currently no threads accessing the multiplexer. Also note that the consumer thread must be dead or you
	 * might run into visibility problems.</p>
	 */
	public void clear();

	/**
	 * <p>Return the next mutable object that can be used by the given producer to dispatch data to the multiplexer.</p>
	 * 
	 * <p>If no object is currently available (i.e. the multiplexer is full) this method returns null.</p>
	 * 
	 * @param producer the zero-based index of the producer to be used
	 * @return the next mutable object that can be used by the producer or null if the multiplexer is full
	 */
	public E nextToDispatch(int producer);
	
	/**
	 * <p>Same as {@link #nextToDispatch(int)} but it replaces/swaps the object that is returned by the given <code>swap</code> object, inside the multiplexer.</p>
	 * 
	 * @param producer the zero-based index of the producer to be used
	 * @param swap the object that will replace the returned object inside the multiplexer
	 * @return the object that was in the multiplexer and was replaced by the given object
	 */
	public E nextToDispatch(int producer, E swap);

	/**
	 * <p>Dispatch/Flush all previously obtained objects through the {@link #nextToDispatch(int)} method to the consumer.</p>
	 * 
	 * @param producer the zero-based index of the producer to be used
	 * @param lazySet true to flush (i.e. notify the consumer) in a lazy way or false to flush <b>immediately</b>
	 */
	public void flush(int producer, boolean lazySet);
	
	/**
	 * <p>Dispatch <b>immediately</b> all previously obtained objects through the {@link #nextToDispatch(int)} method to the consumer.
	 * Note that this is the same as calling <code>flush(producer, false)</code>.</p>
	 * 
	 * @param producer the zero-based index of the producer to be used
	 */
	public void flush(int producer);
	
	/**
	 * <p>Return the number of objects that can be safely polled from this multiplexer.</p>
	 * 
	 * <p>If the multiplexer is empty, this method returns 0.</p>
	 * 
	 * @return number of objects that can be polled
	 */
	public long availableToPoll();
	
	/**
	 * <p>Poll an object from the multiplexer. You can only call this method after calling {@link #availableToPoll()} so you
	 * know for sure what is the maximum number of times you can call this method.</p>
	 * 
	 * <p><b>NOTE:</b> You must <b>never</b> keep your own reference to the mutable object returned by this method.
	 * Read what you need to read from the object and release its reference.
	 * The object returned should be treated as a <i>data transfer object</i> therefore you should read what you need from it and let it go.</p>
	 * 
	 * @return a data transfer object from the multiplexer
	 */
	public E poll();

	/**
	 * <p>Must be called to indicate that all polling has been concluded, in other words, 
	 * you poll what you can/want to poll and call this method to signal the producer threads that you are done.</p>
	 * 
	 * @param lazySet true to notify the producers in a lazy way or false to notify the producers <b>immediately</b>
	 */
	public void donePolling(boolean lazySet);
	
	/**
	 * <p>That's the same as calling <code>donePolling(false)</code>, in other words, the producers will be notified <b>immediately</b> that polling is done.</p>
	 */
	public void donePolling();
	
	/**
	 * The (fixed) number of producers that this multiplexer has.
	 *  
	 * @return the number of producers
	 */
	public int getNumberOfProducers();
	
	/**
	 * Convenient method to get the producers. It returns <code>null</code> when all producers have been returned.
	 * 
	 * @return the next producer from the list of all producers of this multiplexer
	 */
	public Producer<E> nextProducer();
	
	/**
	 * Return a producer by its index. This method throws a <code>RuntimeException</code> if the index is invalid.
	 * 
	 * @param index the zero-based index of the producer to be used
	 * @return the producer
	 */
	public Producer<E> getProducer(int index);
}