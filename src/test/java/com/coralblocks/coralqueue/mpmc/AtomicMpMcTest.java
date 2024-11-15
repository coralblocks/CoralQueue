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
package com.coralblocks.coralqueue.mpmc;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.coralblocks.coralqueue.example.mpmc.Basics.Consumer;
import com.coralblocks.coralqueue.example.mpmc.Basics.Message;
import com.coralblocks.coralqueue.example.mpmc.Basics.Producer;

public class AtomicMpMcTest {
	
	@Test
	public void testAll() throws InterruptedException {
		
		final int messagesToSend = 10000;
		final int batchSizeToSend = 100;
		final int numberOfProducers = 4;
		final int numberOfConsumers = 4;
		
		final int totalMessagesToSend = messagesToSend * numberOfProducers;
		
		MpMc<Message> mpmc = new AtomicMpMc<Message>(Message.class, numberOfProducers, numberOfConsumers);
		
		Producer[] producers = new Producer[numberOfProducers];
		for(int i = 0; i < producers.length; i++) {
			producers[i] = new Producer(mpmc, i, messagesToSend, batchSizeToSend, numberOfConsumers);
		}
		
		Consumer[] consumers = new Consumer[numberOfConsumers];
		for(int i = 0; i < consumers.length; i++) {
			consumers[i] = new Consumer(mpmc, i);
		}
		
		for(int i = 0; i < consumers.length; i++) consumers[i].start();
		for(int i = 0; i < producers.length; i++) producers[i].start();
			
		for(int i = 0; i < producers.length; i++) {
			producers[i].join();
		}
		
		for(int i = 0; i < consumers.length; i++) {
			consumers[i].join();
		}
		
		List<Message> messagesReceived = new ArrayList<Message>(totalMessagesToSend);
		for(int i = 0; i < consumers.length; i++) {
			messagesReceived.addAll(consumers[i].getMessagesReceived());
		}
		
		List<Long> batchesReceived = new ArrayList<Long>(totalMessagesToSend);
		for(int i = 0; i < consumers.length; i++) {
			batchesReceived.addAll(consumers[i].getBatchesReceived());
		}
		
		// Did we receive all messages?
		Assert.assertEquals(totalMessagesToSend, messagesReceived.size());
		
		// Were there any duplicates?
		Assert.assertEquals(messagesReceived.size(), messagesReceived.stream().distinct().count());
		
		// If we sum all batches do we get the correct number of messages?
		long sumOfAllBatches = batchesReceived.stream().mapToLong(Long::longValue).sum();
		Assert.assertEquals(totalMessagesToSend, sumOfAllBatches);
	}
}