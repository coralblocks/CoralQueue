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

import java.util.concurrent.locks.LockSupport;

/**
 * A wait strategy that calls <code>LockSupport.parkNanos(long)</code> for its await operation.
 */
public class ParkWaitStrategy extends AbstractWaitStrategy {
	
	public static final long DEFAULT_PARK_TIME_IN_NANOS = 100;
	
	private final long parkTimeInNanos;

	public ParkWaitStrategy(long maxAwaitCycleCount, long parkTimeInNanos) {
		super(maxAwaitCycleCount);
		this.parkTimeInNanos = parkTimeInNanos;
	}
	
	public ParkWaitStrategy(long parkTimeInNanos) {
		this(DEFAULT_MAX_AWAIT_CYCLE_COUNT, parkTimeInNanos);
	}
	
	public ParkWaitStrategy() {
		this(DEFAULT_MAX_AWAIT_CYCLE_COUNT, DEFAULT_PARK_TIME_IN_NANOS);
	}

	@Override
	protected final void awaitOperation() {
		LockSupport.parkNanos(parkTimeInNanos);
	}
}