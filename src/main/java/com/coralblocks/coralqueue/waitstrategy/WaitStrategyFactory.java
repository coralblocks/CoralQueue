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

public class WaitStrategyFactory {
	
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