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
 * <p>A wait strategy that sleeps 1 millisecond by calling <code>Thread.sleep(1)</code>.
 * It can also back off by incrementing its sleep time by 1 millisecond until it reaches a maximum sleep time.
 * Its string type for the factory method {@link WaitStrategy#getWaitStrategy(String)} is "sleep".</p>
 * 
 * <p>NOTE: You can optionally pass -DcoralQueueCountBlocking=true to count the total number of blockings.</p>
 */
public class SleepWaitStrategy implements WaitStrategy {
	
	private final static boolean DEFAULT_BACK_OFF = false;
	private final static long DEFAULT_MAX_SLEEP_TIME = 5;
	
	private final boolean sleepBackOff;
	private final long maxSleepTime;
	
	private int sleepTime = 0;
	
	private final BlockingCounter blockingCounter = new BlockingCounter();
	
	/**
	 * Creates a <code>SleepWaitStrategy</code>.
	 * 
	 * @param sleepBackOff true to support backing off by increasing the sleep time
	 * @param maxSleepTime the max sleep time in milliseconds if sleep backing off is enabled
	 */
	public SleepWaitStrategy(boolean sleepBackOff, long maxSleepTime) {
		this.sleepBackOff = sleepBackOff;
		this.maxSleepTime = maxSleepTime;
		
	}
	
	/**
	 * Creates a <code>SleepWaitStrategy</code>. The default backing off max sleep time is used (5 milliseconds).
	 * 
	 * @param sleepBackOff true to support backing off by increasing the sleep time
	 */
	public SleepWaitStrategy(boolean sleepBackOff) {
		this(sleepBackOff, DEFAULT_MAX_SLEEP_TIME);
	}
	
	/**
	 * Creates a <code>SleepWaitStrategy</code> without backing off.
	 */
	public SleepWaitStrategy() {
		this(DEFAULT_BACK_OFF, DEFAULT_MAX_SLEEP_TIME);
	}

	@Override
    public final void block() {
		
		blockingCounter.increment();
		
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

	@Override
    public final void reset() {
		
		blockingCounter.reset();
		
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