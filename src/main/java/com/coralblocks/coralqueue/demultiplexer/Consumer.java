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
 * A holder for a demultiplexer consumer. It contains a reference to the demultiplexer and the consumer index.
 * 
 * @param <E> The mutable transfer object to be used by this demultiplexer
 */
public class Consumer<E> {
	
	private final int index;
	private final Demultiplexer<E> demux;
	
	Consumer(Demultiplexer<E> demux, int index) {
		this.index = index;
		this.demux = demux;
	}
	
	/**
	 * Return the consumer index
	 * 
	 * @return the consumer index
	 */
	public final int getIndex() {
		return index;
	}
	
	/**
	 * Delegates to the demultiplexer with the right consumer index.
	 * 
	 * @return the number of available objects to poll
	 */
	public final long availableToPoll() {
		return demux.availableToPoll(index);
	}
	
	/**
	 * Delegates to the demultiplexer with the right consumer index.
	 * 
	 * @return the polled object
	 */
	public final E poll() {
		return demux.poll(index);
	}
	
	/**
	 * Delegates to the demultiplexer with the right consumer index.
	 * 
	 * @param lazySet true to notify later or false to notify immediately
	 */
	public final void donePolling(boolean lazySet) {
		demux.donePolling(index, lazySet);
	}
	
	/**
	 * Delegates to the demultiplexer with the right consumer index.
	 */
	public final void donePolling() {
		demux.donePolling(index);
	}
}
