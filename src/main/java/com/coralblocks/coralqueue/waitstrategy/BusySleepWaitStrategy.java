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
 * A wait strategy that calls the {@link sleepFor(long)} method as its blocking operation.
 */
public class BusySleepWaitStrategy extends AbstractWaitStrategy {
	
	public final static long DEFAULT_SLEEP_TIME_IN_NANOS = 100;
	
	private final long sleepTimeInNanos;

	public BusySleepWaitStrategy(long maxBlockCount, long sleepTimeInNanos) {
		super(maxBlockCount);
		this.sleepTimeInNanos = sleepTimeInNanos;
	}
	
	public BusySleepWaitStrategy(long sleepTimeInNanos) {
		this(DEFAULT_MAX_BLOCK_COUNT, sleepTimeInNanos);
	}
	
	public BusySleepWaitStrategy() {
		this(DEFAULT_MAX_BLOCK_COUNT, DEFAULT_SLEEP_TIME_IN_NANOS);
	}

	@Override
	protected final void blockOperation() {
		sleepFor(sleepTimeInNanos);
	}
	
	/**
	 * This method uses <code>Sytem.nanoTime()</code> to loop until the given number of nanoseconds has elapsed.
	 * 
	 * @param nanos the number of nanoseconds to wait for
	 * @return the number of times it has to loop to reach the give number of nanoseconds
	 */
    public final static long sleepFor(long nanos) {
    	// NOTE: we are returning loops from a public method
    	// just to avoid code removal (just to be safe)
    	long loops = 0;
        long time = System.nanoTime();
        while((System.nanoTime() - time) < nanos) {
        	loops++;
        }
        return loops;
    }
}