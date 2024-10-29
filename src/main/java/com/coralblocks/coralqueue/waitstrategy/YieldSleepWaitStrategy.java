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

public class YieldSleepWaitStrategy implements WaitStrategy {

	private final static int DEFAULT_YIELD_COUNT = 1000;
	private final static boolean DEFAULT_BACK_OFF = false;
	private final static long DEFAULT_MAX_SLEEP_TIME = 5L;

	private final int yieldCount;
	private final boolean sleepBackOff;
	private final long maxSleepTime;

	private int count = 0;
	private int sleepTime = 0;
	
	private final BlockCount blockCount = new BlockCount();

	public YieldSleepWaitStrategy(final int yieldCount, final boolean sleepBackOff, final long maxSleepTime) {
		this.yieldCount = yieldCount;
		this.sleepBackOff = sleepBackOff;
		this.maxSleepTime = maxSleepTime;
	}

	public YieldSleepWaitStrategy(final int yieldCount) {
		this(yieldCount, DEFAULT_BACK_OFF, DEFAULT_MAX_SLEEP_TIME);
	}
	
	public YieldSleepWaitStrategy(final int yieldCount, final boolean sleepBackOff) {
		this(yieldCount, sleepBackOff, DEFAULT_MAX_SLEEP_TIME);
	}
	
	public YieldSleepWaitStrategy(final boolean sleepBackOff, final long maxSleepTime) {
		this(DEFAULT_YIELD_COUNT, sleepBackOff, maxSleepTime);
	}

	public YieldSleepWaitStrategy(final boolean sleepBackOff) {
		this(DEFAULT_YIELD_COUNT, sleepBackOff, DEFAULT_MAX_SLEEP_TIME);
	}

	public YieldSleepWaitStrategy() {
		this(DEFAULT_YIELD_COUNT);
	}

	@Override
	public final void block() {

		blockCount.increment();
		
		if (count < yieldCount) {
			Thread.yield();
			count++;
		} else {
			if (sleepBackOff) {
				
				if (sleepTime != maxSleepTime) sleepTime++;
				
				try {
				
					Thread.sleep(sleepTime);
					
				} catch(InterruptedException e) {
					// NOOP
				}
				
			} else {
				
				try {
					
					Thread.sleep(1);
					
				} catch(InterruptedException e) {
					// NOOP
				}
			}
		}
	}

	@Override
	public final void reset() {
		blockCount.reset();
		count = 0;
		sleepTime = 0;
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
