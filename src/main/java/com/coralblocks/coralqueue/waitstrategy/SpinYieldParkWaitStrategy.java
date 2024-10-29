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
 * This wait strategy first busy spins, then yields, then sleep. You can configure each component by passing a spinCount and yieldCount.
 * It optionally supports backing-off for the LockSupport.parkNanos method, increasing the sleep time by one nanosecond until the reset
 * method is called. It also supports a maximum sleep time (default is 1 millisecond).
 * 
* 
 */
public class SpinYieldParkWaitStrategy implements WaitStrategy {

	private final static int DEFAULT_SPIN_COUNT = 1000000;
	private final static int DEFAULT_YIELD_COUNT = 1000;
	private final static boolean DEFAULT_BACK_OFF = false;
	private final static long DEFAULT_MAX_PARK_TIME = 1000000L;

	private final int spinCount;
	private final int yieldCount;
	private final boolean parkBackOff;
	private final long maxParkTime;

	private int count = 0;
	private int parkTime = 0;
	
	private final BlockCount blockCount = new BlockCount();

	public SpinYieldParkWaitStrategy(final int spinCount, final int yieldCount, final boolean parkBackOff, final long maxParkTime) {
		this.spinCount = spinCount;
		this.yieldCount = yieldCount + spinCount;
		this.parkBackOff = parkBackOff;
		this.maxParkTime = maxParkTime;
	}
	
	public SpinYieldParkWaitStrategy(final int spinCount, final int yieldCount, final boolean parkBackOff) {
		this(spinCount, yieldCount, parkBackOff, DEFAULT_MAX_PARK_TIME);
	}
	
	public SpinYieldParkWaitStrategy(final boolean parkBackOff, final long maxParkTime) {
		this(DEFAULT_SPIN_COUNT, DEFAULT_YIELD_COUNT, parkBackOff, maxParkTime);
	}

	public SpinYieldParkWaitStrategy(final boolean parkBackOff) {
		this(DEFAULT_SPIN_COUNT, DEFAULT_YIELD_COUNT, parkBackOff, DEFAULT_MAX_PARK_TIME);
	}

	public SpinYieldParkWaitStrategy(final int spinCount, final int yieldCount) {
		this(spinCount, yieldCount, DEFAULT_BACK_OFF, DEFAULT_MAX_PARK_TIME);
	}

	public SpinYieldParkWaitStrategy() {
		this(DEFAULT_SPIN_COUNT, DEFAULT_YIELD_COUNT, DEFAULT_BACK_OFF, DEFAULT_MAX_PARK_TIME);
	}

	@Override
	public final void block() {
		
		blockCount.increment();

		if (count < spinCount) {

			count++;

		} else if (count < yieldCount) {

			Thread.yield();

			count++;

		} else {
			if (parkBackOff) {
				
				if (parkTime != maxParkTime) parkTime++;
				
				LockSupport.parkNanos(parkTime);
			} else {
				LockSupport.parkNanos(1L);
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
