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
 * This wait strategy first busy-spins then parks with backing off if enabled. Also supports a maximum park time (default is 1 millisecond).
 * 
* 
 */
public class SpinParkWaitStrategy implements WaitStrategy {

	private final static int DEFAULT_SPIN_COUNT = 1000000;
	private final static boolean DEFAULT_BACK_OFF = false;
	private final static long DEFAULT_MAX_PARK_TIME = 1000000L;

	private final int spinCount;
	private final boolean parkBackOff;
	private final long maxParkTime;

	private int count = 0;
	private int parkTime = 0;
	
	private final BlockCount blockCount = new BlockCount();

	public SpinParkWaitStrategy(final int spinCount, final boolean parkBackOff, final long maxParkTime) {
		this.spinCount = spinCount;
		this.parkBackOff = parkBackOff;
		this.maxParkTime = maxParkTime;
	}

	public SpinParkWaitStrategy(final boolean parkBackOff) {
		this(DEFAULT_SPIN_COUNT, parkBackOff, DEFAULT_MAX_PARK_TIME);
	}
	
	public SpinParkWaitStrategy(final boolean parkBackOff, final long maxParkTime) {
		this(DEFAULT_SPIN_COUNT, parkBackOff, maxParkTime);
	}

	public SpinParkWaitStrategy(final int spinCount) {
		this(spinCount, DEFAULT_BACK_OFF, DEFAULT_MAX_PARK_TIME);
	}

	public SpinParkWaitStrategy() {
		this(DEFAULT_SPIN_COUNT);
	}

	@Override
	public final void block() {
		
		blockCount.increment();

		if (count < spinCount) {

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
