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

/**
 * A delegate so that a {@link Broadcaster} can be used as a {@link Queue}.
 *
 * @param <E> The mutable transfer object to be used by this broadcaster
 */
public class BroadcasterDelegateQueue<E> implements Queue<E> {
	
	private final Broadcaster<E> broadcaster;
	private final int consumerIndex;
	
	/**
	 * Creates a <code>BroadcasterDelegateQueue</code>.
	 * 
	 * @param broadcaster the broadcaster to delegate to
	 * @param consumerIndex the consumerIndex to use
	 */
	public BroadcasterDelegateQueue(Broadcaster<E> broadcaster, int consumerIndex) {
		this.broadcaster = broadcaster;
		this.consumerIndex = consumerIndex;
	}
	
	/**
	 * Return the consumer index being used by this delegate
	 * 
	 * @return the consumer index
	 */
	public final int getConsumerIndex() {
		return consumerIndex;
	}
	
	/**
	 * Return the broadcaster used as the deledage
	 * 
	 * @return the broadcaster used as the delegate
	 */
	public final Broadcaster<E> getDelegate() {
		return broadcaster;
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
	    return broadcaster.availableToPoll(consumerIndex);
    }

	@Override
    public E poll() {
		return broadcaster.poll(consumerIndex);
    }
	
	@Override
	public final void replace(E newVal) {
		throw new UnsupportedOperationException();
	}

	@Override
    public void donePolling(boolean lazySet) {
		broadcaster.donePolling(consumerIndex, lazySet);
    }

	@Override
    public void donePolling() {
		broadcaster.donePolling(consumerIndex);
    }

	@Override
    public void rollBack() {
		broadcaster.rollBack(consumerIndex);
    }

	@Override
    public void rollBack(long items) {
		broadcaster.rollBack(consumerIndex, items);
    }

	@Override
    public E peek() {
	    return broadcaster.peek(consumerIndex);
    }
}
