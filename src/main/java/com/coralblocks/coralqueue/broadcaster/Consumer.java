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

public class Consumer<E> {
	
	private final int index;
	private final Broadcaster<E> broadcaster;
	
	Consumer(Broadcaster<E> broadcaster, int index) {
		this.index = index;
		this.broadcaster = broadcaster;
	}
	
	public final int getIndex() {
		return index;
	}
	
	public final long availableToPoll() {
		return broadcaster.availableToPoll(index);
	}
	
	public final E poll() {
		return broadcaster.poll(index);
	}
	
	public final void donePolling(boolean lazySet) {
		broadcaster.donePolling(index, lazySet);
	}
	
	public final void donePolling() {
		broadcaster.donePolling(index);
	}
	
	public final void disable() {
		broadcaster.disableConsumer(index);
	}
}
