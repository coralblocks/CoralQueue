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
 * A special demultiplexer that broadcasts (delivers) all messages to all consumers, in other words, all consumers will poll and receive all messages sent by the producer.
 *
 * @param <E>
 */
public interface Broadcaster<E> {
	
	public void clear();

	/**
	 * Return the next pooled mutable object that can be used by the producer to dispatch data to the splitter.
	 * 
	 * @return the next mutable object that can be used by the producer.
	 */
	public E nextToDispatch();

	/**
	 * Dispatch all previously obtained objects through the <i>nextToDispatch()</i> method to the splitter.
	 * 
	 * @param lazySet flush (i.e. notify the consumers) in a lazy way or flush immediately
	 */
	public void flush(boolean lazySet);
	
	/**
	 * Dispatch *immediately* all previously obtained objects through the <i>nextToDispatch()</i> method to the splitter.
	 */
	public void flush();
	
	/**
	 * Return the number of objects that can be safely polled from this splitter by this consumer. It can return zero.
	 * 
	 * @param consumer the consumer index
	 * 
	 * @return number of objects that can be polled.
	 */
	public long availableToPoll(int consumer);

	/**
	 * Poll a object from the splitter. You can only call this method after calling <i>availableToPoll()</i> so you
	 * know what is the maximum times you can call it.
	 * 
	 * NOTE: You should NOT keep your own reference for this mutable object. Read what you need to get from it and release its reference.
	 * 
	 * @param consumer the consumer index
	 * 
	 * @return an object from the splitter.
	 */
	public E poll(int consumer);

	/**
	 * Called to indicate that all polling have been concluded.
	 * 
	 * @param consumer the consumer index
	 * @param lazySet notify the producer in a lazy way or notify the producer immediately
	 */
	public void donePolling(int consumer, boolean lazySet);
	
	/**
	 * Called to indicate that all polling have been concluded.
	 * 
	 * @param consumer the consumer index
	 */
	public void donePolling(int consumer);
	
	/**
	 * Pretend you never polled the last object you polled since the last time you called donePolling().
	 * You can call this as many times as you want before you call donePolling() and rollback any poll() you have done.
	 * This is unaffected by availableToPoll(). Only donePolling() reset the counter of the last polled objects.
	 * Because rollback() reset the counter of the last polled objects, you can even call it twice in a row and the second
	 * rollback() will have no effect since you have polled anything.
	 * 
	 * @param consumer the consumer index
	 * 
	 */
	public void rollBack(int consumer);
	
	/**
	 * Same as rollback but allows you to specify how many previous polls you want to rollback
	 * 
	 * @param consumer the consumer index
	 * @param items how many polls to rollback
	 */
	public void rollBack(int consumer, long items);
	
	/**
	 * Return the element from the pool without removing it.
	 * 
	 * @param consumer the consumer index
	 * 
	 * @return the next available object to be polled from the queue.
	 */
	public E peek(int consumer);
	
	/**
	 * The number of consumers listening on this splitter.
	 * 
	 * @return the number of consumers
	 */
	public int getNumberOfConsumers();
	
	/**
	 * This will disable a consumer and allow the splitter to continue to operate without getting full.
	 * This is useful for when a consumer has a problem and stops polling the splitter. In that situation
	 * the internal queue will get full unless you disable the consumer.
	 * 
	 * @param index
	 */
	public void disableConsumer(int index);
	
	public Consumer<E> getConsumer(int index);
}