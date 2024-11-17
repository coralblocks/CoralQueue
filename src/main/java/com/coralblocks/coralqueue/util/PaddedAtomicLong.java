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
package com.coralblocks.coralqueue.util;

import java.util.concurrent.atomic.AtomicLong;

/**
 * <p>A padded <code>AtomicLong</code> that occupies a whole CPU cache line (64Kb) so that
 * two different sequences (from producer and from consumer) do not mess with each other inside the CPU cache.</p>
 * 
 * <p>NOTE: The <code>AtomicLong</code> class stores its long value internally as a <i>volatile</i> variable.</p>
 */
public class PaddedAtomicLong extends AtomicLong {
	
	private static final long VALUE_LONG = 19760120L;
	
	public volatile long value1 = VALUE_LONG + 0;
	public volatile long value2 = VALUE_LONG + 1;
	public volatile long value3 = VALUE_LONG + 2;
	public volatile long value4 = VALUE_LONG + 3;
	public volatile long value5 = VALUE_LONG + 4;
	public volatile long value6 = VALUE_LONG + 5;
	
	/*
	 * Why do we only need 6 longs and not 7 ???
	 * 
	 * That's because each object has at least a header with 8 bytes.
	 * 
	 * So to complete a 64bytes cache line, we have:
	 * 
	 *  - the header (8 bytes)
	 *  - our sequence (8 bytes)
	 *  - 6 longs (6 x 8 = 48 bytes)
	 *  
	 *  TOTAL: 64 bytes
	 */
	
	/**
	 * Creates a new <code>PaddedAtomicLong</code> by calling <code>super(value)</code>.
	 * 
	 * @param value the value of the <code>AtomicLong</code> superclass.
	 */
	public PaddedAtomicLong(final long value) {
		super(value);
	}

	/**
	 * This method has no purpose. It is here just to prevent HotSpot optimization and code removal.
	 * 
	 * @return the total of all values inside the object
	 */
	public long getTotal() {
		// try to prevent hotspot optimization...
		return value1 + value2 + value3 + value4 + value5 + value6;
	}
}
