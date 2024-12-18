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
package com.coralblocks.coralqueue.broadcaster;

/**
 * A consumer to receive messages from this broadcaster. 
 *
 * @param <E> The data transfer mutable object to be used by this broadcaster
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
	 * @return the number of available objects that can be fetched
	 */
	public final long availableToFetch() {
		return broadcaster.availableToFetch(consumerIndex);
	}
	
	/**
	 * Delegate to the broadcaster
	 * 
	 * @return the object fetched
	 */
	public final E fetch() {
		return broadcaster.fetch(consumerIndex);
	}
	
	/**
	 * Delegate to the broadcaster
	 * 
	 * @param lazySet false to signal to the producer immediately
	 */
	public final void doneFetching(boolean lazySet) {
		broadcaster.doneFetching(consumerIndex, lazySet);
	}
	
	/**
	 * Delegate to the broadcaster
	 */
	public final void doneFetching() {
		broadcaster.doneFetching(consumerIndex);
	}
	
	/**
	 * Disable this consumer by delegating to the <code>disableConsumer(int)</code> method of this broadcaster
	 */
	public final void disable() {
		broadcaster.disableConsumer(consumerIndex);
	}
}
