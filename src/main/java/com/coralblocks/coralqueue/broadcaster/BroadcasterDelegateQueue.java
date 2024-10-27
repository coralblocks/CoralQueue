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

import com.coralblocks.coralqueue.queue.Queue;

public class BroadcasterDelegateQueue<E> implements Queue<E> {
	
	private final Broadcaster<E> broadcaster;
	private final int index;
	
	public BroadcasterDelegateQueue(Broadcaster<E> broadcaster, int index) {
		this.broadcaster = broadcaster;
		this.index = index;
	}
	
	@Override
	public final void clear() {
		broadcaster.clear();
	}

	@Override
    public E nextToDispatch() {
	    return broadcaster.nextToDispatch();
    }
	
	@Override
	public final E nextToDispatch(E swap) {
		throw new UnsupportedOperationException();
	}

	@Override
    public void flush(boolean lazySet) {
		broadcaster.flush(lazySet);
    }

	@Override
    public void flush() {
		broadcaster.flush();
    }

	@Override
    public long availableToPoll() {
	    return broadcaster.availableToPoll(index);
    }

	@Override
    public E poll() {
		return broadcaster.poll(index);
    }
	
	@Override
	public final void replace(E newVal) {
		throw new UnsupportedOperationException();
	}

	@Override
    public void donePolling(boolean lazySet) {
		broadcaster.donePolling(index, lazySet);
    }

	@Override
    public void donePolling() {
		broadcaster.donePolling(index);
    }

	@Override
    public void rollBack() {
		broadcaster.rollBack(index);
    }

	@Override
    public void rollBack(long items) {
		broadcaster.rollBack(index, items);
    }

	@Override
    public E peek() {
	    return broadcaster.peek(index);
    }
}
