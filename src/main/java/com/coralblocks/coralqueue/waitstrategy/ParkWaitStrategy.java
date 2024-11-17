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
 * <p>A wait strategy that parks 1 nanosecond by calling <code>LockSupport.parkNanos(1L)</code>.
 * It can also back off by incrementing its park time by 1 microsecond until it reaches a maximum park time.
 * Its string type for the factory method {@link WaitStrategy#getWaitStrategy(String)} is "park".</p>
 * 
 * <p>NOTE: You can optionally pass -DcoralQueueCountBlocking=true to count the total number of blockings.</p>
 */
public class ParkWaitStrategy implements WaitStrategy {
	
	private final static boolean DEFAULT_BACK_OFF = false;
	private final static long DEFAULT_MAX_PARK_TIME = 1000000L; // 1 millisecond
	
	private final boolean parkBackOff;
	private final long maxParkTime;
	
	private int parkTime = 0;
	
	private final BlockingCounter blockingCounter = new BlockingCounter();
	
	/**
	 * Creates a <code>ParkWaitStrategy</code>.
	 * 
	 * @param parkBackOff true to support backing off by increasing the park time
	 * @param maxParkTime the max park time in nanoseconds if park backing off is enabled
	 */
	public ParkWaitStrategy(boolean parkBackOff, long maxParkTime) {
		this.parkBackOff = parkBackOff;
		this.maxParkTime = maxParkTime;
		
	}
	
	/**
	 * Creates a <code>ParkWaitStrategy</code>. The default backing off max park time is used (1_000_000 nanoseconds).
	 * 
	 * @param parkBackOff true to support backing off by increasing the park time
	 */
	public ParkWaitStrategy(boolean parkBackOff) {
		this(parkBackOff, DEFAULT_MAX_PARK_TIME);
	}
	
	/**
	 * Creates a <code>ParkWaitStrategy</code> without backing off.
	 */
	public ParkWaitStrategy() {
		this(DEFAULT_BACK_OFF, DEFAULT_MAX_PARK_TIME);
	}

	@Override
    public final void block() {
		
		blockingCounter.increment();
		
		if (parkBackOff) {
			if (parkTime != maxParkTime) parkTime++;
			LockSupport.parkNanos(parkTime);
		} else {
			LockSupport.parkNanos(1L);
		}
    }

	@Override
    public final void reset() {
		
		blockingCounter.reset();
		
		parkTime = 0;
    }
	
	@Override
	public final long getTotalBlockCount() {
		
		return blockingCounter.getTotalBlockCount();
	}
	
	@Override
	public final void resetTotalBlockCount() {
		
		blockingCounter.resetTotalBlockCount();
	}
}