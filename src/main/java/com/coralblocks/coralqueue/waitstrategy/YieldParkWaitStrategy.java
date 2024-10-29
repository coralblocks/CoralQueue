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

import java.util.concurrent.locks.LockSupport;

/**
 * No busy spinning, just yield and sleep. You can configure how many iteration to yield before sleeping. Supporing backing-off
 * functionality, in other words, if turned on, the sleeping time will increase by one nanosecond gradually until reset is called.
 * It also supports a maximum park time (default is 1 millisecond).
 * 
* 
 */
public class YieldParkWaitStrategy implements WaitStrategy {

	private final static int DEFAULT_YIELD_COUNT = 1000;
	private final static boolean DEFAULT_BACK_OFF = false;
	private final static long DEFAULT_MAX_PARK_TIME = 1000000L;

	private final int yieldCount;
	private final boolean parkBackOff;
	private final long maxParkTime;

	private int count = 0;
	private int parkTime = 0;
	
	private final BlockCount blockCount = new BlockCount();

	public YieldParkWaitStrategy(final int yieldCount, final boolean parkBackOff, final long maxParkTime) {
		this.yieldCount = yieldCount;
		this.parkBackOff = parkBackOff;
		this.maxParkTime = maxParkTime;
	}

	public YieldParkWaitStrategy(final int yieldCount) {
		this(yieldCount, DEFAULT_BACK_OFF, DEFAULT_MAX_PARK_TIME);
	}
	
	public YieldParkWaitStrategy(final int yieldCount, final boolean parkBackOff) {
		this(yieldCount, parkBackOff, DEFAULT_MAX_PARK_TIME);
	}
	
	public YieldParkWaitStrategy(final boolean parkBackOff, final long maxParkTime) {
		this(DEFAULT_YIELD_COUNT, parkBackOff, maxParkTime);
	}

	public YieldParkWaitStrategy(final boolean parkBackOff) {
		this(DEFAULT_YIELD_COUNT, parkBackOff, DEFAULT_MAX_PARK_TIME);
	}

	public YieldParkWaitStrategy() {
		this(DEFAULT_YIELD_COUNT);
	}

	@Override
	public final void block() {

		blockCount.increment();
		
		if (count < yieldCount) {
			Thread.yield();
			count++;
		} else {
			if (parkBackOff) {
				
				if (parkTime != maxParkTime) parkTime++;
				
				LockSupport.parkNanos(parkTime);
			} else {
				LockSupport.parkNanos(1);
			}
		}
	}

	@Override
	public final void reset() {
		blockCount.reset();
		count = 0;
		parkTime = 0;
	}
	
	@Override
	public final long getTotalBlockCount() {
		return blockCount.getTotalBlockCount();
	}
	
	@Override
	public final void resetTotalBlockCount() {
		
		blockCount.resetTotalBlockCount();
	}

}
