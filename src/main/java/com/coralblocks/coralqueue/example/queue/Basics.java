package com.coralblocks.coralqueue.example.queue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.coralblocks.coralqueue.queue.AtomicQueue;

public class Basics {
	
	public static class Message {
		long value;
		boolean last;
	}
	
	public static class Producer extends Thread {
		
		private final AtomicQueue<Message> queue;
		private final int messagesToSend;
		private final int batchSizeToSend;
		private int idToSend = 1;
		private long busySpinCount = 0;
		
		public Producer(AtomicQueue<Message> queue, int messagesToSend, int batchSizeToSend) {
			super(Producer.class.getSimpleName()); // name of the thread
			this.queue = queue;
			this.messagesToSend = messagesToSend;
			this.batchSizeToSend = batchSizeToSend;
		}
		
		public long getBusySpinCount() {
			return busySpinCount;
		}
		
		@Override
		public final void run() {
			int messagesSent = 0;
			int remaining = messagesToSend - messagesSent;
			while(remaining > 0) {
				int batchToSend = Math.min(batchSizeToSend, remaining);
				for(int i = 0; i < batchToSend; i++) {
					Message m;
					while((m = queue.nextToDispatch()) == null) { // <=========
						// busy spin (default and fastest wait strategy)
						busySpinCount++;
					}
					m.value = idToSend++;
					m.last = m.value == messagesToSend; // is it the last message I'll be sending?
				}
				queue.flush(); // <=========
				remaining -= batchToSend;
			}
		}
	}
	
	public static class Consumer extends Thread {
		
		private final AtomicQueue<Message> queue;
		private final List<Long> messagesReceived  = new ArrayList<Long>();
		private final List<Long> batchesReceived = new ArrayList<Long>();
		private long busySpinCount = 0;
		
		public Consumer(AtomicQueue<Message> queue) {
			super(Consumer.class.getSimpleName()); // name of the thread
			this.queue = queue;
		}
		
		public List<Long> getMessagesReceived() {
			return messagesReceived;
		}
		
		public List<Long> getBatchesReceived() {
			return batchesReceived;
		}
		
		public long getBusySpinCount() {
			return busySpinCount;
		}
		
		@Override
		public final void run() {
			boolean isRunning = true;
			while(isRunning) {
				long avail = queue.availableToPoll(); // <=========
				if (avail > 0) {
					for(long i = 0; i < avail; i++) {
						Message m = queue.poll(); // <=========
						messagesReceived.add(m.value);
						if (m.last) isRunning = false; 
					}
					queue.donePolling(); // <=========
					batchesReceived.add(avail);
				} else {
					// busy spin (default and fastest wait strategy)
					busySpinCount++;
				}
			}
		}
	}
	
	public static void main(String[] args) throws InterruptedException {
		
		final int messagesToSend = args.length > 0 ? Integer.parseInt(args[0]) : 10000;
		final int batchSizeToSend = args.length > 1 ? Integer.parseInt(args[1]) : 100;
		
		AtomicQueue<Message> queue = new AtomicQueue<Message>(Message.class);
		
		Producer producer = new Producer(queue, messagesToSend, batchSizeToSend);
		Consumer consumer = new Consumer(queue);
		
		System.out.println("Producer will send " + messagesToSend + " messages in batches of " + batchSizeToSend + " messages...\n");
		
		producer.start();
		consumer.start();
		
		producer.join();
		System.out.println("Thread " + producer.getName() + " done and exited...");
		
		consumer.join();
		System.out.println("Thread " + consumer.getName() + " done and exited...");
		
		System.out.println();
		
		List<Long> messagesReceived = consumer.getMessagesReceived();
		List<Long> batchesReceived = consumer.getBatchesReceived();
		
		// Did we receive all messages?
		if (messagesReceived.size() == messagesToSend) System.out.println("SUCCESS: All messages received! => " + messagesToSend);
		else System.out.println("ERROR: Wrong number of messages received! => " + messagesReceived.size());
		
		// Where there any duplicates?
		if (messagesReceived.stream().distinct().count() == messagesReceived.size()) System.out.println("SUCCESS: No duplicate messages were received!");
		else System.out.println("ERROR: Found duplicate messages!");
		
		// Where the messages received in order?
		List<Long> sortedList = new ArrayList<Long>(messagesReceived);
		Collections.sort(sortedList);
		if (sortedList.equals(messagesReceived)) System.out.println("SUCCESS: Messages were received in order!");
		else System.out.println("ERROR: Messages were received out of order!");
		
		// If we sum all batches do we get the correct number of messages?
		long sumOfAllBatches = batchesReceived.stream().mapToLong(Long::longValue).sum();
		if (sumOfAllBatches == messagesToSend) System.out.println("SUCCESS: The sum of message from the batches received is correct! => " + sumOfAllBatches);
		else System.out.println("ERROR: The sum of message from the batches received is incorrect! => " + sumOfAllBatches);
		
		System.out.println("\nMore info:\n");
		
		System.out.println("Number of batches received: " + batchesReceived.size());
		System.out.println("Batches received: " + batchesReceived.toString());
		System.out.println("Producer busy-spin count: " + producer.getBusySpinCount());
		System.out.println("Consumer busy-spin count: " + consumer.getBusySpinCount());
	}
}