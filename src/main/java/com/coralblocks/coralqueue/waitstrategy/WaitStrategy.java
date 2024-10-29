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
 * <p>A <code>WaitStrategy</code> interface describing how a producer and/or consumer can choose to wait (i.e. block) while its CoralQueue data structure is full/empty.</p>
 * 
 * <p>Note that using a <code>WaitStrategy</code> is not mandatory as producers and consumers can simply choose to busy spin. However, as a CPU core is a scarce resource, there
 * can be situations where you will want to not waste clock cycles by busy spinning.</p>
 */
public interface WaitStrategy {
	
	/**
	 * Do something to wait: busy spin, park, yield, sleep, back off, etc.
	 */
	public void block();
	
	/**
	 * Reset the strategy because we have waited and we have accomplished what we were waiting for.
	 */
	public void reset();
	
	/**
	 * Optional method to return the number of times this wait strategy has blocked, if supported
	 * 
	 * @return the number of times this wait strategy has blocked or -1 if blocking is not supported
	 */
	default public long getTotalBlockCount() {
		return -1;
	}
	
	/**
	 * Optional method to reset the total block count number to zero. If not supported, this method will do nothing.
	 */
	default public void resetTotalBlockCount() {
		
	}
	
	/**
	 * A factory static method to get a new <code>WaitStrategy</code> instance.
	 * 
	 * @param type the type of wait strategy we want
	 * @return a new instance of the <code>WaitStrategy</code>
	 */
	public static WaitStrategy getWaitStrategy(String type) {
		if (type.equalsIgnoreCase("spin")) {
			return new SpinWaitStrategy();
		} else if (type.equalsIgnoreCase("park")) {
			return new ParkWaitStrategy();
		} else if (type.equalsIgnoreCase("park-backoff")) {
			return new ParkWaitStrategy(true);
		} else if (type.equalsIgnoreCase("spinYieldPark")) {
			return new SpinYieldParkWaitStrategy();
		} else if (type.equalsIgnoreCase("spinYieldPark-backoff")) {
			return new SpinYieldParkWaitStrategy(true);
		} else if (type.equalsIgnoreCase("spinPark")) {
			return new SpinParkWaitStrategy();
		} else if (type.equalsIgnoreCase("spinPark-backoff")) {
			return new SpinParkWaitStrategy(true);
		} else if (type.equalsIgnoreCase("yieldPark")) {
			return new YieldParkWaitStrategy();
		} else if (type.equalsIgnoreCase("yieldPark-backoff")) {
			return new YieldParkWaitStrategy(true);
		} else if (type.equalsIgnoreCase("sleep")) {
			return new SleepWaitStrategy();
		} else if (type.equalsIgnoreCase("sleep-backoff")) {
			return new SleepWaitStrategy(true);
		} else if (type.equalsIgnoreCase("spinYieldSleep")) {
			return new SpinYieldSleepWaitStrategy();
		} else if (type.equalsIgnoreCase("spinYieldSleep-backoff")) {
			return new SpinYieldSleepWaitStrategy(true);
		} else if (type.equalsIgnoreCase("spinSleep")) {
			return new SpinSleepWaitStrategy();
		} else if (type.equalsIgnoreCase("spinSleep-backoff")) {
			return new SpinSleepWaitStrategy(true);
		} else if (type.equalsIgnoreCase("yieldSleep")) {
			return new YieldSleepWaitStrategy();
		} else if (type.equalsIgnoreCase("yieldSleep-backoff")) {
			return new YieldSleepWaitStrategy(true);
		} else {
			throw new IllegalArgumentException("Cannot create wait strategy for type: " + type);
		}
	}
}
