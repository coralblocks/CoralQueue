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
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.coralblocks.coralqueue.waitstrategy;

import org.junit.Assert;
import org.junit.Test;

public class WaitStrategyValidationTest {

	private interface BackOffBuilder {
		void build(long start, long max, int step);
	}

	@Test
	public void testAwaitCycleLimitAndReset() {
		WaitStrategy waitStrategy = new BusySpinWaitStrategy(2);

		Assert.assertFalse(waitStrategy.await());
		Assert.assertTrue(waitStrategy.await());
		Assert.assertTrue(waitStrategy.await());

		waitStrategy.reset();
		Assert.assertFalse(waitStrategy.await());
	}

	@Test
	public void testCompositeRequiresStrategies() {
		Assert.assertThrows(NullPointerException.class, () -> new CompositeWaitStrategy((WaitStrategy[]) null));
		Assert.assertThrows(IllegalArgumentException.class, () -> new CompositeWaitStrategy());
		Assert.assertThrows(NullPointerException.class, () -> new CompositeWaitStrategy(new BusySpinWaitStrategy(), null));
	}

	@Test
	public void testParkBackOffArguments() {
		assertBackOffArguments((start, max, step) -> new ParkBackOffWaitStrategy(start, max, step));
	}

	@Test
	public void testSleepBackOffArguments() {
		assertBackOffArguments((start, max, step) -> new SleepBackOffWaitStrategy(start, max, step));
	}

	@Test
	public void testBusySleepBackOffArguments() {
		assertBackOffArguments((start, max, step) -> new BusySleepBackOffWaitStrategy(start, max, step));
	}

	private static void assertBackOffArguments(BackOffBuilder builder) {
		Assert.assertThrows(IllegalArgumentException.class, () -> builder.build(-1, 1, 1));
		Assert.assertThrows(IllegalArgumentException.class, () -> builder.build(0, -1, 1));
		Assert.assertThrows(IllegalArgumentException.class, () -> builder.build(2, 1, 1));
		Assert.assertThrows(IllegalArgumentException.class, () -> builder.build(0, 1, 0));
		Assert.assertThrows(IllegalArgumentException.class, () -> builder.build(0, 1, -1));
	}
}
