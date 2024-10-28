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

import com.coralblocks.coralqueue.broadcaster.Broadcaster;

/**
 * A producer holder for the {@link MpMcBroadcaster}. It contains a {@link Broadcaster} and the producer index. 
 *
 * @param <E> The mutable transfer object to be used by this mpmc broadcaster
 */
public class Producer<E> {
	
	private final Broadcaster<E> broadcaster;
	private final int index;
	
	Producer(Broadcaster<E> broadcaster, int index) {
		this.index = index;
		this.broadcaster = broadcaster;
	}

	/**
	 * See {@link com.coralblocks.coralqueue.broadcaster.Broadcaster#nextToDispatch()} for more details.
	 * 
	 * @return the next mutable object that can be used by the producer or null if the broadcaster is full
	 */
	public final E nextToDispatch() {
		return broadcaster.nextToDispatch();
	}
	
	/**
	 * See {@link com.coralblocks.coralqueue.broadcaster.Broadcaster#flush(boolean)} for more details.
	 * 
	 * @param lazySet true to flush (i.e. notify the consumer) in a lazy way or false to flush <b>immediately</b>
	 */
	public final void flush(boolean lazySet) {
		broadcaster.flush(lazySet);
	}
	
	/**
	 * See {@link com.coralblocks.coralqueue.broadcaster.Broadcaster#flush()} for more details.
	 */
	public final void flush() {
		broadcaster.flush();
	}
	
	/**
	 * Return the index of this producer
	 * 
	 * @return the producer index
	 */
	public final int getIndex() {
		return index;
	}
}
