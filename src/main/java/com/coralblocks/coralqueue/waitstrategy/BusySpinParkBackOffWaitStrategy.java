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

public class BusySpinParkBackOffWaitStrategy extends AbstractCompositeWaitStrategy {
	
	public final static long DEFAULT_MAX_BUSY_SPIN_COUNT = 10_000_000;
	public final static long DEFAULT_START_PARK_TIME_IN_NANOS = 1_000;
	public final static long DEFAULT_MAX_PARK_TIME_IN_NANOS = 1_000_000;
	public final static int DEFAULT_STEP_IN_NANOS = 1_000;
	
	private final CompositeWaitStrategy compositeWS;
	
	public BusySpinParkBackOffWaitStrategy(long maxBusySpinCount, long startParkTimeInNanos, long maxParkTimeInNanos, int stepInNanos) {
		
		WaitStrategy spinWS = new BusySpinWaitStrategy(maxBusySpinCount);
		WaitStrategy parkWS = new ParkBackOffWaitStrategy(startParkTimeInNanos, maxParkTimeInNanos, stepInNanos);
		
		this.compositeWS = new CompositeWaitStrategy(spinWS, parkWS);
	}
	
	public BusySpinParkBackOffWaitStrategy() {
		this(DEFAULT_MAX_BUSY_SPIN_COUNT, DEFAULT_START_PARK_TIME_IN_NANOS, DEFAULT_MAX_PARK_TIME_IN_NANOS, DEFAULT_STEP_IN_NANOS);
	}
	
	public BusySpinParkBackOffWaitStrategy(long maxBusySpinCount) {
		this(maxBusySpinCount, DEFAULT_START_PARK_TIME_IN_NANOS, DEFAULT_MAX_PARK_TIME_IN_NANOS, DEFAULT_STEP_IN_NANOS);
	}
	
	@Override
	protected final CompositeWaitStrategy getCompositeWaitStrategy() {
		return compositeWS;
	}
}