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
package com.coralblocks.coralqueue.broadcaster;

import org.junit.Assert;
import org.junit.Test;

import com.coralblocks.coralqueue.example.broadcaster.Basics.Consumer;
import com.coralblocks.coralqueue.example.broadcaster.Basics.Message;
import com.coralblocks.coralqueue.example.broadcaster.Basics.Producer;


public class AtomicBroadcasterTest {

	@Test
	public void testDelegateCannotClearBroadcaster() {
		Broadcaster<Message> broadcaster = new AtomicBroadcaster<Message>(1, Message.class, 2);
		BroadcasterDelegateQueue<Message> delegate = new BroadcasterDelegateQueue<Message>(broadcaster, 0);

		Assert.assertNotNull(broadcaster.nextToDispatch());
		broadcaster.flush();
		Assert.assertEquals(1, broadcaster.availableToFetch(0));
		Assert.assertEquals(1, broadcaster.availableToFetch(1));

		Assert.assertThrows(UnsupportedOperationException.class, delegate::clear);

		Assert.assertEquals(1, broadcaster.availableToFetch(0));
		Assert.assertEquals(1, broadcaster.availableToFetch(1));
	}

	@Test
	public void testDisabledConsumerCannotFetch() {
		Broadcaster<Message> broadcaster = new AtomicBroadcaster<Message>(1, Message.class, 1);
		broadcaster.disableConsumer(0);

		Assert.assertNotNull(broadcaster.nextToDispatch());
		broadcaster.flush();
		Assert.assertEquals(0, broadcaster.availableToFetch(0));

		try {
			broadcaster.fetch(0);
			Assert.fail("Expected IllegalStateException");
		} catch(IllegalStateException expected) {
			// expected
		}
	}

	@Test
	public void testDoneFetchingCannotReenableDisabledConsumer() {
		Broadcaster<Message> broadcaster = new AtomicBroadcaster<Message>(1, Message.class, 1);

		Assert.assertNotNull(broadcaster.nextToDispatch());
		broadcaster.flush();
		Assert.assertEquals(1, broadcaster.availableToFetch(0));
		Assert.assertNotNull(broadcaster.fetch(0));
		broadcaster.disableConsumer(0);

		try {
			broadcaster.doneFetching(0);
			Assert.fail("Expected IllegalStateException");
		} catch(IllegalStateException expected) {
			// expected
		}

		Assert.assertNotNull(broadcaster.nextToDispatch());
		Assert.assertNotNull(broadcaster.nextToDispatch());
	}

	@Test
	public void testDisableIsPermanentAcrossClear() {
		Broadcaster<Message> broadcaster = new AtomicBroadcaster<Message>(1, Message.class, 1);
		broadcaster.disableConsumer(0);
		broadcaster.clear();

		try {
			broadcaster.fetch(0);
			Assert.fail("Expected IllegalStateException");
		} catch(IllegalStateException expected) {
			// expected
		}
	}

	@Test
	public void testDisablingAllConsumersDoesNotBlockProducer() {
		Broadcaster<Message> broadcaster = new AtomicBroadcaster<Message>(2, Message.class, 2);
		broadcaster.disableConsumer(0);
		broadcaster.disableConsumer(1);

		Assert.assertNotNull(broadcaster.nextToDispatch());
		Assert.assertNotNull(broadcaster.nextToDispatch());
		Assert.assertNotNull(broadcaster.nextToDispatch());
	}
	
	@Test
	public void testAll() throws InterruptedException {
		
		final int messagesToSend = 10000;
		final int batchSizeToSend = 100;
		final int numberOfConsumers = 4;
		
		Broadcaster<Message> broadcaster = new AtomicBroadcaster<Message>(Message.class, numberOfConsumers);
		
		Producer producer = new Producer(broadcaster, messagesToSend, batchSizeToSend);
		
		Consumer[] consumers = new Consumer[numberOfConsumers];
		for(int i = 0; i < consumers.length; i++) {
			consumers[i] = new Consumer(broadcaster, i);
		}
		
		for(int i = 0; i < consumers.length; i++) {
			consumers[i].start();
		}
		producer.start();
			
		for(int i = 0; i < consumers.length; i++) {
			consumers[i].join();
		}
		
		producer.join();
		
		// Did all consumers receive all messages?
		for(int i = 0; i < consumers.length; i++) {
			Assert.assertEquals(messagesToSend, consumers[i].getMessagesReceived().size());
		}
		
		// Were there any duplicates?
		for(int i = 0; i < consumers.length; i++) {
			Assert.assertEquals(consumers[i].getMessagesReceived().size(), consumers[i].getMessagesReceived().stream().distinct().count());
		}
		
		// If we sum all batches received do we get the correct number of messages?
		for(int i = 0; i < consumers.length; i++) {
			long sumOfAllBatches = consumers[i].getBatchesReceived().stream().mapToLong(Long::longValue).sum();
			Assert.assertEquals(messagesToSend, sumOfAllBatches);
		}
	}
}
