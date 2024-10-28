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
package com.coralblocks.coralqueue.example.broadcaster;

import java.util.ArrayList;
import java.util.List;

import com.coralblocks.coralqueue.broadcaster.AtomicBroadcaster;

public class Basics {
	
	public static class Message {

		long value;
		boolean last;
	}
	
	public static class Producer extends Thread {
		
		private final AtomicBroadcaster<Message> broadcaster;
		private final int messagesToSend;
		private final int batchSizeToSend;
		private int idToSend = 1;
		private long busySpinCount = 0;
		
		public Producer(AtomicBroadcaster<Message> broadcaster, int messagesToSend, int batchSizeToSend) {
			super(Producer.class.getSimpleName()); // name of the thread
			this.broadcaster = broadcaster;
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
					while((m = broadcaster.nextToDispatch()) == null) { // <=========
						// busy spin while blocking (default and fastest wait strategy)
						busySpinCount++; // save the number of busy-spins, just for extra info later
					}
					m.value = idToSend++; // sending an unique value so the messages sent are unique
					m.last = m.value == messagesToSend; // is it the last message I'll be sending?
				}
				broadcaster.flush(); // <=========
				remaining -= batchToSend;
			}
		}
	}
	
	public static class Consumer extends Thread {
		
		private final AtomicBroadcaster<Message> broadcaster;
		private final List<Long> messagesReceived  = new ArrayList<Long>();
		private final List<Long> batchesReceived = new ArrayList<Long>();
		private long busySpinCount = 0;
		private final int consumerIndex;
		
		public Consumer(AtomicBroadcaster<Message> broadcaster, int consumerIndex) {
			super(Consumer.class.getSimpleName() + "-" + consumerIndex); // name of the thread
			this.broadcaster = broadcaster;
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
				long avail = broadcaster.availableToPoll(consumerIndex); // <=========
				if (avail > 0) {
					for(long i = 0; i < avail; i++) {
						Message m = broadcaster.poll(consumerIndex); // <=========
						messagesReceived.add(m.value); // save all messages received so we can later check them
						if (m.last) isRunning = false; // wait to receive the done signal from the producer
					}
					broadcaster.donePolling(consumerIndex); // <=========
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
		final int numberOfConsumers = args.length > 2 ? Integer.parseInt(args[2]) : 4;
		
		AtomicBroadcaster<Message> broadcaster = new AtomicBroadcaster<Message>(Message.class, numberOfConsumers);
		
		Producer producer = new Producer(broadcaster, messagesToSend, batchSizeToSend);
		
		Consumer[] consumers = new Consumer[numberOfConsumers];
		for(int i = 0; i < consumers.length; i++) {
			consumers[i] = new Consumer(broadcaster, i);
		}
		
		System.out.println("Producer will broadcast " + messagesToSend + " messages in batches of " + batchSizeToSend + " messages"
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
		
		// Did all consumers receive all messages?
		for(int i = 0; i < consumers.length; i++) {
			if (consumers[i].getMessagesReceived().size() == messagesToSend) System.out.println("SUCCESS: " + consumers[i].getName() + " received all messages! => " + messagesToSend);
			else System.out.println("ERROR: Wrong number of messages received! => " + consumers[i].getMessagesReceived().size());
		}
		
		System.out.println();
		
		// Were there any duplicates?
		for(int i = 0; i < consumers.length; i++) {
			if (consumers[i].getMessagesReceived().stream().distinct().count() == consumers[i].getMessagesReceived().size()) System.out.println("SUCCESS: " + consumers[i].getName() + " received no duplicate messages!");
			else System.out.println("ERROR: Found duplicate messages!");
		}
		
		System.out.println();
		
		// If we sum all batches received do we get the correct number of messages?
		for(int i = 0; i < consumers.length; i++) {
			long sumOfAllBatches = consumers[i].getBatchesReceived().stream().mapToLong(Long::longValue).sum();
			if (sumOfAllBatches == messagesToSend) System.out.println("SUCCESS: " + consumers[i].getName() + " => The sum of messages from the batches received is correct! => " + messagesToSend);
			else System.out.println("ERROR: " + consumers[i].getName() + " => The sum of messages from the batches received is incorrect! => " + sumOfAllBatches);
		}
		
		System.out.println();
		
		for(int i = 0; i < consumers.length; i++) {
			System.out.println(consumers[i].getName() + " number of batches received: " + consumers[i].getBatchesReceived().size());
			System.out.println(consumers[i].getName() + " batches received: " + consumers[i].getBatchesReceived().toString());
			System.out.println();
		}
		
		System.out.println("Producer busy-spin count: " + producer.getBusySpinCount());
		
		System.out.println();
		
		for(int i = 0; i < consumers.length; i++) {
			System.out.println(consumers[i].getName() + " busy-spin count: " + consumers[i].getBusySpinCount());
		}
	}
}