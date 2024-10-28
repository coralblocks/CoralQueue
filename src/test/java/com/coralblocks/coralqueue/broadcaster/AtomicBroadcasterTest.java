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
package com.coralblocks.coralqueue.broadcaster;

import org.junit.Assert;
import org.junit.Test;

import com.coralblocks.coralqueue.example.broadcaster.Basics.Consumer;
import com.coralblocks.coralqueue.example.broadcaster.Basics.Message;
import com.coralblocks.coralqueue.example.broadcaster.Basics.Producer;


public class AtomicBroadcasterTest {
	
	@Test
	public void testAll() throws InterruptedException {
		
		final int messagesToSend = 10000;
		final int batchSizeToSend = 100;
		final int numberOfConsumers = 4;
		
		AtomicBroadcaster<Message> broadcaster = new AtomicBroadcaster<Message>(Message.class, numberOfConsumers);
		
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
			Assert.assertEquals(consumers[i].getMessagesReceived().size(),  messagesToSend);
		}
		
		// Where there any duplicates?
		for(int i = 0; i < consumers.length; i++) {
			Assert.assertEquals(consumers[i].getMessagesReceived().stream().distinct().count(), consumers[i].getMessagesReceived().size());
		}
		
		// If we sum all batches received do we get the correct number of messages?
		for(int i = 0; i < consumers.length; i++) {
			long sumOfAllBatches = consumers[i].getBatchesReceived().stream().mapToLong(Long::longValue).sum();
			Assert.assertEquals(sumOfAllBatches, messagesToSend);
		}
	}
}