package com.coralblocks.coralqueue.example.demultiplexer;

import java.util.ArrayList;
import java.util.List;

import com.coralblocks.coralqueue.demultiplexer.AtomicDynamicDemultiplexer;

public class DynamicBasics {
	
	public static class Message {

		long value;
		boolean last;
	}
	
	public static class Producer extends Thread {
		
		private final AtomicDynamicDemultiplexer<Message> demux;
		private final int messagesToSend;
		private final int batchSizeToSend;
		private int idToSend = 1;
		private long busySpinCount = 0;
		private final Consumer[] extraConsumers;
		private boolean extraConsumersStarted = false;
		private final int finalNumberOfConsumers;
		
		public Producer(AtomicDynamicDemultiplexer<Message> demux, int messagesToSend, int batchSizeToSend, Consumer[] extraConsumers, int finalNumberOfConsumers) {
			super(Producer.class.getSimpleName()); // name of the thread
			this.demux = demux;
			this.messagesToSend = messagesToSend;
			this.batchSizeToSend = batchSizeToSend;
			this.extraConsumers = extraConsumers;
			this.finalNumberOfConsumers = finalNumberOfConsumers;
		}
		
		public long getBusySpinCount() {
			return busySpinCount;
		}
		
		@Override
		public final void run() {
			int remaining = messagesToSend - finalNumberOfConsumers; // last message to each consumer will be sent later
			while(remaining > 0) {
				
//				// For testing we start extra consumers from this producer (dynamic consumers being started and added)
//				if (remaining < messagesToSend * 0.66 && !extraConsumersStarted) {
//					extraConsumersStarted = true;
//					for(int i = 0; i < extraConsumers.length; i++) {
//						extraConsumers[i].start();
//					}
//				}
				
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
			
			System.out.println("----> Sending last from producer!");
			
			// Now send one last message to each consumer (notice nextToDispatch below takes the consumerIndex)
			for(int i = 0; i < finalNumberOfConsumers; i++) {
				Message m;
				while((m = demux.nextToDispatch(i)) == null) { // <========= here we direct the message to a specific consumer
					// busy spin (default and fastest wait strategy)
					busySpinCount++; // save the number of busy-spins, just for extra info later
				}
				System.out.println("---- SENT: " + i);
				m.value = idToSend++; // sending an unique value so the messages sent are unique
				m.last = true; // last message
				demux.flush(); 
			}
			//demux.flush(); // <=========
		}
	}
	
	public static class Consumer extends Thread {
		
		private static volatile int IDs = 0;
		
		private final AtomicDynamicDemultiplexer<Message> demux;
		private final List<Long> messagesReceived  = new ArrayList<Long>();
		private final List<Long> batchesReceived = new ArrayList<Long>();
		private long busySpinCount = 0;
		private final int totalMessagesToReceive;
		private final Consumer[] extraConsumers;
		private boolean extraConsumersStarted = false;
		
		public Consumer(AtomicDynamicDemultiplexer<Message> demux, int totalMessagesToReceive, Consumer[] extraConsumers) {
			super(Consumer.class.getSimpleName() + "-" + IDs++); // name of the thread
			this.demux = demux;
			this.totalMessagesToReceive = totalMessagesToReceive;
			this.extraConsumers = extraConsumers;
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
				long avail = demux.availableToPoll(); // <=========
				if (avail > 0) {
					
//					// For testing we start extra producers from the consumer (dynamic producers being started and added)
//					if (extraConsumers != null && messagesReceived.size() > totalMessagesToReceive * 0.33 && !extraConsumersStarted) {
//						extraConsumersStarted = true;
//						for(int i = 0; i < extraConsumers.length; i++) {
//							extraConsumers[i].start();
//						}
//					}
					
					for(long i = 0; i < avail; i++) {
						Message m = demux.poll(); // <=========
						messagesReceived.add(m.value); // save all messages received so we can later check them
						if (m.last) isRunning = false; // wait to receive the done signal from the producer
					}
					demux.donePolling(); // <=========
					batchesReceived.add(avail); // save the batch sizes received, just so we can double check
					System.out.println("-----> messagesReceived: " + messagesReceived.size() + " in " + getName());
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
		final int initialNumberOfConsumers = args.length > 2 ? Integer.parseInt(args[2]) : 4;
		final int extraConsumersCreatedByProducer = args.length > 3 ? Integer.parseInt(args[3]) : 2;
		final int extraConsumersCreatedByConsumer = args.length > 4 ? Integer.parseInt(args[4]) : 2;
		
		final int finalNumberOfConsumers = initialNumberOfConsumers + extraConsumersCreatedByProducer + extraConsumersCreatedByConsumer;
		
		AtomicDynamicDemultiplexer<Message> mux = new AtomicDynamicDemultiplexer<Message>(Message.class, initialNumberOfConsumers);
		
//		Consumer[] extraConProducer = new Consumer[extraConsumersCreatedByProducer];
//		for(int i = 0; i < extraConProducer.length; i++) {
//			extraConProducer[i] = new Consumer(mux, messagesToSend, null);
//		}
//		
//		Consumer[] extraConConsumer = new Consumer[extraConsumersCreatedByConsumer];
//		for(int i = 0; i < extraConConsumer.length; i++) {
//			extraConConsumer[i] = new Consumer(mux, messagesToSend, null);
//		}
		
		Consumer[] consumers = new Consumer[initialNumberOfConsumers];
		for(int i = 0; i < consumers.length; i++) {
			consumers[i] = new Consumer(mux, messagesToSend, null);
		}
		
		Producer producer = new Producer(mux, messagesToSend, batchSizeToSend, null, finalNumberOfConsumers);
		
		System.out.println("Producer will send " + messagesToSend + " messages in batches of " + batchSizeToSend + " messages"
							+ " to " + finalNumberOfConsumers + " consumers...\n");
		
		for(int i = 0; i < consumers.length; i++) {
			consumers[i].start();
		}
		
		producer.start();
		
		producer.join();
		System.out.println("\nThread " + producer.getName() + " done and exited...");
			
		for(int i = 0; i < consumers.length; i++) {
			consumers[i].join();
			System.out.println("Thread " + consumers[i].getName() + " done and exited...");
		}
		
//		for(int i = 0; i < extraConProducer.length; i++) {
//			extraConProducer[i].join();
//			System.out.println("Thread " + extraConProducer[i].getName() + " done and exited...");
//		}
//		
//		for(int i = 0; i < extraConConsumer.length; i++) {
//			extraConConsumer[i].join();
//			System.out.println("Thread " + extraConConsumer[i].getName() + " done and exited...");
//		}
		
		System.out.println();
		
		List<Long> totalMessagesReceived = new ArrayList<Long>(messagesToSend);
		for(int i = 0; i < consumers.length; i++) {
			totalMessagesReceived.addAll(consumers[i].getMessagesReceived());
		}
//		for(int i = 0; i < extraConProducer.length; i++) {
//			totalMessagesReceived.addAll(extraConProducer[i].getMessagesReceived());
//		}
//		for(int i = 0; i < extraConConsumer.length; i++) {
//			totalMessagesReceived.addAll(extraConConsumer[i].getMessagesReceived());
//		}
		
		List<Long> totalBatchesReceived = new ArrayList<Long>(messagesToSend);
		for(int i = 0; i < consumers.length; i++) {
			totalBatchesReceived.addAll(consumers[i].getBatchesReceived());
		}
//		for(int i = 0; i < extraConProducer.length; i++) {
//			totalBatchesReceived.addAll(extraConProducer[i].getBatchesReceived());
//		}
//		for(int i = 0; i < extraConConsumer.length; i++) {
//			totalBatchesReceived.addAll(extraConConsumer[i].getBatchesReceived());
//		}
		
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
		
		for(int i = 0; i < consumers.length; i++) {
			System.out.println(consumers[i].getName() + " messages received: " + consumers[i].getMessagesReceived().size());
		}
//		for(int i = 0; i < extraConProducer.length; i++) {
//			System.out.println(extraConProducer[i].getName() + " messages received: " + extraConProducer[i].getMessagesReceived().size());
//		}
//		for(int i = 0; i < extraConConsumer.length; i++) {
//			System.out.println(extraConConsumer[i].getName() + " messages received: " + extraConConsumer[i].getMessagesReceived().size());
//		}
		
		System.out.println();
		
		for(int i = 0; i < consumers.length; i++) {
			System.out.println(consumers[i].getName() + " number of batches received: " + consumers[i].getBatchesReceived().size());
			System.out.println(consumers[i].getName() + " batches received: " + consumers[i].getBatchesReceived().toString());
		}
//		for(int i = 0; i < extraConProducer.length; i++) {
//			System.out.println(extraConProducer[i].getName() + " number of batches received: " + extraConProducer[i].getBatchesReceived().size());
//			System.out.println(extraConProducer[i].getName() + " batches received: " + extraConProducer[i].getBatchesReceived().toString());
//		}
//		for(int i = 0; i < extraConConsumer.length; i++) {
//			System.out.println(extraConConsumer[i].getName() + " number of batches received: " + extraConConsumer[i].getBatchesReceived().size());
//			System.out.println(extraConConsumer[i].getName() + " batches received: " + extraConConsumer[i].getBatchesReceived().toString());
//		}
		
		System.out.println();
		
		System.out.println("Producer busy-spin count: " + producer.getBusySpinCount());
		
		System.out.println();
		
		for(int i = 0; i < consumers.length; i++) {
			System.out.println(consumers[i].getName() + " busy-spin count: " + consumers[i].getBusySpinCount());
		}
//		for(int i = 0; i < extraConProducer.length; i++) {
//			System.out.println(extraConProducer[i].getName() + " busy-spin count: " + extraConProducer[i].getBusySpinCount());
//		}
//		for(int i = 0; i < extraConConsumer.length; i++) {
//			System.out.println(extraConConsumer[i].getName() + " busy-spin count: " + extraConConsumer[i].getBusySpinCount());
//		}
	}
}