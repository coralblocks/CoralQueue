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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Assert;
import org.junit.Test;

public class WaitStrategyInterruptTest {

	@Test
	public void testSleepStrategiesWrapInterrupt() {
		assertInterruptIsWrapped(new SleepWaitStrategy(0));
		assertInterruptIsWrapped(new SleepBackOffWaitStrategy(0, 0, 1));
	}

	@Test
	public void testParkStrategiesThrowUncheckedInterrupt() {
		assertUncheckedInterrupt(new ParkWaitStrategy(0));
		assertUncheckedInterrupt(new ParkBackOffWaitStrategy(0, 0, 1));
	}

	@Test
	public void testParkWaitStrategyThrowsUncheckedInterruptWhileWaiting() throws InterruptedException {
		AtomicBoolean interruptionThrown = new AtomicBoolean();
		AtomicBoolean interruptStatusAfterException = new AtomicBoolean();
		Thread thread = new Thread(() -> {
			try {
				new ParkWaitStrategy(TimeUnit.SECONDS.toNanos(10)).await();
			} catch(WaitStrategyInterruptedException expected) {
				interruptionThrown.set(true);
				interruptStatusAfterException.set(Thread.currentThread().isInterrupted());
			}
		});
		thread.setDaemon(true);

		thread.start();
		thread.interrupt();
		thread.join(1_000);

		Assert.assertFalse(thread.isAlive());
		Assert.assertTrue(interruptionThrown.get());
		Assert.assertTrue(interruptStatusAfterException.get());
	}

	@Test
	public void testBusySleepStrategiesThrowUncheckedInterrupt() {
		assertUncheckedInterrupt(new BusySleepWaitStrategy(0));
		assertUncheckedInterrupt(new BusySleepBackOffWaitStrategy(0, 0, 1));
	}

	@Test
	public void testYieldWaitStrategyThrowsUncheckedInterrupt() {
		assertUncheckedInterrupt(new YieldWaitStrategy());
	}

	@Test
	public void testCompositeWaitStrategyThrowsUncheckedInterrupt() {
		assertUncheckedInterrupt(new CompositeWaitStrategy(new ParkWaitStrategy(0)));
	}

	@Test
	public void testBusySpinWaitStrategyPreservesInterrupt() {
		Thread.currentThread().interrupt();
		try {
			Assert.assertFalse(new BusySpinWaitStrategy().await());
			Assert.assertTrue(Thread.currentThread().isInterrupted());
		} finally {
			Thread.interrupted();
		}
	}

	@Test
	public void testBusySpinCompositesThrowAfterSpinStage() {
		assertBusySpinCompositeDefersInterrupt(new BusySpinYieldWaitStrategy(1));
		assertBusySpinCompositeDefersInterrupt(new BusySpinYieldSleepWaitStrategy(1, 1, 0));
		assertBusySpinCompositeDefersInterrupt(new BusySpinParkBackOffWaitStrategy(1, 0, 0, 1));
		assertBusySpinCompositeDefersInterrupt(new BusySpinSleepBackOffWaitStrategy(1, 0, 0, 1));
	}

	private static void assertInterruptIsWrapped(WaitStrategy waitStrategy) {
		Thread.currentThread().interrupt();
		try {
			WaitStrategyInterruptedException exception = Assert.assertThrows(WaitStrategyInterruptedException.class, waitStrategy::await);
			Assert.assertTrue(exception.getCause() instanceof InterruptedException);
			Assert.assertTrue(Thread.currentThread().isInterrupted());
		} finally {
			Thread.interrupted();
		}
	}

	private static void assertUncheckedInterrupt(WaitStrategy waitStrategy) {
		Thread.currentThread().interrupt();
		try {
			Assert.assertThrows(WaitStrategyInterruptedException.class, waitStrategy::await);
			Assert.assertTrue(Thread.currentThread().isInterrupted());
		} finally {
			Thread.interrupted();
		}
	}

	private static void assertBusySpinCompositeDefersInterrupt(WaitStrategy waitStrategy) {
		Thread.currentThread().interrupt();
		try {
			Assert.assertFalse(waitStrategy.await());
			Assert.assertTrue(Thread.currentThread().isInterrupted());
			Assert.assertThrows(WaitStrategyInterruptedException.class, waitStrategy::await);
			Assert.assertTrue(Thread.currentThread().isInterrupted());
		} finally {
			Thread.interrupted();
		}
	}
}
