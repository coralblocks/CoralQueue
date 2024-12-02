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

import com.coralblocks.coralqueue.multiplexer.Multiplexer;

/**
 * The output of the diamond queue, in other words, tasks will be fetched from the diamond queue through here.
 * 
 * @param <E> an object inheriting from {@link Task}
 */
public class Output<E extends Task> {
	
	private final Multiplexer<E> mux;
	
	Output(Multiplexer<E> mux) {
		this.mux = mux;
	}
	
	/**
	 * <p>Return the number of objects that can be safely fetched from the diamond queue.</p>
	 * 
	 * <p>If the diamond queue is empty, this method returns 0.</p>
	 * 
	 * @return the number of objects that can be fetched
	 */
	public final long availableToFetch() {
		return mux.availableToFetch();
	}
	
	/**
	 * <p>Fetch an object from the diamond queue. You can only call this method after calling {@link #availableToFetch()} so you
	 * know for sure what is the maximum number of times you can call this method.</p>
	 * 
	 * <p><b>NOTE:</b> You must <b>never</b> keep your own reference to the mutable object returned by this method.
	 * Read what you need to read from the object and release its reference.
	 * The object returned should be treated as a <i>data transfer mutable object</i> therefore you should read what you need from it and let it go.</p>
	 * 
	 * @return a data transfer mutable object from the diamond queue
	 */
	public final E fetch() {
		return mux.fetch();
	}
	
	/**
	 * <p>Must be called to indicate that all fetching has been concluded, in other words, 
	 * you fetch what you can/want to fetch and call this method to signal the worker threads that you are done.</p>
	 * 
	 * <p><b>NOTE:</b> This method only needs to be called if there was something available to be fetched, in other words,
	 * if {@link #availableToFetch()} returned zero then this method does not need to be called.</p>
	 * 
	 * @param lazySet true to notify the worker threads in a lazy way or false to notify the worker threads <b>immediately</b>
	 */
	public final void doneFetching(boolean lazySet) {
		mux.doneFetching(lazySet);
	}
	
	/**
	 * <p>That's the same as calling <code>doneFetching(false)</code>, in other words, the worker threads will 
	 *  be notified <b>immediately</b> that fetching is done.</p>
	 */
	public final void doneFetching() {
		mux.doneFetching();
	}
}
