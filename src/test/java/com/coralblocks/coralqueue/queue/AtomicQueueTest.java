package com.coralblocks.coralqueue.queue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.coralblocks.coralqueue.example.queue.Basics.Consumer;
import com.coralblocks.coralqueue.example.queue.Basics.Message;
import com.coralblocks.coralqueue.example.queue.Basics.Producer;

public class AtomicQueueTest {
	
	@Test
	public void testAll() throws InterruptedException {
		
		final int messagesToSend = 100000;
		final int batchSizeToSend =100;
		
		AtomicQueue<Message> queue = new AtomicQueue<Message>(Message.class);
		
		Producer producer = new Producer(queue, messagesToSend, batchSizeToSend);
		Consumer consumer = new Consumer(queue);
		
		producer.start();
		consumer.start();
		
		producer.join();
		consumer.join();
		
		List<Long> messagesReceived = consumer.getMessagesReceived();
		List<Long> batchesReceived = consumer.getBatchesReceived();
		
		// Did we receive all messages?
		Assert.assertEquals(messagesReceived.size(), messagesToSend);
		
		// Where there any duplicates?
		Assert.assertEquals(messagesReceived.stream().distinct().count(), messagesReceived.size());
		
		// Where the messages received in order?
		List<Long> sortedList = new ArrayList<Long>(messagesReceived);
		Collections.sort(sortedList);
		Assert.assertEquals(sortedList, messagesReceived);
		
		// If we sum all batches do we get the correct number of messages?
		long sumOfAllBatches = batchesReceived.stream().mapToLong(Long::longValue).sum();
		Assert.assertEquals(sumOfAllBatches, messagesToSend);
	}
}