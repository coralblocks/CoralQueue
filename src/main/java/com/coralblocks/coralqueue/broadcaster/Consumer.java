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
 * A consumer to receive messages from this broadcaster. 
 *
 * @param <E> The mutable transfer object to be used by this broadcaster
 */
public class Consumer<E> {
	
	private final int consumerIndex;
	private final Broadcaster<E> broadcaster;
	
	Consumer(Broadcaster<E> broadcaster, int consumerIndex) {
		this.broadcaster = broadcaster;
		this.consumerIndex = consumerIndex;
	}
	
	/**
	 * Return the consumer index
	 * 
	 * @return the consumer index 
	 */
	public final int getIndex() {
		return consumerIndex;
	}
	
	/**
	 * Delegate to the broadcaster
	 * 
	 * @return the number of available objects that can be polled
	 */
	public final long availableToPoll() {
		return broadcaster.availableToPoll(consumerIndex);
	}
	
	/**
	 * Delegate to the broadcaster
	 * 
	 * @return the object polled
	 */
	public final E poll() {
		return broadcaster.poll(consumerIndex);
	}
	
	/**
	 * Delegate to the broadcaster
	 * 
	 * @param lazySet false to signal to the producer immediately
	 */
	public final void donePolling(boolean lazySet) {
		broadcaster.donePolling(consumerIndex, lazySet);
	}
	
	/**
	 * Delegate to the broadcaster
	 */
	public final void donePolling() {
		broadcaster.donePolling(consumerIndex);
	}
	
	/**
	 * Disable this consumer by delegating to the <code>disableConsumer(int)</code> method of this broadcaster
	 */
	public final void disable() {
		broadcaster.disableConsumer(consumerIndex);
	}
}