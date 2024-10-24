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

/**
 * A mutable long class to facilitate re-using and pooling without generating garbage through auto-boxing.
 */
public class MutableLong {
	
	private static long DEFAULT_VALUE = 0;
	
	private long value;
	
	private boolean isNull;
	
	/**
	 * Constructs a new <code>MutableLong</code> with the given long value.
	 *
	 * @param value the initial long value
	 */
	public MutableLong(long value) {
		set(value);
	}
	
	/**
	 * Constructs a new <code>MutableLong</code> with the zero value.
	 */
	public MutableLong() {
		this(DEFAULT_VALUE);
	}
	
	/**
	 * Changes the value of this <code>MutableLong</code>.
	 *
	 * @param value the new long value
	 */
	public void set(long value) {
		this.value = value;
		this.isNull = false;
	}
	
	/**
	 * Checks if it is null.
	 *
	 * @return true, if it is null
	 */
	public boolean isNull() {
		return isNull;
	}
	
	/**
	 * Makes it equal to null.
	 */
	public void setNull() {
		isNull = true;
	}
	
	/**
	 * Gets the current long value.
	 *
	 * @return the long value
	 * @throws NullPointerException if the value is null
	 */
	public long get() {
		if (isNull) throw new NullPointerException();
		return value;
	}
	
	@Override
	public String toString() {
		return isNull ? "NULL" : String.valueOf(value);
	}
}
