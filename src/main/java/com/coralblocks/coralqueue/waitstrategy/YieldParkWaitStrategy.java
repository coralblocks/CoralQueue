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
 * <p>A wait strategy that yields for some cycles, then parks 1 nanosecond by calling <code>LockSupport.parkNanos(1L)</code>.
 * It can also back off by incrementing its park time by 1 nanosecond until it reaches a maximum park time.
 * Its string type for the factory method {@link WaitStrategy#getWaitStrategy(String)} is "yieldPark".</p>
 * 
 * <p>NOTE: You can optionally pass -DcoralQueueCountBlocking=true to count the total number of blockings.</p>
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
	
	private final BlockingCounter blockingCounter = new BlockingCounter();

	/**
	 * Creates a <code>YieldParkWaitStrategy</code>.
	 * 
	 * @param yieldCount the number of cycles to yield before starting to park
	 * @param parkBackOff true to support backing off by increasing the park time
	 * @param maxParkTime the max park time in nanoseconds if park backing off is enabled
	 */
	public YieldParkWaitStrategy(final int yieldCount, final boolean parkBackOff, final long maxParkTime) {
		this.yieldCount = yieldCount;
		this.parkBackOff = parkBackOff;
		this.maxParkTime = maxParkTime;
	}

	/**
	 * Creates a <code>YieldParkWaitStrategy</code> without any backing off from park.
	 * 
	 * @param yieldCount the number of cycles to yield before starting to park
	 */
	public YieldParkWaitStrategy(final int yieldCount) {
		this(yieldCount, DEFAULT_BACK_OFF, DEFAULT_MAX_PARK_TIME);
	}
	
	/**
	 * Creates a <code>YieldParkWaitStrategy</code> with the default max park time of 1_000_000 nanoseconds (if backing off is enabled).
	 * 
	 * @param yieldCount the number of cycles to yield before starting to park
	 * @param parkBackOff true to support backing off by increasing the park time
	 */
	public YieldParkWaitStrategy(final int yieldCount, final boolean parkBackOff) {
		this(yieldCount, parkBackOff, DEFAULT_MAX_PARK_TIME);
	}
	
	/**
	 * Creates a <code>YieldParkWaitStrategy</code> with the default yield count of 1_000.
	 * 
	 * @param parkBackOff true to support backing off by increasing the park time
	 * @param maxParkTime the max park time in nanoseconds if park backing off is enabled
	 */
	public YieldParkWaitStrategy(final boolean parkBackOff, final long maxParkTime) {
		this(DEFAULT_YIELD_COUNT, parkBackOff, maxParkTime);
	}

	/**
	 * Creates a <code>YieldParkWaitStrategy</code> with the default yield count of 1_000 and the default max park time of 1_000_000 nanoseconds.
	 * 
	 * @param parkBackOff true to support backing off by increasing the park time
	 */
	public YieldParkWaitStrategy(final boolean parkBackOff) {
		this(DEFAULT_YIELD_COUNT, parkBackOff, DEFAULT_MAX_PARK_TIME);
	}

	/**
	 * Creates a <code>YieldParkWaitStrategy</code> with the default yield count of 1_000 and without any backing off from park.
	 */
	public YieldParkWaitStrategy() {
		this(DEFAULT_YIELD_COUNT);
	}

	@Override
	public final void block() {

		blockingCounter.increment();
		
		if (count < yieldCount) {
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
		blockingCounter.reset();
		count = 0;
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
