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
		
		AtomicDemultiplexer<Message> mux = new AtomicDemultiplexer<Message>(Message.class, numberOfConsumers);
		
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
		
		// Where there any duplicates?
		Assert.assertEquals(totalMessagesReceived.stream().distinct().count(), totalMessagesReceived.size());
		
		// If we sum all batches do we get the correct number of messages?
		long sumOfAllBatches = totalBatchesReceived.stream().mapToLong(Long::longValue).sum();
		Assert.assertEquals(sumOfAllBatches, messagesToSend);
	}
}