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
 * A wait strategy that uses the LockSupport.parkNanos method with some optional backing-off functionality. If backing-off is turned on, the sleepTime will gradually increase
 * by one nanosecond until the strategy is reset. You can also specify the max park (i.e. sleep) time in nanoseconds (default is 1 millisecond).
 * 
* 
 */
public class ParkWaitStrategy implements WaitStrategy {
	
	private final static boolean DEFAULT_BACK_OFF = false;
	private final static long DEFAULT_MAX_PARK_TIME = 1000000L; // 1 millisecond
	
	private final boolean parkBackOff;
	private final long maxParkTime;
	
	private int parkTime = 0;
	
	private final BlockCount blockCount = new BlockCount();
	
	public ParkWaitStrategy(boolean parkBackOff, long maxParkTime) {
		this.parkBackOff = parkBackOff;
		this.maxParkTime = maxParkTime;
		
	}
	
	public ParkWaitStrategy(boolean parkBackOff) {
		this(parkBackOff, DEFAULT_MAX_PARK_TIME);
	}
	
	public ParkWaitStrategy() {
		this(DEFAULT_BACK_OFF, DEFAULT_MAX_PARK_TIME);
	}

	@Override
    public final void block() {
		
		blockCount.increment();
		
		if (parkBackOff) {
			if (parkTime != maxParkTime) parkTime++;
			LockSupport.parkNanos(parkTime);
		} else {
			LockSupport.parkNanos(1);
		}
    }

	@Override
    public final void reset() {
		
		blockCount.reset();
		
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