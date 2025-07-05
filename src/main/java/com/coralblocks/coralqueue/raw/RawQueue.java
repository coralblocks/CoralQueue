/* 
 * Copyright 2015-2024 (c) CoralBlocks LLC - http://www.coralblocks.com
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
package com.coralblocks.coralqueue.raw;

/**
 * A queue implementation that uses {@link RawBytes} to transfer data from producer (i.e. writer) to consumer (i.e. reader).
 */
public interface RawQueue {
	
	/**
	 * Clear this queue so that it can be re-used
	 */
	public void clear();
	
	/**
	 * Return the space in bytes that you can write to
	 * 
	 * @return the space available to receive data in bytes
	 */
	public long availableToWrite();

	/**
	 * Return a {@link RawBytes} that you can write to (i.e. the producer)
	 * 
	 * @return the {@link RawBytes} producer (i.e. writer)
	 */
	public RawBytes getProducer();
	
	/**
	 * Inform the consumer about the new data written to the queue
	 * 
	 * @param lazySet true to flush (i.e. notify the consumer) in a lazy way or false to flush <b>immediately</b>
	 */
	public void flush(boolean lazySet);
	
	/**
	 * Inform the consumer immediately about the new data written to the queue
	 */
	public void flush();
	
	/**
	 * Return the number of bytes from the queue available to be read
	 * 
	 * @return the number of bytes that can be read from the queue by the consumer (i.e. reader)
	 */
	public long availableToRead();
	
	/**
	 * Return a {@link RawBytes} that you can read from (i.e. the consumer)
	 * 
	 * @return the {@link RawBytes} consumer (i.e. reader)
	 */
	public RawBytes getConsumer();

	/**
	 * Inform the producer that some bytes were read from the queue
	 * 
	 * @param lazySet true to notify the producer in a lazy way or false to notify the producer <b>immediately</b>
	 */
	public void doneReading(boolean lazySet);
	
	/**
	 * Inform the producer immediately that some bytes were read from the queue
	 */
	public void doneReading();
}