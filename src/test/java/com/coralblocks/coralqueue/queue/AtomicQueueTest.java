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
package com.coralblocks.coralqueue.queue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.coralblocks.coralqueue.example.queue.Basics.Consumer;
import com.coralblocks.coralqueue.example.queue.Basics.Message;
import com.coralblocks.coralqueue.example.queue.Basics.Producer;

public class AtomicQueueTest {

	@Test
	public void testSwapReplacesDispatchedSlot() {

		Queue<Message> queue = new AtomicQueue<Message>(1, Message.class);
		Message swap = new Message();

		Message pooled = queue.nextToDispatch(swap);
		Assert.assertNotSame(swap, pooled);
		queue.flush();

		Assert.assertSame(swap, queue.fetch());
		queue.doneFetching();
		Assert.assertSame(swap, queue.nextToDispatch(pooled));
	}

	@Test
	public void testReplaceChangesFetchedSlot() {

		Queue<Message> queue = new AtomicQueue<Message>(1, Message.class);
		Message original = queue.nextToDispatch();
		queue.flush();

		Assert.assertSame(original, queue.fetch());
		Message replacement = new Message();
		queue.replace(replacement);
		queue.doneFetching();

		Assert.assertSame(replacement, queue.nextToDispatch());
	}

	@Test
	public void testRollBackReplaysFetchedObjects() {

		Queue<Message> queue = new AtomicQueue<Message>(2, Message.class);
		Message first = queue.nextToDispatch();
		Message second = queue.nextToDispatch();
		queue.flush();

		Assert.assertSame(first, queue.fetch());
		Assert.assertSame(second, queue.fetch());

		queue.rollBack(1);
		Assert.assertEquals(1, queue.availableToFetch());
		Assert.assertSame(second, queue.fetch());

		queue.rollBack();
		Assert.assertEquals(2, queue.availableToFetch());
		Assert.assertSame(first, queue.fetch());
		Assert.assertSame(second, queue.fetch());
		queue.doneFetching();
	}

	@Test
	public void testClearDiscardsMessagesAndAllowsReuse() {

		Queue<Message> queue = new AtomicQueue<Message>(1, Message.class);
		Message message = queue.nextToDispatch();
		queue.flush();
		Assert.assertEquals(1, queue.availableToFetch());

		queue.clear();

		Assert.assertEquals(0, queue.availableToFetch());
		Assert.assertSame(message, queue.nextToDispatch());
		Assert.assertNull(queue.nextToDispatch());
	}

	@Test
	public void testNullSwapIsRejectedWithoutClaimingSlot() {

		Queue<Message> queue = new AtomicQueue<Message>(1, Message.class);

		try {
			queue.nextToDispatch(null);
			Assert.fail("Expected NullPointerException");
		} catch(NullPointerException expected) {
			// expected
		}

		Assert.assertNotNull(queue.nextToDispatch());
	}

	@Test
	public void testFetchWithoutRemoveRejectsEmptyQueue() {

		Queue<Message> queue = new AtomicQueue<Message>(1, Message.class);

		try {
			queue.fetch(false);
			Assert.fail("Expected IllegalStateException");
		} catch(IllegalStateException expected) {
			// expected
		}
	}

	@Test
	public void testReplaceRequiresFetchedObject() {

		Queue<Message> queue = new AtomicQueue<Message>(1, Message.class);
		Message replacement = new Message();

		try {
			queue.replace(replacement);
			Assert.fail("Expected IllegalStateException");
		} catch(IllegalStateException expected) {
			// expected
		}

		Assert.assertNotSame(replacement, queue.nextToDispatch());
	}

	@Test
	public void testReplaceRejectsNull() {

		Queue<Message> queue = new AtomicQueue<Message>(1, Message.class);

		queue.nextToDispatch();
		queue.flush();
		queue.fetch();

		try {
			queue.replace(null);
			Assert.fail("Expected NullPointerException");
		} catch(NullPointerException expected) {
			// expected
		}
	}
	
	@Test
	public void testAll() throws InterruptedException {
		
		final int messagesToSend = 100000;
		final int batchSizeToSend = 100;
		
		Queue<Message> queue = new AtomicQueue<Message>(Message.class);
		
		Producer producer = new Producer(queue, messagesToSend, batchSizeToSend);
		Consumer consumer = new Consumer(queue);
		
		producer.start();
		consumer.start();
		
		producer.join();
		consumer.join();
		
		List<Long> messagesReceived = consumer.getMessagesReceived();
		List<Long> batchesReceived = consumer.getBatchesReceived();
		
		// Did we receive all messages?
		Assert.assertEquals(messagesToSend, messagesReceived.size());
		
		// Where there any duplicates?
		Assert.assertEquals(messagesReceived.size(), messagesReceived.stream().distinct().count());
		
		// Were the messages received in order?
		List<Long> sortedList = new ArrayList<Long>(messagesReceived);
		Collections.sort(sortedList);
		Assert.assertEquals(messagesReceived, sortedList);
		
		// If we sum all batches do we get the correct number of messages?
		long sumOfAllBatches = batchesReceived.stream().mapToLong(Long::longValue).sum();
		Assert.assertEquals(messagesToSend, sumOfAllBatches);
	}
}
