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
package com.coralblocks.coralqueue.example.waitstrategy;

import org.junit.Assert;
import org.junit.Test;

import com.coralblocks.coralqueue.queue.AtomicQueue;
import com.coralblocks.coralqueue.queue.Queue;
import com.coralblocks.coralqueue.example.waitstrategy.Basics.Consumer;
import com.coralblocks.coralqueue.example.waitstrategy.Basics.Message;
import com.coralblocks.coralqueue.example.waitstrategy.Basics.Producer;

public class BasicsTest {

	@Test
	public void testInterruptStopsProducerAndConsumer() throws InterruptedException {
		Queue<Message> queue = new AtomicQueue<Message>(1, Message.class);
		Producer producer = new Producer(queue, Integer.MAX_VALUE, 1);
		Consumer consumer = new Consumer(queue);
		producer.setDaemon(true);
		consumer.setDaemon(true);

		producer.start();
		consumer.start();
		producer.interrupt();
		consumer.interrupt();
		producer.join(1_000);
		consumer.join(1_000);

		Assert.assertFalse(producer.isAlive());
		Assert.assertFalse(consumer.isAlive());
	}
}
