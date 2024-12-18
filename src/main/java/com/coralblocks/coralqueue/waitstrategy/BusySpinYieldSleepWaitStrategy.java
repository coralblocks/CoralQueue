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

public class BusySpinYieldSleepWaitStrategy extends AbstractCompositeWaitStrategy {
	
	public static final long DEFAULT_MAX_BUSY_SPIN_COUNT = 20_000_000;
	public static final long DEFAULT_MAX_YIELD_COUNT = 100;
	public static final long DEFAULT_SLEEP_TIME_IN_MILLIS = 1;
	
	private final CompositeWaitStrategy compositeWS;
	
	public BusySpinYieldSleepWaitStrategy(long maxBusySpinCount, long maxYieldCount, long sleepTimeInMillis) {
		WaitStrategy spinWS = new BusySpinWaitStrategy(maxBusySpinCount);
		WaitStrategy yieldWS = new YieldWaitStrategy(maxYieldCount);
		WaitStrategy sleepWS = new SleepWaitStrategy(sleepTimeInMillis);
		this.compositeWS = new CompositeWaitStrategy(spinWS, yieldWS, sleepWS);
	}
	
	public BusySpinYieldSleepWaitStrategy() {
		this(DEFAULT_MAX_BUSY_SPIN_COUNT, DEFAULT_MAX_YIELD_COUNT, DEFAULT_SLEEP_TIME_IN_MILLIS);
	}
	
	@Override
	protected final CompositeWaitStrategy getCompositeWaitStrategy() {
		return compositeWS;
	}
}