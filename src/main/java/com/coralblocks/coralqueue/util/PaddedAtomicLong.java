/* 
 * Copyright 2015-2024 (c) CoralBlocks LLC - http://www.coralblocks.com
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

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

abstract class PaddedAtomicLongLhsPadding {
	// 56 bytes before the hot value
	// 7 longs (56 bytes) are enough (instead of 8 longs / 64 bytes) because, 
	// on HotSpot with 64-byte cache lines, an aligned long cannot occupy two cache lines
	long p01, p02, p03, p04, p05, p06, p07;
}

abstract class PaddedAtomicLongValue extends PaddedAtomicLongLhsPadding {
	// On HotSpot with 64-byte cache lines, this aligned long cannot be split across two cache lines
	volatile long value;
}

abstract class PaddedAtomicLongRhsPadding extends PaddedAtomicLongValue {
	// 56 bytes after the hot value
	// 7 longs (56 bytes) are enough (instead of 8 longs / 64 bytes) because, 
	// on HotSpot with 64-byte cache lines, an aligned long cannot occupy two cache lines
	long p08, p09, p10, p11, p12, p13, p14;
}

/**
 * <p>Each CPU core has a small, fast L1 cache. It stores memory in blocks called cache lines,
 * commonly 64 bytes. Writing anything in a line invalidates other cores' copies of the whole line.
 * Therefore the goal is to prevent writes to unrelated values from invalidating the cache line containing
 * our hot sequence.</p>
 *
 * <p>Basically padding on both sides keeps unrelated values out of its cache line, so their writes cannot invalidate it.
 * Padding is needed on both sides because our cached long sequence may be near either end of the line.</p>
 *
 * <p>HotSpot aligns an 8-byte long, so it cannot cross a 64-byte line. In other words, our cached hot long sequence
 * can never be split across two cache lines. At most 56 bytes remain on either side; therefore seven padding longs are enough.
 * In order to keep the sequence value between the two padding blocks, we use separate superclasses to enforce the order:
 * left padding, hot value, right padding.</p>
 */
public class PaddedAtomicLong extends PaddedAtomicLongRhsPadding {

	private static final VarHandle VALUE;

	static {
		try {
			VALUE = MethodHandles.lookup().findVarHandle(PaddedAtomicLongValue.class, "value", long.class);
		} catch (ReflectiveOperationException e) {
			throw new ExceptionInInitializerError(e);
		}
	}
	
	/**
	 * Creates a new <code>PaddedAtomicLong</code> with the given value.
	 * 
	 * @param value the initial value
	 */
	public PaddedAtomicLong(final long value) {
		this.value = value;
	}

	/**
	 * Returns the current value with volatile read semantics.
	 *
	 * @return the current value
	 */
	public final long get() {
		return value;
	}

	/**
	 * Sets the value with volatile write semantics.
	 *
	 * @param value the new value
	 */
	public final void set(long value) {
		this.value = value;
	}

	/**
	 * Sets the value with release semantics.
	 *
	 * @param value the new value
	 */
	public final void lazySet(long value) {
		VALUE.setRelease(this, value);
	}

	/**
	 * This method has no purpose. It is here just to prevent HotSpot optimization and code removal.
	 * 
	 * @return the total of all values inside the object
	 */
	public long getTotal() {
		// Prevent HotSpot optimization and code removal
		return p01 + p02 + p03 + p04 + p05 + p06 + p07
				+ p08 + p09 + p10 + p11 + p12 + p13 + p14 - get();
	}
}
