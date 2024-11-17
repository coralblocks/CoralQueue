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

/**
 * <p>A wait strategy that busy spins for some cycles, then yields for some cycles, then sleeps 1 millisecond by calling <code>Thread.sleep(1)</code>.
 * It can also back off by incrementing its sleep time by 1 millisecond until it reaches a maximum sleep time.
 * Its string type for the factory method {@link WaitStrategy#getWaitStrategy(String)} is "spinYieldSleep".</p>
 * 
 * <p>NOTE: You can optionally pass -DcoralQueueCountBlocking=true to count the total number of blockings.</p>
 */
public class SpinYieldSleepWaitStrategy implements WaitStrategy {

	private final static int DEFAULT_SPIN_COUNT = 1000000;
	private final static int DEFAULT_YIELD_COUNT = 1000;
	private final static boolean DEFAULT_BACK_OFF = false;
	private final static long DEFAULT_MAX_SLEEP_TIME = 5L;

	private final int spinCount;
	private final int yieldCount;
	private final boolean sleepBackOff;
	private final long maxSleepTime;

	private int count = 0;
	private int sleepTime = 0;
	
	private final BlockingCounter blockingCounter = new BlockingCounter();

	/**
	 * Creates a <code>SpinYieldSleepWaitStrategy</code>.
	 * 
	 * @param spinCount the number of cycles to busy spin before starting to yield
	 * @param yieldCount the number of cycles to yield before starting to sleep
	 * @param sleepBackOff true to support backing off by increasing the sleep time
	 * @param maxSleepTime the max sleep time in milliseconds if sleep backing off is enabled
	 */
	public SpinYieldSleepWaitStrategy(final int spinCount, final int yieldCount, final boolean sleepBackOff, final long maxSleepTime) {
		this.spinCount = spinCount;
		this.yieldCount = yieldCount + spinCount;
		this.sleepBackOff = sleepBackOff;
		this.maxSleepTime = maxSleepTime;
	}
	
	/**
	 * Creates a <code>SpinYieldSleepWaitStrategy</code> with the default max sleep time of 5 milliseconds (if backing off is enabled).
	 * 
	 * @param spinCount the number of cycles to busy spin before starting to yield
	 * @param yieldCount the number of cycles to yield before starting to sleep
	 * @param sleepBackOff true to support backing off by increasing the sleep time
	 */
	public SpinYieldSleepWaitStrategy(final int spinCount, final int yieldCount, final boolean sleepBackOff) {
		this(spinCount, yieldCount, sleepBackOff, DEFAULT_MAX_SLEEP_TIME);
	}
	
	/**
	 * Creates a <code>SpinYieldSleepWaitStrategy</code> with the default spin count of 1_000_000 and the default yield count of 1_000.
	 * 
	 * @param sleepBackOff true to support backing off by increasing the sleep time
	 * @param maxSleepTime the max sleep time in milliseconds if sleep backing off is enabled
	 */
	public SpinYieldSleepWaitStrategy(final boolean sleepBackOff, final long maxSleepTime) {
		this(DEFAULT_SPIN_COUNT, DEFAULT_YIELD_COUNT, sleepBackOff, maxSleepTime);
	}

	/**
	 * Creates a <code>SpinYieldSleepWaitStrategy</code> with the default spin count of 1_000_000, the default yield count of 1_000 and the default max sleep time of 5 milliseconds.
	 * 
	 * @param sleepBackOff true to support backing off by increasing the sleep time
	 */
	public SpinYieldSleepWaitStrategy(final boolean sleepBackOff) {
		this(DEFAULT_SPIN_COUNT, DEFAULT_YIELD_COUNT, sleepBackOff, DEFAULT_MAX_SLEEP_TIME);
	}

	/**
	 * Creates a <code>SpinYieldSleepWaitStrategy</code> without any backing off from sleep.
	 * 
	 * @param spinCount the number of cycles to busy spin before starting to yield
	 * @param yieldCount the number of cycles to yield before starting to sleep
	 */
	public SpinYieldSleepWaitStrategy(final int spinCount, final int yieldCount) {
		this(spinCount, yieldCount, DEFAULT_BACK_OFF, DEFAULT_MAX_SLEEP_TIME);
	}

	/**
	 * Creates a <code>SpinYieldSleepWaitStrategy</code> with the default spin count of 1_000_000, the default yield count of 1_000 and without any backing off from sleep.
	 */
	public SpinYieldSleepWaitStrategy() {
		this(DEFAULT_SPIN_COUNT, DEFAULT_YIELD_COUNT, DEFAULT_BACK_OFF, DEFAULT_MAX_SLEEP_TIME);
	}

	@Override
	public final void block() {
		
		blockingCounter.increment();

		if (count < spinCount) {

			count++;

		} else if (count < yieldCount) {

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
					
					Thread.sleep(1L);
					
				} catch(InterruptedException e) {
					// NOOP
				}
			}
		}
	}

	@Override
	public final void reset() {
		blockingCounter.reset();
		count = 0;
		sleepTime = 0;
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
