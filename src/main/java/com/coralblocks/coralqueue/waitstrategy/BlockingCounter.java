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

final class BlockingCounter {
	
	private volatile long totalBlockCount = 0;
	private long currBlockCount = 0;
	private final boolean isActive;
	
	BlockingCounter() {
		String s = System.getProperty("coralQueueCountBlocking");
		this.isActive = s != null && s.equalsIgnoreCase("true");
	}
	
	final void increment() {
		if (!isActive) return;
		currBlockCount++;
	}
	
	final void reset() {
		if (!isActive) return;
		if (currBlockCount > 0) {
			totalBlockCount += currBlockCount; // flush to volatile variable (i.e. flush to memory)
			currBlockCount = 0;
		}
	}
	
	final long getTotalBlockCount() {
		if (!isActive) return -1;
		return totalBlockCount;
	}
	
	final void resetTotalBlockCount() {
		if (!isActive) return; // avoid hitting the volatile variable unless you really have to
		totalBlockCount = 0;
	}
}
