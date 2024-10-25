package com.coralblocks.coralqueue.multiplexer;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.coralblocks.coralqueue.example.multiplexer.DynamicBasics.Consumer;
import com.coralblocks.coralqueue.example.multiplexer.DynamicBasics.Message;
import com.coralblocks.coralqueue.example.multiplexer.DynamicBasics.Producer;

public class AtomicDynamicMultiplexerTest {
	
	@Test
	public void testAll() throws InterruptedException {
		
		final int messagesToSend = 10000;
		final int batchSizeToSend = 100;
		final int initialNumberOfProducers = 4;
		final int extraProducersCreatedByProducer = 2;
		final int extraProducersCreatedByConsumer = 2;
		
		final int finalNumberOfProducers = initialNumberOfProducers + extraProducersCreatedByProducer + extraProducersCreatedByConsumer;
		
		final int totalMessagesToSend = messagesToSend * finalNumberOfProducers;
		
		AtomicDynamicMultiplexer<Message> mux = new AtomicDynamicMultiplexer<Message>(Message.class, initialNumberOfProducers);
		
		Producer[] extraProdProducer = new Producer[extraProducersCreatedByProducer];
		Producer[] extraProdConsumer = new Producer[extraProducersCreatedByConsumer];

		for(int i = 0; i < extraProdProducer.length; i++) {
			extraProdProducer[i] = new Producer(mux, messagesToSend, batchSizeToSend, null);
		}
		
		for(int i = 0; i < extraProdConsumer.length; i++) {
			extraProdConsumer[i] = new Producer(mux, messagesToSend, batchSizeToSend, null);
		}
		
		Producer[] producers = new Producer[initialNumberOfProducers];
		
		for(int i = 0; i < producers.length; i++) {
			producers[i] = new Producer(mux, messagesToSend, batchSizeToSend, i == 0 ? extraProdProducer : null);
		}
		
		Consumer consumer = new Consumer(mux, finalNumberOfProducers, totalMessagesToSend, extraProdConsumer);
		
		consumer.start();
		for(int i = 0; i < producers.length; i++) producers[i].start();
			
		for(int i = 0; i < extraProdProducer.length; i++) {
			extraProdProducer[i].join();
		}
		
		for(int i = 0; i < extraProdConsumer.length; i++) {
			extraProdConsumer[i].join();
		}
		
		for(int i = 0; i < producers.length; i++) {
			producers[i].join();
		}
		
		consumer.join();
		
		System.out.println();
		
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