package com.coralblocks.coralqueue.example.multiplexer;

import java.util.ArrayList;
import java.util.List;

import com.coralblocks.coralqueue.multiplexer.AtomicDynamicMultiplexer;

public class DynamicBasics {
	
	public static class Message {
		
		private static final int PRIME = 31;
		
		String producerName; // messages will be sent from different producers
		long value;
		boolean last;
		
		@Override
		public int hashCode() {
		    return PRIME * (PRIME + producerName.hashCode()) + (int) (value ^ (value >>> 32));
		}
		
		@Override
		public boolean equals(Object obj) {
			if (obj instanceof Message) {
				Message m = (Message) obj;
				return this.producerName.equals(m.producerName) && this.value == m.value;
			}
			return false;
		}
		
		Message copy() {
			Message m = new Message();
			m.producerName = this.producerName;
			m.value = this.value;
			m.last = this.last;
			return m;
		}
	}
	
	public static class Producer extends Thread {
		
		private static volatile int IDs = 0;
		
		private final AtomicDynamicMultiplexer<Message> mux;
		private final int messagesToSend;
		private final int batchSizeToSend;
		private int idToSend = 1;
		private long busySpinCount = 0;
		private final Producer[] extraProducers;
		private boolean extraProducersStarted = false;
		
		public Producer(AtomicDynamicMultiplexer<Message> mux, int messagesToSend, int batchSizeToSend, Producer[] extraProducers) {
			super(Producer.class.getSimpleName() + "-" + IDs++); // name of the thread
			this.mux = mux;
			this.messagesToSend = messagesToSend;
			this.batchSizeToSend = batchSizeToSend;
			this.extraProducers = extraProducers;
		}
		
		public long getBusySpinCount() {
			return busySpinCount;
		}
		
		@Override
		public final void run() {
			int messagesSent = 0;
			int remaining = messagesToSend - messagesSent;
			while(remaining > 0) {
				// For testing we start extra producers from this producer (dynamic producers being started and added)
				if (extraProducers != null && remaining < messagesToSend * 0.66 && !extraProducersStarted) {
					extraProducersStarted = true;
					for(int i = 0; i < extraProducers.length; i++) {
						extraProducers[i].start();
					}
				}
				
				int batchToSend = Math.min(batchSizeToSend, remaining);
				for(int i = 0; i < batchToSend; i++) {
					Message m;
					while((m = mux.nextToDispatch()) == null) { // <=========
						// busy spin (default and fastest wait strategy)
						busySpinCount++; // save the number of busy-spins, just for extra info later
					}
					m.producerName = getName(); // thread name
					m.value = idToSend++; // sending an unique value so the messages sent are unique
					m.last = m.value == messagesToSend; // is it the last message I'll be sending?
				}
				mux.flush(); // <=========
				remaining -= batchToSend;
			}
		}
	}
	
	public static class Consumer extends Thread {
		
		private final AtomicDynamicMultiplexer<Message> mux;
		private final List<Message> messagesReceived  = new ArrayList<Message>();
		private final List<Long> batchesReceived = new ArrayList<Long>();
		private long busySpinCount = 0;
		private int lastCount = 0;
		private final int finalNumberOfProducers;
		private final int totalMessagesToReceive;
		private final Producer[] extraProducers;
		private boolean extraProducersStarted = false;
		
		public Consumer(AtomicDynamicMultiplexer<Message> mux, int finalNumberOfProducers, int totalMessagesToReceive, Producer[] extraProducers) {
			super(Consumer.class.getSimpleName()); // name of the thread
			this.mux = mux;
			this.finalNumberOfProducers = finalNumberOfProducers;
			this.totalMessagesToReceive = totalMessagesToReceive;
			this.extraProducers = extraProducers;
		}
		
		public List<Message> getMessagesReceived() {
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
				long avail = mux.availableToPoll(); // <=========
				if (avail > 0) {
					// For testing we start extra producers from the consumer (dynamic producers being started and added)
					if (messagesReceived.size() > totalMessagesToReceive * 0.33 && !extraProducersStarted) {
						extraProducersStarted = true;
						for(int i = 0; i < extraProducers.length; i++) {
							extraProducers[i].start();
						}
					}
					
					for(long i = 0; i < avail; i++) {
						Message m = mux.poll(); // <=========
						messagesReceived.add(m.copy()); // save all messages received (don't forget to copy!!!) so we can later check them
						if (m.last && ++lastCount == finalNumberOfProducers) isRunning = false; 
					}
					mux.donePolling(); // <=========
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
		final int initialNumberOfProducers = args.length > 2 ? Integer.parseInt(args[2]) : 4;
		final int extraProducersCreatedByProducer = args.length > 3 ? Integer.parseInt(args[3]) : 2;
		final int extraProducersCreatedByConsumer = args.length > 4 ? Integer.parseInt(args[4]) : 2;
		
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
		
		System.out.println("Each of the " + finalNumberOfProducers + " producers will send "
							+ messagesToSend + " messages in batches of " + batchSizeToSend + " messages for a total of "
							+ totalMessagesToSend + " messages... \n");
		
		consumer.start();
		for(int i = 0; i < producers.length; i++) producers[i].start();
			
		for(int i = 0; i < extraProdProducer.length; i++) {
			extraProdProducer[i].join();
			System.out.println("Thread " + extraProdProducer[i].getName() + " done and exited...");
		}
		
		for(int i = 0; i < extraProdConsumer.length; i++) {
			extraProdConsumer[i].join();
			System.out.println("Thread " + extraProdConsumer[i].getName() + " done and exited...");
		}
		
		for(int i = 0; i < producers.length; i++) {
			producers[i].join();
			System.out.println("Thread " + producers[i].getName() + " done and exited...");
		}
		
		consumer.join();
		System.out.println("\nThread " + consumer.getName() + " done and exited...");
		
		System.out.println();
		
		List<Message> messagesReceived = consumer.getMessagesReceived();
		List<Long> batchesReceived = consumer.getBatchesReceived();
		
		// Did we receive all messages?
		if (messagesReceived.size() == totalMessagesToSend) System.out.println("SUCCESS: All messages received! => " + totalMessagesToSend);
		else System.out.println("ERROR: Wrong number of messages received! => " + messagesReceived.size());
		
		// Where there any duplicates?
		if (messagesReceived.stream().distinct().count() == messagesReceived.size()) System.out.println("SUCCESS: No duplicate messages were received!");
		else System.out.println("ERROR: Found duplicate messages!");
		
		// If we sum all batches do we get the correct number of messages?
		long sumOfAllBatches = batchesReceived.stream().mapToLong(Long::longValue).sum();
		if (sumOfAllBatches == totalMessagesToSend) System.out.println("SUCCESS: The sum of message from the batches received is correct! => " + totalMessagesToSend);
		else System.out.println("ERROR: The sum of message from the batches received is incorrect! => " + sumOfAllBatches);
		
		System.out.println("\nMore info:\n");
		
		for(int i = 0; i < extraProdProducer.length; i++) {
			System.out.println(extraProdProducer[i].getName() + " busy-spin count: " + extraProdProducer[i].getBusySpinCount());
		}
		for(int i = 0; i < extraProdConsumer.length; i++) {
			System.out.println(extraProdConsumer[i].getName() + " busy-spin count: " + extraProdConsumer[i].getBusySpinCount());
		}
		for(int i = 0; i < producers.length; i++) {
			System.out.println(producers[i].getName() + " busy-spin count: " + producers[i].getBusySpinCount());
		}
		System.out.println("Consumer busy-spin count: " + consumer.getBusySpinCount());
	}
}