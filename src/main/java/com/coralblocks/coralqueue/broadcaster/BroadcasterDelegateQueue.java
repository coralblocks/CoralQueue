/*
* Copyright (c) CoralBlocks LLC (c) 2017
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
