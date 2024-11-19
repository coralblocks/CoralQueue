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
 * A wait strategy that parks with backing off. It has a start park time in nanoseconds, a maximum park time in nanoseconds and a step value in nanoseconds.
 * Basically with each <code>block()</code> it increases the park time value, stepping with the step value, until it reaches the maximum park time value.
 * Once it reaches the maximum park time value it remains parking with the maximum value.
 * When <code>reset()</code> is called it then starts over from the start park time value.
 */
public class ParkBackOffWaitStrategy extends AbstractWaitStrategy {
	
	public final static long DEFAULT_START_PARK_TIME_IN_NANOS = 1_000;
	public final static long DEFAULT_MAX_PARK_TIME_IN_NANOS = 1_000_000;
	public final static int DEFAULT_STEP_IN_NANOS = 1_000;
	
	private final long startParkTimeInNanos;
	private final long maxParkTimeInNanos;
	private final int stepInNanos;
	
	private long currParkTimeInNanos;

	public ParkBackOffWaitStrategy(long maxBlockCount, long startParkTimeInNanos, long maxParkTimeInNanos, int stepInNanos) {
		super(maxBlockCount);
		this.startParkTimeInNanos = startParkTimeInNanos;
		this.maxParkTimeInNanos = maxParkTimeInNanos;
		this.stepInNanos = stepInNanos;
		this.currParkTimeInNanos = startParkTimeInNanos;
	}
	
	public ParkBackOffWaitStrategy(long maxBlockCount) {
		this(maxBlockCount, DEFAULT_START_PARK_TIME_IN_NANOS, DEFAULT_MAX_PARK_TIME_IN_NANOS, DEFAULT_STEP_IN_NANOS);
	}
	
	public ParkBackOffWaitStrategy() {
		this(DEFAULT_MAX_BLOCK_COUNT, DEFAULT_START_PARK_TIME_IN_NANOS, DEFAULT_MAX_PARK_TIME_IN_NANOS, DEFAULT_STEP_IN_NANOS);
	}
	
	public ParkBackOffWaitStrategy(long startParkTimeInNanos, long maxParkTimeInNanos, int stepInNanos) {
		this(DEFAULT_MAX_BLOCK_COUNT, startParkTimeInNanos, maxParkTimeInNanos, stepInNanos);
	}
	
	@Override
	protected final void blockOperation() {

		LockSupport.parkNanos(currParkTimeInNanos);
		
		if (currParkTimeInNanos == 1) {
			currParkTimeInNanos = currParkTimeInNanos + stepInNanos - 1;
		} else {
			currParkTimeInNanos += stepInNanos;
		}
		
		currParkTimeInNanos = Math.min(maxParkTimeInNanos, currParkTimeInNanos);
	}
	
	@Override
	protected final void resetOperation() {
		currParkTimeInNanos = startParkTimeInNanos;
	}
}