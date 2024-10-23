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
	
	public final E nextToDispatch() {
		return mux.nextToDispatch(index);
	}
	
	public final void flush(boolean lazySet) {
		mux.flush(index, lazySet);
	}
	
	public final void flush() {
		mux.flush(index);
	}
	
	public final int getIndex() {
		return index;
	}
}
