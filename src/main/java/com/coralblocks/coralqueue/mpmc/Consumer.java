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
package com.coralblocks.coralqueue.mpmc;

/**
 * A consumer holder for the {@link MpMc}. It contains a demultiplexer {@link com.coralblocks.coralqueue.demultiplexer.Consumer} and the consumer index.
 * 
 * Its API is the same as a regular {@link com.coralblocks.coralqueue.queue.Queue}.
 * 
 * @param <E> The data transfer mutable object to be used by this mpmc
 */
public class Consumer<E> {
	
	private final com.coralblocks.coralqueue.demultiplexer.Consumer<E>[] consumers;
	private int currConsumerIndex = 0;
	private final int index;
	private final int nConsumers;
	private final long[] availToPoll;
	private final boolean[] needsDonePolling;
	
	Consumer(com.coralblocks.coralqueue.demultiplexer.Consumer<E>[] consumers, int index) {
		this.consumers = consumers;
		this.index = index;
		this.nConsumers = consumers.length;
		this.availToPoll = new long[consumers.length];
		this.needsDonePolling = new boolean[consumers.length];
	}
	
	/**
	 * See {@link com.coralblocks.coralqueue.queue.Queue#availableToPoll()} for more details.
	 * 
	 * @return the number of objects that can be polled
	 */
	public final long availableToPoll() {
		long total = 0;
		for(int i = 0; i < nConsumers; i++) {
			long x = consumers[i].availableToPoll();
			availToPoll[i] = x;
			total += x;
			needsDonePolling[i] = false;
		}
		currConsumerIndex = 0;
		return total;
	}
	
	/**
	 * See {@link com.coralblocks.coralqueue.queue.Queue#poll()} for more details.
	 * 
	 * @return a data transfer mutable object from the queue
	 */
	public final E poll() {
		while(true) {
			if (availToPoll[currConsumerIndex] > 0) {
				E e = consumers[currConsumerIndex].poll();
				needsDonePolling[currConsumerIndex] = true;
				if (e == null) {
					if (++currConsumerIndex == nConsumers) return null;
				} else {
					availToPoll[currConsumerIndex]--;
					return e;
				}
			} else {
				if (++currConsumerIndex == nConsumers) return null;
			}
		}
	}
	
	/**
	 * See {@link com.coralblocks.coralqueue.queue.Queue#donePolling(boolean)} for more details.
	 * 
	 * @param lazySet true to notify the producer in a lazy way or false to notify the producer <b>immediately</b>
	 */
	public final void donePolling(boolean lazySet) {
		for(int i = 0; i < nConsumers; i++) {
			if (needsDonePolling[i]) consumers[i].donePolling(lazySet);
		}
	}
	
	/**
	 * See {@link com.coralblocks.coralqueue.queue.Queue#donePolling()} for more details.
	 */
	public final void donePolling() {
		for(int i = 0; i < nConsumers; i++) {
			if (needsDonePolling[i]) consumers[i].donePolling();
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
