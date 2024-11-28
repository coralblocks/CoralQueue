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
package com.coralblocks.coralqueue.mpmcbroadcaster;

/**
 * A consumer holder for the {@link MpMcBroadcaster}. It contains a broadcaster {@link com.coralblocks.coralqueue.broadcaster.Consumer} and the consumer index.
 * 
 * Its API is the same as a regular {@link com.coralblocks.coralqueue.queue.Queue}.
 * 
 * @param <E> The data transfer mutable object to be used by this mpmc broadcaster
 */
public class Consumer<E> {
	
	private final com.coralblocks.coralqueue.broadcaster.Consumer<E>[] consumers;
	private int currConsumerIndex = 0;
	private final int index;
	private final int nConsumers;
	private final long[] availToFetch;
	private final boolean[] needsDoneFetching;
	
	Consumer(com.coralblocks.coralqueue.broadcaster.Consumer<E>[] consumers, int index) {
		this.consumers = consumers;
		this.index = index;
		this.nConsumers = consumers.length;
		this.availToFetch = new long[consumers.length];
		this.needsDoneFetching = new boolean[consumers.length];
	}
	
	/**
	 * See {@link com.coralblocks.coralqueue.queue.Queue#availableToFetch()} for more details.
	 * 
	 * @return the number of objects that can be fetched
	 */
	public final long availableToFetch() {
		long total = 0;
		for(int i = 0; i < nConsumers; i++) {
			long x = consumers[i].availableToFetch();
			availToFetch[i] = x;
			total += x;
			needsDoneFetching[i] = false;
		}
		currConsumerIndex = 0;
		return total;
	}
	
	/**
	 * See {@link com.coralblocks.coralqueue.queue.Queue#fetch()} for more details.
	 * 
	 * @return a data transfer mutable object from the queue
	 */
	public final E fetch() {
		while(true) {
			if (availToFetch[currConsumerIndex] > 0) {
				E e = consumers[currConsumerIndex].fetch();
				needsDoneFetching[currConsumerIndex] = true;
				if (e == null) {
					if (++currConsumerIndex == nConsumers) return null;
				} else {
					availToFetch[currConsumerIndex]--;
					return e;
				}
			} else {
				if (++currConsumerIndex == nConsumers) return null;
			}
		}
	}
	
	/**
	 * See {@link com.coralblocks.coralqueue.queue.Queue#doneFetching(boolean)} for more details.
	 * 
	 * @param lazySet true to notify the producer in a lazy way or false to notify the producer <b>immediately</b>
	 */
	public final void doneFetching(boolean lazySet) {
		for(int i = 0; i < nConsumers; i++) {
			if (needsDoneFetching[i]) consumers[i].doneFetching(lazySet);
		}
	}
	
	/**
	 * See {@link com.coralblocks.coralqueue.queue.Queue#doneFetching()} for more details.
	 */
	public final void doneFetching() {
		for(int i = 0; i < nConsumers; i++) {
			if (needsDoneFetching[i]) consumers[i].doneFetching();
		}
	}
	
	/**
	 * Return the index of this consumer
	 * 
	 * @return the consumer index
	 */
	public final int getIndex() {
		return index;
	}
}
