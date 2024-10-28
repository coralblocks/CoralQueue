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
package com.coralblocks.coralqueue.example.mpmc;

import java.util.ArrayList;
import java.util.List;

import com.coralblocks.coralqueue.mpmc.AtomicMpMc;

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
		
		private final AtomicMpMc<Message> mpmc;
		private final int messagesToSend;
		private final int batchSizeToSend;
		private int idToSend = 1;
		private long busySpinCount = 0;
		private final int producerIndex;
		private final int numberOfConsumers;
		
		public Producer(AtomicMpMc<Message> mpmc, int producerIndex, int messagesToSend, int batchSizeToSend, int numberOfConsumers) {
			super(Producer.class.getSimpleName() + "-" + producerIndex); // name of the thread
			this.mpmc = mpmc;
			this.producerIndex = producerIndex;
			this.messagesToSend = messagesToSend;
			this.batchSizeToSend = batchSizeToSend;
			this.numberOfConsumers = numberOfConsumers;
		}
		
		public long getBusySpinCount() {
			return busySpinCount;
		}
		
		@Override
		public final void run() {
			int remaining = messagesToSend - numberOfConsumers;
			while(remaining > 0) {
				int batchToSend = Math.min(batchSizeToSend, remaining);
				for(int i = 0; i < batchToSend; i++) {
					Message m;
					while((m = mpmc.nextToDispatch(producerIndex)) == null) { // <=========
						// busy spin while blocking (default and fastest wait strategy)
						busySpinCount++; // save the number of busy-spins, just for extra info later
					}
					m.producerIndex = producerIndex;
					m.value = idToSend++; // sending an unique value so the messages sent are unique
					m.last = false; // is it the last message I'll be sending?
				}
				mpmc.flush(producerIndex); // <=========
				remaining -= batchToSend;
			}
			
			// now send the last message to each consumer
			for(int i = 0; i < numberOfConsumers; i++) {
				Message m;
				while((m = mpmc.nextToDispatch(producerIndex, i)) == null) { // <========= directed to a specific consumer
					// busy spin while blocking (default and fastest wait strategy)
					busySpinCount++; // save the number of busy-spins, just for extra info later
				}
				m.producerIndex = producerIndex;
				m.value = idToSend++; // sending an unique value so the messages sent are unique
				m.last = true; // is it the last message I'll be sending?
			}
			mpmc.flush(producerIndex); // <=========
		}
	}
	
	public static class Consumer extends Thread {
		
		private final AtomicMpMc<Message> mpmc;
		private final List<Message> messagesReceived  = new ArrayList<Message>();
		private final List<Long> batchesReceived = new ArrayList<Long>();
		private long busySpinCount = 0;
		private int lastCount = 0;
		private final int consumerIndex;
		
		public Consumer(AtomicMpMc<Message> mpmc, int consumerIndex) {
			super(Consumer.class.getSimpleName() + "-" + consumerIndex); // name of the thread
			this.mpmc = mpmc;
			this.consumerIndex = consumerIndex;
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
				long avail = mpmc.availableToPoll(consumerIndex); // <=========
				if (avail > 0) {
					for(long i = 0; i < avail; i++) {
						Message m = mpmc.poll(consumerIndex); // <=========
						messagesReceived.add(m.copy()); // save all messages received (don't forget to copy!!!) so we can later check them
						if (m.last && ++lastCount == mpmc.getNumberOfProducers()) isRunning = false; // wait to receive the done signal from ALL producers
					}
					mpmc.donePolling(consumerIndex); // <=========
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
		final int numberOfConsumers = args.length > 3 ? Integer.parseInt(args[3]) : 4;
		
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
		
		System.out.println("Each of the " + numberOfProducers + " producers will send "
							+ messagesToSend + " messages in batches of " + batchSizeToSend + " messages to "
							+ numberOfConsumers + " consumers"
							+ " for a total of " + totalMessagesToSend + " messages... \n");
		
		
		for(int i = 0; i < consumers.length; i++) consumers[i].start();
		for(int i = 0; i < producers.length; i++) producers[i].start();
			
		for(int i = 0; i < producers.length; i++) {
			producers[i].join();
			System.out.println("Thread " + producers[i].getName() + " done and exited...");
		}
		
		System.out.println();
		
		for(int i = 0; i < consumers.length; i++) {
			consumers[i].join();
			System.out.println("Thread " + consumers[i].getName() + " done and exited...");
		}
		
		System.out.println();
		
		List<Message> messagesReceived = new ArrayList<Message>(totalMessagesToSend);
		for(int i = 0; i < consumers.length; i++) {
			messagesReceived.addAll(consumers[i].getMessagesReceived());
		}
		
		List<Long> batchesReceived = new ArrayList<Long>(totalMessagesToSend);
		for(int i = 0; i < consumers.length; i++) {
			batchesReceived.addAll(consumers[i].getBatchesReceived());
		}
		
		// Did we receive all messages?
		if (messagesReceived.size() == totalMessagesToSend) System.out.println("SUCCESS: All messages received! => " + totalMessagesToSend);
		else System.out.println("ERROR: Wrong number of messages received! => " + messagesReceived.size());
		
		// Were there any duplicates?
		if (messagesReceived.stream().distinct().count() == messagesReceived.size()) System.out.println("SUCCESS: No duplicate messages were received!");
		else System.out.println("ERROR: Found duplicate messages!");
		
		// If we sum all batches received do we get the correct number of messages?
		long sumOfAllBatches = batchesReceived.stream().mapToLong(Long::longValue).sum();
		if (sumOfAllBatches == totalMessagesToSend) System.out.println("SUCCESS: The sum of messages from the batches received is correct! => " + totalMessagesToSend);
		else System.out.println("ERROR: The sum of messages from the batches received is incorrect! => " + sumOfAllBatches);
		
		System.out.println("\nMore info:\n");
		
		for(int i = 0; i < producers.length; i++) {
			System.out.println(producers[i].getName() + " busy-spin count: " + producers[i].getBusySpinCount());
		}
		
		System.out.println();
		
		for(int i = 0; i < consumers.length; i++) {
			System.out.println(consumers[i].getName() + " number of batches received: " + consumers[i].getBatchesReceived().size());
			System.out.println(consumers[i].getName() + " batches received: " + consumers[i].getBatchesReceived().toString());
			System.out.println(consumers[i].getName() + " busy-spin count: " + consumers[i].getBusySpinCount());
			System.out.println();
		}
	}
}