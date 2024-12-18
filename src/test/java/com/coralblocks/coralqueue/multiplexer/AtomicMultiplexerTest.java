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
package com.coralblocks.coralqueue.multiplexer;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.coralblocks.coralqueue.example.multiplexer.Basics.Consumer;
import com.coralblocks.coralqueue.example.multiplexer.Basics.Message;
import com.coralblocks.coralqueue.example.multiplexer.Basics.Producer;

public class AtomicMultiplexerTest {
	
	@Test
	public void testAll() throws InterruptedException {
		
		final int messagesToSend = 10000;
		final int batchSizeToSend = 100;
		final int numberOfProducers = 4;
		
		final int totalMessagesToSend = messagesToSend * numberOfProducers;
		
		Multiplexer<Message> mux = new AtomicMultiplexer<Message>(Message.class, numberOfProducers);
		
		Producer[] producers = new Producer[numberOfProducers];
		for(int i = 0; i < producers.length; i++) {
			producers[i] = new Producer(mux, i, messagesToSend, batchSizeToSend);
		}
		
		Consumer consumer = new Consumer(mux);
		
		consumer.start();
		for(int i = 0; i < producers.length; i++) producers[i].start();
			
		consumer.join();
		for(int i = 0; i < producers.length; i++) {
			producers[i].join();
		}
		
		List<Message> messagesReceived = consumer.getMessagesReceived();
		List<Long> batchesReceived = consumer.getBatchesReceived();
		
		// Did we receive all messages?
		Assert.assertEquals(totalMessagesToSend, messagesReceived.size());
		
		// Were there any duplicates?
		Assert.assertEquals(messagesReceived.size(), messagesReceived.stream().distinct().count());
		
		// If we sum all batches do we get the correct number of messages?
		long sumOfAllBatches = batchesReceived.stream().mapToLong(Long::longValue).sum();
		Assert.assertEquals(totalMessagesToSend, sumOfAllBatches);
	}
}