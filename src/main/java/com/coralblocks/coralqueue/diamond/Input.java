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
package com.coralblocks.coralqueue.diamond;

import com.coralblocks.coralqueue.demultiplexer.Demultiplexer;

/**
 * The input of the diamond queue, in other words, tasks will be sent to the diamond queue through here.
 * 
 * @param <E> an object inheriting from {@link Task}
 */
public class Input<E extends Task> {
	
	private final Demultiplexer<E> demux;
	
	Input(Demultiplexer<E> demux) {
		this.demux = demux;
	}
	
	/**
	 * Get another task to dispatch
	 * 
	 * @return a task to be dispatched
	 */
	public final E nextToDispatch() {
		return demux.nextToDispatch();
	}
	
	/**
	 * Get another task to dispatch, but dispatch to this specific worker thread so it can be ordered.
	 * If some tasks need to be executed in order, they can be sent to the same worker thread.
	 * 
	 * @param workerThreadIndex the worker thread to send the task
	 * @return the task to be dispatched
	 */
	public final E nextToDispatch(int workerThreadIndex) {
		return demux.nextToDispatch(workerThreadIndex);
	}
	
	/**
	 * <p>Dispatch/Flush all previously obtained objects through the {@link #nextToDispatch()} method to the worker threads.</p>
	 * 
	 * @param lazySet true to flush (i.e. notify the worker threads) in a lazy way or false to flush <b>immediately</b>
	 */
	public final void flush(boolean lazySet) {
		demux.flush(lazySet);
	}
	
	/**
	 * <p>Dispatch <b>immediately</b> all previously obtained objects through the {@link #nextToDispatch()} method to the worker threads.
	 * Note that this is the same as calling <code>flush(false)</code>.</p>
	 */
	public final void flush() {
		demux.flush();
	}
}
