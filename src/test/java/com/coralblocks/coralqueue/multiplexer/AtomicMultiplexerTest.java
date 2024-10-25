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
		
		AtomicMultiplexer<Message> mux = new AtomicMultiplexer<Message>(Message.class, numberOfProducers);
		
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
		Assert.assertEquals(messagesReceived.size(), totalMessagesToSend);
		
		// Where there any duplicates?
		Assert.assertEquals(messagesReceived.stream().distinct().count(), messagesReceived.size());
		
		// If we sum all batches do we get the correct number of messages?
		long sumOfAllBatches = batchesReceived.stream().mapToLong(Long::longValue).sum();
		Assert.assertEquals(sumOfAllBatches, totalMessagesToSend);
	}
}