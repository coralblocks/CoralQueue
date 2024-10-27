/* 
 * Copyright 2024 (c) CoralBlocks - http://www.coralblocks.com
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
package com.coralblocks.coralqueue.example.multiplexer;

import java.util.ArrayList;
import java.util.List;

import com.coralblocks.coralqueue.multiplexer.AtomicMultiplexer;

public class Basics {
	
	public static class Message {
		
		private static final int PRIME = 31;
		
		int producerIndex; // messages will be sent from different producers
		long value;
		boolean last;
		
		@Override
		public int hashCode() {
		    return PRIME * (PRIME + producerIndex) + (int) (value ^ (value >>> 32));
		}
		
		@Override
		public boolean equals(Object obj) {
			if (obj instanceof Message) {
				Message m = (Message) obj;
				return this.producerIndex == m.producerIndex && this.value == m.value;
			}
			return false;
		}
		
		Message copy() {
			Message m = new Message();
			m.producerIndex = this.producerIndex;
			m.value = this.value;
			m.last = this.last;
			return m;
		}
	}
	
	public static class Producer extends Thread {
		
		private final AtomicMultiplexer<Message> mux;
		private final int messagesToSend;
		private final int batchSizeToSend;
		private int idToSend = 1;
		private long busySpinCount = 0;
		private final int producerIndex;
		
		public Producer(AtomicMultiplexer<Message> mux, int producerIndex, int messagesToSend, int batchSizeToSend) {
			super(Producer.class.getSimpleName() + "-" + producerIndex); // name of the thread
			this.mux = mux;
			this.producerIndex = producerIndex;
			this.messagesToSend = messagesToSend;
			this.batchSizeToSend = batchSizeToSend;
		}
		
		public long getBusySpinCount() {
			return busySpinCount;
		}
		
		@Override
		public final void run() {
			int remaining = messagesToSend;
			while(remaining > 0) {
				int batchToSend = Math.min(batchSizeToSend, remaining);
				for(int i = 0; i < batchToSend; i++) {
					Message m;
					while((m = mux.nextToDispatch(producerIndex)) == null) { // <=========
						// busy spin while blocking (default and fastest wait strategy)
						busySpinCount++; // save the number of busy-spins, just for extra info later
					}
					m.producerIndex = producerIndex;
					m.value = idToSend++; // sending an unique value so the messages sent are unique
					m.last = m.value == messagesToSend; // is it the last message I'll be sending?
				}
				mux.flush(producerIndex); // <=========
				remaining -= batchToSend;
			}
		}
	}
	
	public static class Consumer extends Thread {
		
		private final AtomicMultiplexer<Message> mux;
		private final List<Message> messagesReceived  = new ArrayList<Message>();
		private final List<Long> batchesReceived = new ArrayList<Long>();
		private long busySpinCount = 0;
		private int lastCount = 0;
		
		public Consumer(AtomicMultiplexer<Message> mux) {
			super(Consumer.class.getSimpleName()); // name of the thread
			this.mux = mux;
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
					for(long i = 0; i < avail; i++) {
						Message m = mux.poll(); // <=========
						messagesReceived.add(m.copy()); // save all messages received (don't forget to copy!!!) so we can later check them
						if (m.last && ++lastCount == mux.getNumberOfProducers()) isRunning = false; // wait to receive the done signal from ALL producers
					}
					mux.donePolling(); // <=========
					batchesReceived.add(avail); // save the batch sizes received, just so we can double check
				} else {
					// busy spin while blocking (default and fastest wait strategy)
					busySpinCount++; // save the number of busy-spins, just for extra info later
				}
			}
		}
	}
	
	public static void main(String[] args) throws InterruptedException {
		
		final int messagesToSend = args.length > 0 ? Integer.parseInt(args[0]) : 10000;
		final int batchSizeToSend = args.length > 1 ? Integer.parseInt(args[1]) : 100;
		final int numberOfProducers = args.length > 2 ? Integer.parseInt(args[2]) : 4;
		
		final int totalMessagesToSend = messagesToSend * numberOfProducers;
		
		AtomicMultiplexer<Message> mux = new AtomicMultiplexer<Message>(Message.class, numberOfProducers);
		
		Producer[] producers = new Producer[numberOfProducers];
		for(int i = 0; i < producers.length; i++) {
			producers[i] = new Producer(mux, i, messagesToSend, batchSizeToSend);
		}
		
		Consumer consumer = new Consumer(mux);
		
		System.out.println("Each of the " + numberOfProducers + " producers will send "
							+ messagesToSend + " messages in batches of " + batchSizeToSend + " messages for a total of "
							+ totalMessagesToSend + " messages... \n");
		
		consumer.start();
		for(int i = 0; i < producers.length; i++) producers[i].start();
			
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
		
		System.out.println("Number of batches received: " + batchesReceived.size());
		System.out.println("Batches received: " + batchesReceived.toString());
		for(int i = 0; i < numberOfProducers; i++) {
			System.out.println("Producer " + i + " busy-spin count: " + producers[i].getBusySpinCount());
		}
		System.out.println("Consumer busy-spin count: " + consumer.getBusySpinCount());
	}
}