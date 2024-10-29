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
package com.coralblocks.coralqueue.waitstrategy;

final class BlockCount {
	
	private volatile long totalBlockCount = 0;
	private long currBlockCount = 0;
	private final boolean isActive;
	private final int addValue;
	
	public BlockCount() {
		this(false);
	}
	
	public BlockCount(boolean isActive) {
		this.isActive = isActive;
		this.addValue = isActive ? 1 : 0;
	}
	
	final void increment() {
		currBlockCount += addValue;
	}
	
	final void reset() {
		if (currBlockCount > 0) {
			totalBlockCount += currBlockCount; // flush to volatile variable (i.e. flush to memory)
			currBlockCount = 0;
		}
	}
	
	final long getTotalBlockCount() {
		return isActive ? totalBlockCount : -1;
	}
	
	final void resetTotalBlockCount() {
		if (isActive) {
			totalBlockCount = 0;
		}
	}
}
