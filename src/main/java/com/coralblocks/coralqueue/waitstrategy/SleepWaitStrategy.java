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
 * A wait strategy that calls <code>Thread.sleep(long)</code> for its await operation.
 */
public class SleepWaitStrategy extends AbstractWaitStrategy {
	
	public static final long DEFAULT_SLEEP_TIME_IN_MILLIS = 1;
	
	private final long sleepTimeInMillis;

	public SleepWaitStrategy(long maxAwaitCycleCount, long sleepTimeInMillis) {
		super(maxAwaitCycleCount);
		this.sleepTimeInMillis = sleepTimeInMillis;
	}
	
	public SleepWaitStrategy(long sleepTimeInMillis) {
		this(DEFAULT_MAX_AWAIT_CYCLE_COUNT, sleepTimeInMillis);
	}
	
	public SleepWaitStrategy() {
		this(DEFAULT_MAX_AWAIT_CYCLE_COUNT, DEFAULT_SLEEP_TIME_IN_MILLIS);
	}

	@Override
	protected final void awaitOperation() {
		try {
			Thread.sleep(sleepTimeInMillis);
		} catch(InterruptedException e) {
			throw new RuntimeException(e);
		}
	}
}