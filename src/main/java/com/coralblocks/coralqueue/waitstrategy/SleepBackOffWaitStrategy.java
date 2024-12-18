/* 
 * Copyright 2015-2024 (c) CoralBlocks LLC - http://www.coralblocks.com
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
 * A wait strategy that sleeps with backing off. It has a start sleep time in milliseconds, a maximum sleep time in milliseconds and a step value in milliseconds.
 * Basically with each <code>block()</code> it increases the sleep time value, stepping with the step value, until it reaches the maximum sleep time value.
 * Once it reaches the maximum sleep time value it remains sleeping with the maximum value.
 * When <code>reset()</code> is called it then starts over from the start sleep time value.
 */
public class SleepBackOffWaitStrategy extends AbstractWaitStrategy {
	
	public static final long DEFAULT_START_SLEEP_TIME_IN_MILLIS = 1;
	public static final long DEFAULT_MAX_SLEEP_TIME_IN_MILLIS = 1_000;
	public static final int DEFAULT_STEP_IN_MILLIS = 10;
	
	private final long startSleepTimeInMillis;
	private final long maxSleepTimeInMillis;
	private final int stepInMillis;
	
	private long currSleepTimeInMillis;

	public SleepBackOffWaitStrategy(long maxBlockCount, long startSleepTimeInMillis, long maxSleepTimeInMillis, int stepInMillis) {
		super(maxBlockCount);
		this.startSleepTimeInMillis = startSleepTimeInMillis;
		this.maxSleepTimeInMillis = maxSleepTimeInMillis;
		this.stepInMillis = stepInMillis;
		this.currSleepTimeInMillis = startSleepTimeInMillis;
	}
	
	public SleepBackOffWaitStrategy(long maxBlockCount) {
		this(maxBlockCount, DEFAULT_START_SLEEP_TIME_IN_MILLIS, DEFAULT_MAX_SLEEP_TIME_IN_MILLIS, DEFAULT_STEP_IN_MILLIS);
	}
	
	public SleepBackOffWaitStrategy() {
		this(DEFAULT_MAX_BLOCK_COUNT, DEFAULT_START_SLEEP_TIME_IN_MILLIS, DEFAULT_MAX_SLEEP_TIME_IN_MILLIS, DEFAULT_STEP_IN_MILLIS);
	}
	
	public SleepBackOffWaitStrategy(long startSleepTimeInMillis, long maxSleepTimeInMillis, int stepInMillis) {
		this(DEFAULT_MAX_BLOCK_COUNT, startSleepTimeInMillis, maxSleepTimeInMillis, stepInMillis);
	}
	
	@Override
	protected final void blockOperation() {
		
		try {
			Thread.sleep(currSleepTimeInMillis);
		} catch(InterruptedException e) {
			throw new RuntimeException(e);
		}
		
		if (currSleepTimeInMillis == 1) {
			currSleepTimeInMillis = currSleepTimeInMillis + stepInMillis - 1;
		} else {
			currSleepTimeInMillis += stepInMillis;
		}
		
		currSleepTimeInMillis = Math.min(maxSleepTimeInMillis, currSleepTimeInMillis);
	}
	
	@Override
	protected final void resetOperation() {
		currSleepTimeInMillis = startSleepTimeInMillis;
	}
}
