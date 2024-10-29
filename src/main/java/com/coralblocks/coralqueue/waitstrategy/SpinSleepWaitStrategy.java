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
 * <p>A wait strategy that busy spins for some cycles, then sleeps 1 millisecond by calling <code>Thread.sleep(1)</code>.
 * It can also back off by incrementing its sleep time by 1 millisecond until it reaches a maximum sleep time.
 * Its string type for the factory method {@link WaitStrategy#getWaitStrategy(String)} is "spinSleep".</p>
 * 
 * <p>NOTE: You can optionally pass -DcoralQueueBlockCount=true to count the total number of blocks.</p>
 */
public class SpinSleepWaitStrategy implements WaitStrategy {

	private final static int DEFAULT_SPIN_COUNT = 1000000;
	private final static boolean DEFAULT_BACK_OFF = false;
	private final static long DEFAULT_MAX_SLEEP_TIME = 5;

	private final int spinCount;
	private final boolean sleepBackOff;
	private final long maxSleepTime;

	private int count = 0;
	private int sleepTime = 0;
	
	private final BlockCount blockCount = new BlockCount();

	/**
	 * Creates a <code>SpinSleepWaitStrategy</code>.
	 * 
	 * @param spinCount the number of cycles to busy spin before starting to sleep
	 * @param sleepBackOff true to support backing off by increasing the sleep time
	 * @param maxSleepTime the max sleep time in milliseconds if sleep backing off is enabled
	 */
	public SpinSleepWaitStrategy(final int spinCount, final boolean sleepBackOff, final long maxSleepTime) {
		this.spinCount = spinCount;
		this.sleepBackOff = sleepBackOff;
		this.maxSleepTime = maxSleepTime;
	}

	/**
	 * Creates a <code>SpinSleepWaitStrategy</code> with a default spin count of 1_000_000 and a default max sleep time (for backing off) of 5 milliseconds.
	 * 
	 * @param sleepBackOff true to use backing off
	 */
	public SpinSleepWaitStrategy(final boolean sleepBackOff) {
		this(DEFAULT_SPIN_COUNT, sleepBackOff, DEFAULT_MAX_SLEEP_TIME);
	}
	
	/**
	 * Creates a <code>SpinSleepWaitStrategy</code> with a default spin count of 1_000_000.
	 * 
	 * @param sleepBackOff true to support backing off
	 * @param maxSleepTime the max sleep time in milliseconds if sleep backing off is enabled
	 */
	public SpinSleepWaitStrategy(final boolean sleepBackOff, final long maxSleepTime) {
		this(DEFAULT_SPIN_COUNT, sleepBackOff, maxSleepTime);
	}

	/**
	 * Creates a <code>SpinSleepWaitStrategy</code> without backing off (will keep sleeping 1 millisecond after the spin count).
	 * 
	 * @param spinCount the number of cycles to busy spin before starting to sleep
	 */
	public SpinSleepWaitStrategy(final int spinCount) {
		this(spinCount, DEFAULT_BACK_OFF, DEFAULT_MAX_SLEEP_TIME);
	}

	/**
	 * Creates a <code>SpinSleepWaitStrategy</code> with a default spin count of 1_000_000 and without backing off (will keep sleeping 1 millisecond after the spin count).
	 */
	public SpinSleepWaitStrategy() {
		this(DEFAULT_SPIN_COUNT);
	}

	@Override
	public final void block() {
		
		blockCount.increment();

		if (count < spinCount) {

			count++;

		} else {
			
			if (sleepBackOff) {
				
				if (sleepTime != maxSleepTime) sleepTime++;
				
				try {
					Thread.sleep(sleepTime);
				} catch(InterruptedException e) {
					throw new RuntimeException(e);
				}
				
			} else {
				
				try {
					Thread.sleep(1);
				} catch(InterruptedException e) {
					throw new RuntimeException(e);
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
