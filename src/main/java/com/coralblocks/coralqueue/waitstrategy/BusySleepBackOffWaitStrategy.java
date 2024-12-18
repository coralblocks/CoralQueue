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
 * A wait strategy that busy sleeps with backing off. It has a start sleep time in nanoseconds, a maximum sleep time in nanoseconds and a step value in nanoseconds.
 * Basically with each <code>block()</code> it increases the sleep time value, stepping with the step value, until it reaches the maximum sleep time value.
 * Once it reaches the maximum sleep time value it remains sleeping with the maximum value.
 * When <code>reset()</code> is called it then starts over from the start sleep time value.
 */
public class BusySleepBackOffWaitStrategy extends AbstractWaitStrategy {
	
	public static final long DEFAULT_START_SLEEP_TIME_IN_NANOS = 1_000;
	public static final long DEFAULT_MAX_SLEEP_TIME_IN_NANOS = 1_000_000;
	public static final int DEFAULT_STEP_IN_NANOS = 1_000;
	
	private final long startSleepTimeInNanos;
	private final long maxSleepTimeInNanos;
	private final int stepInNanos;
	
	private long currSleepTimeInNanos;

	public BusySleepBackOffWaitStrategy(long maxBlockCount, long startSleepTimeInNanos, long maxSleepTimeInNanos, int stepInNanos) {
		super(maxBlockCount);
		this.startSleepTimeInNanos = startSleepTimeInNanos;
		this.maxSleepTimeInNanos = maxSleepTimeInNanos;
		this.stepInNanos = stepInNanos;
		this.currSleepTimeInNanos = startSleepTimeInNanos;
	}
	
	public BusySleepBackOffWaitStrategy(long maxBlockCount) {
		this(maxBlockCount, DEFAULT_START_SLEEP_TIME_IN_NANOS, DEFAULT_MAX_SLEEP_TIME_IN_NANOS, DEFAULT_STEP_IN_NANOS);
	}
	
	public BusySleepBackOffWaitStrategy() {
		this(DEFAULT_MAX_BLOCK_COUNT, DEFAULT_START_SLEEP_TIME_IN_NANOS, DEFAULT_MAX_SLEEP_TIME_IN_NANOS, DEFAULT_STEP_IN_NANOS);
	}
	
	public BusySleepBackOffWaitStrategy(long startSleepTimeInNanos, long maxSleepTimeInNanos, int stepInNanos) {
		this(DEFAULT_MAX_BLOCK_COUNT, startSleepTimeInNanos, maxSleepTimeInNanos, stepInNanos);
	}
	
	@Override
	protected final void blockOperation() {

		BusySleepWaitStrategy.sleepFor(currSleepTimeInNanos);
		
		if (currSleepTimeInNanos == 1) {
			currSleepTimeInNanos = currSleepTimeInNanos + stepInNanos - 1;
		} else {
			currSleepTimeInNanos += stepInNanos;
		}
		
		currSleepTimeInNanos = Math.min(maxSleepTimeInNanos, currSleepTimeInNanos);
	}
	
	@Override
	protected final void resetOperation() {
		currSleepTimeInNanos = startSleepTimeInNanos;
	}
}