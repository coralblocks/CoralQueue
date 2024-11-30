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

public class BusySpinSleepBackOffWaitStrategy extends AbstractCompositeWaitStrategy {
	
	public static final long DEFAULT_MAX_BUSY_SPIN_COUNT = 10_000_000;
	public static final long DEFAULT_START_SLEEP_TIME_IN_MILLIS = 1;
	public static final long DEFAULT_MAX_SLEEP_TIME_IN_MILLIS = 100;
	public static final int DEFAULT_STEP_IN_MILLIS = 1;
	
	private final CompositeWaitStrategy compositeWS;
	
	public BusySpinSleepBackOffWaitStrategy(long maxBusySpinCount, long startSleepTimeInMillis, long maxSleepTimeInMillis, int stepInMillis) {
		
		WaitStrategy spinWS = new BusySpinWaitStrategy(maxBusySpinCount);
		WaitStrategy sleepWS = new SleepBackOffWaitStrategy(startSleepTimeInMillis, maxSleepTimeInMillis, stepInMillis);
		
		this.compositeWS = new CompositeWaitStrategy(spinWS, sleepWS);
	}
	
	public BusySpinSleepBackOffWaitStrategy() {
		this(DEFAULT_MAX_BUSY_SPIN_COUNT, DEFAULT_START_SLEEP_TIME_IN_MILLIS, DEFAULT_MAX_SLEEP_TIME_IN_MILLIS, DEFAULT_STEP_IN_MILLIS);
	}
	
	public BusySpinSleepBackOffWaitStrategy(long maxBusySpinCount) {
		this(maxBusySpinCount, DEFAULT_START_SLEEP_TIME_IN_MILLIS, DEFAULT_MAX_SLEEP_TIME_IN_MILLIS, DEFAULT_STEP_IN_MILLIS);
	}
	
	@Override
	protected final CompositeWaitStrategy getCompositeWaitStrategy() {
		return compositeWS;
	}
}