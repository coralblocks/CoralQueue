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
		
		AtomicMpMc<Message> mpmc = new AtomicMpMc<Message>(Message.class, numberOfProducers, numberOfConsumers);
		
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
		Assert.assertEquals(messagesReceived.size(), totalMessagesToSend);
		
		// Where there any duplicates?
		Assert.assertEquals(messagesReceived.stream().distinct().count(), messagesReceived.size());
		
		// If we sum all batches do we get the correct number of messages?
		long sumOfAllBatches = batchesReceived.stream().mapToLong(Long::longValue).sum();
		Assert.assertEquals(sumOfAllBatches, totalMessagesToSend);
	}
}