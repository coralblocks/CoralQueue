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
package com.coralblocks.coralqueue.demultiplexer;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.coralblocks.coralqueue.example.demultiplexer.Basics.Consumer;
import com.coralblocks.coralqueue.example.demultiplexer.Basics.Message;
import com.coralblocks.coralqueue.example.demultiplexer.Basics.Producer;

public class AtomicDemultiplexerTest {
	
	@Test
	public void testAll() throws InterruptedException {
		
		final int messagesToSend = 10000;
		final int batchSizeToSend = 100;
		final int numberOfConsumers = 4;
		
		Demultiplexer<Message> mux = new AtomicDemultiplexer<Message>(Message.class, numberOfConsumers);
		
		Producer producer = new Producer(mux, messagesToSend, batchSizeToSend);
		
		Consumer[] consumers = new Consumer[numberOfConsumers];
		for(int i = 0; i < consumers.length; i++) {
			consumers[i] = new Consumer(mux, i);
		}
		
		for(int i = 0; i < consumers.length; i++) {
			consumers[i].start();
		}
		producer.start();
			
		for(int i = 0; i < consumers.length; i++) {
			consumers[i].join();
		}
		
		producer.join();
		
		List<Long> totalMessagesReceived = new ArrayList<Long>(messagesToSend * numberOfConsumers);
		for(int i = 0; i < consumers.length; i++) {
			totalMessagesReceived.addAll(consumers[i].getMessagesReceived());
		}
		
		List<Long> totalBatchesReceived = new ArrayList<Long>(messagesToSend * numberOfConsumers);
		for(int i = 0; i < consumers.length; i++) {
			totalBatchesReceived.addAll(consumers[i].getBatchesReceived());
		}
		
		// Did we receive all messages?
		Assert.assertEquals(totalMessagesReceived.size(), messagesToSend);
		
		// Were there any duplicates?
		Assert.assertEquals(totalMessagesReceived.stream().distinct().count(), totalMessagesReceived.size());
		
		// If we sum all batches do we get the correct number of messages?
		long sumOfAllBatches = totalBatchesReceived.stream().mapToLong(Long::longValue).sum();
		Assert.assertEquals(sumOfAllBatches, messagesToSend);
	}
}