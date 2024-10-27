/*
* Copyright (c) CoralBlocks LLC (c) 2017
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
