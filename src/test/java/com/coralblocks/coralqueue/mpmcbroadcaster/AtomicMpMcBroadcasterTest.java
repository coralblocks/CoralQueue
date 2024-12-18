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
package com.coralblocks.coralqueue.mpmcbroadcaster;

import org.junit.Assert;
import org.junit.Test;

import com.coralblocks.coralqueue.example.mpmcbroadcaster.Basics.Consumer;
import com.coralblocks.coralqueue.example.mpmcbroadcaster.Basics.Message;
import com.coralblocks.coralqueue.example.mpmcbroadcaster.Basics.Producer;


public class AtomicMpMcBroadcasterTest {
	
	@Test
	public void testAll() throws InterruptedException {
		
		final int messagesToSend = 10000;
		final int batchSizeToSend = 100;
		final int numberOfProducers = 4;
		final int numberOfConsumers = 4;
		
		final int totalMessagesToSend = messagesToSend * numberOfProducers;
		
		MpMcBroadcaster<Message> mpmcBroadcaster = new AtomicMpMcBroadcaster<Message>(Message.class, numberOfProducers, numberOfConsumers);
		
		Producer[] producers = new Producer[numberOfProducers];
		for(int i = 0; i < producers.length; i++) {
			producers[i] = new Producer(mpmcBroadcaster, i, messagesToSend, batchSizeToSend);
		}
		
		Consumer[] consumers = new Consumer[numberOfConsumers];
		for(int i = 0; i < consumers.length; i++) {
			consumers[i] = new Consumer(mpmcBroadcaster, i);
		}
		
		for(int i = 0; i < consumers.length; i++) consumers[i].start();
		for(int i = 0; i < producers.length; i++) producers[i].start();
			
		for(int i = 0; i < producers.length; i++) {
			producers[i].join();
		}
		
		for(int i = 0; i < consumers.length; i++) {
			consumers[i].join();
		}
		
		// Did we receive all messages?
		for(int i = 0; i < consumers.length; i++) {
			Assert.assertEquals(totalMessagesToSend, consumers[i].getMessagesReceived().size());
		}
		
		// Were there any duplicates?
		for(int i = 0; i < consumers.length; i++) {
			Assert.assertEquals(consumers[i].getMessagesReceived().size(), consumers[i].getMessagesReceived().stream().distinct().count());
		}
		
		// If we sum all batches received do we get the correct number of messages?
		for(int i = 0; i < consumers.length; i++) {
			long sumOfAllBatches = consumers[i].getBatchesReceived().stream().mapToLong(Long::longValue).sum();
			Assert.assertEquals(totalMessagesToSend, sumOfAllBatches);
		}
	}
}