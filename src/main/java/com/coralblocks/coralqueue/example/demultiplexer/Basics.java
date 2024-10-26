package com.coralblocks.coralqueue.example.demultiplexer;

import java.util.ArrayList;
import java.util.List;

import com.coralblocks.coralqueue.demultiplexer.AtomicDemultiplexer;

public class Basics {
	
	public static class Message {

		long value;
		boolean last;
	}
	
	public static class Producer extends Thread {
		
		private final AtomicDemultiplexer<Message> demux;
		private final int messagesToSend;
		private final int batchSizeToSend;
		private int idToSend = 1;
		private long busySpinCount = 0;
		
		public Producer(AtomicDemultiplexer<Message> demux, int messagesToSend, int batchSizeToSend) {
			super(Producer.class.getSimpleName()); // name of the thread
			this.demux = demux;
			this.messagesToSend = messagesToSend;
			this.batchSizeToSend = batchSizeToSend;
		}
		
		public long getBusySpinCount() {
			return busySpinCount;
		}
		
		@Override
		public final void run() {
			final int numberOfConsumers = demux.getNumberOfConsumers();
			int remaining = messagesToSend - numberOfConsumers; // last message to each consumer will be sent later
			while(remaining > 0) {
				int batchToSend = Math.min(batchSizeToSend, remaining);
				for(int i = 0; i < batchToSend; i++) {
					Message m;
					while((m = demux.nextToDispatch()) == null) { // <=========
						// busy spin (default and fastest wait strategy)
						busySpinCount++; // save the number of busy-spins, just for extra info later
					}
					m.value = idToSend++; // sending an unique value so the messages sent are unique
					m.last = false;
				}
				demux.flush(); // <=========
				remaining -= batchToSend;
			}
			// Now send one last message to each consumer (notice nextToDispatch below takes the consumerIndex)
			for(int i = 0; i < numberOfConsumers; i++) {
				Message m;
				while((m = demux.nextToDispatch(i)) == null) { // <========= here we direct the message to a specific consumer
					// busy spin (default and fastest wait strategy)
					busySpinCount++; // save the number of busy-spins, just for extra info later
				}
				m.value = idToSend++; // sending an unique value so the messages sent are unique
				m.last = true; // last message
			}
			demux.flush(); // <=========
		}
	}
	
	public static class Consumer extends Thread {
		
		private final AtomicDemultiplexer<Message> demux;
		private final List<Long> messagesReceived  = new ArrayList<Long>();
		private final List<Long> batchesReceived = new ArrayList<Long>();
		private long busySpinCount = 0;
		private final int consumerIndex;
		
		public Consumer(AtomicDemultiplexer<Message> demux, int consumerIndex) {
			super(Consumer.class.getSimpleName() + "-" + consumerIndex); // name of the thread
			this.demux = demux;
			this.consumerIndex = consumerIndex;
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
				long avail = demux.availableToPoll(consumerIndex); // <=========
				if (avail > 0) {
					for(long i = 0; i < avail; i++) {
						Message m = demux.poll(consumerIndex); // <=========
						messagesReceived.add(m.value); // save all messages received so we can later check them
						if (m.last) isRunning = false; // wait to receive the done signal from the producer
					}
					demux.donePolling(consumerIndex); // <=========
					batchesReceived.add(avail); // save the batch sizes received, just so we can double check
				} else {
					// busy spin (default and fastest wait strategy)
					busySpinCount++; // save the number of busy-spins, just for extra info later
				}
			}
		}
	}
	
	public static void main(String[] args) throws InterruptedException {
		
		final int messagesToSend = args.length > 0 ? Integer.parseInt(args[0]) : 10000;
		final int batchSizeToSend = args.length > 1 ? Integer.parseInt(args[1]) : 100;
		final int numberOfConsumers = args.length > 2 ? Integer.parseInt(args[2]) : 4;
		
		AtomicDemultiplexer<Message> mux = new AtomicDemultiplexer<Message>(Message.class, numberOfConsumers);
		
		Producer producer = new Producer(mux, messagesToSend, batchSizeToSend);
		
		Consumer[] consumers = new Consumer[numberOfConsumers];
		for(int i = 0; i < consumers.length; i++) {
			consumers[i] = new Consumer(mux, i);
		}
		
		System.out.println("Producer will send " + messagesToSend + " messages in batches of " + batchSizeToSend + " messages"
							+ " to " + numberOfConsumers + " consumers...\n");
		
		for(int i = 0; i < consumers.length; i++) {
			consumers[i].start();
		}
		producer.start();
			
		for(int i = 0; i < consumers.length; i++) {
			consumers[i].join();
			System.out.println("Thread " + consumers[i].getName() + " done and exited...");
		}
		
		producer.join();
		System.out.println("\nThread " + producer.getName() + " done and exited...");
		
		System.out.println();
		
		List<Long> totalMessagesReceived = new ArrayList<Long>(messagesToSend * numberOfConsumers);
		for(int i = 0; i < consumers.length; i++) {
			totalMessagesReceived.addAll(consumers[i].getMessagesReceived());
		}
		
		List<Long> totalBatchesReceived = new ArrayList<Long>(messagesToSend * numberOfConsumers);
		for(int i = 0; i < consumers.length; i++) {
			totalBatchesReceived.addAll(consumers[i].getBatchesReceived());
		}
		
		// Did we receive all messages?
		if (totalMessagesReceived.size() == messagesToSend) System.out.println("SUCCESS: All messages received! => " + messagesToSend);
		else System.out.println("ERROR: Wrong number of messages received! => " +totalMessagesReceived.size());
		
		// Where there any duplicates?
		if (totalMessagesReceived.stream().distinct().count() == totalMessagesReceived.size()) System.out.println("SUCCESS: No duplicate messages were received!");
		else System.out.println("ERROR: Found duplicate messages!");
		
		// If we sum all batches do we get the correct number of messages?
		long sumOfAllBatches = totalBatchesReceived.stream().mapToLong(Long::longValue).sum();
		if (sumOfAllBatches == messagesToSend) System.out.println("SUCCESS: The sum of message from the batches received is correct! => " + messagesToSend);
		else System.out.println("ERROR: The sum of message from the batches received is incorrect! => " + sumOfAllBatches);
		
		System.out.println("\nMore info:\n");
		
		System.out.println("Number of total batches received from all consumers: " + totalBatchesReceived.size());
		
		System.out.println();
		
		for(int i = 0; i < numberOfConsumers; i++) {
			System.out.println("Consumer " + i + " messages received: " + consumers[i].getMessagesReceived().size());
		}
		
		System.out.println();
		
		for(int i = 0; i < numberOfConsumers; i++) {
			System.out.println("Consumer " + i + " number of batches received: " + consumers[i].getBatchesReceived().size());
			System.out.println("Consumer " + i + " batches received: " + consumers[i].getBatchesReceived().toString());
			System.out.println();
		}
		
		System.out.println("Producer busy-spin count: " + producer.getBusySpinCount());
		
		System.out.println();
		
		for(int i = 0; i < numberOfConsumers; i++) {
			System.out.println("Consumer " + i + " busy-spin count: " + consumers[i].getBusySpinCount());
		}
	}
}