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
package com.coralblocks.coralqueue.multiplexer;

/**
 * A holder for a multiplexer producer. It contains a reference to the multiplexer and the producer index.
 * 
 * @param <E> The mutable transfer object to be used by this queue
 */
public class Producer<E> {
	
	private final int index;
	private final Multiplexer<E> mux;
	
	Producer(Multiplexer<E> mux, int index) {
		this.index = index;
		this.mux = mux;
	}
	
	/**
	 * Delegates to the multiplexer with the right producer index.
	 * 
	 * @return the next object to dispatch
	 */
	public final E nextToDispatch() {
		return mux.nextToDispatch(index);
	}
	
	/**
	 * Delegates to the multiplexer with the right producer index.
	 * 
	 * @param lazySet true for lazy or false for immediately
	 */
	public final void flush(boolean lazySet) {
		mux.flush(index, lazySet);
	}
	
	/**
	 * Delegates to the multiplexer with the right producer index.
	 */
	public final void flush() {
		mux.flush(index);
	}
	
	/**
	 * Return the zero-based index of this producer
	 * 
	 * @return the zero-based index of this producer
	 */
	public final int getIndex() {
		return index;
	}
}
