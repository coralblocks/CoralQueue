/* 
 * Copyright 2015-2024 (c) CoralBlocks LLC - http://www.coralblocks.com
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
package com.coralblocks.coralqueue.example.mpmcbroadcaster;

import java.util.ArrayList;
import java.util.List;

import com.coralblocks.coralqueue.mpmcbroadcaster.AtomicMpMcBroadcaster;
import com.coralblocks.coralqueue.mpmcbroadcaster.MpMcBroadcaster;

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
		
		private final MpMcBroadcaster<Message> mpmcBroadcaster;
		private final int messagesToSend;
		private final int batchSizeToSend;
		private int idToSend = 1;
		private long busySpinCount = 0;
		private final int producerIndex;
		
		public Producer(MpMcBroadcaster<Message> mpmcBroadcaster, int producerIndex, int messagesToSend, int batchSizeToSend) {
			super(Producer.class.getSimpleName() + "-" + producerIndex); // name of the thread
			this.mpmcBroadcaster = mpmcBroadcaster;
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
					while((m = mpmcBroadcaster.nextToDispatch(producerIndex)) == null) { // <=========
						// busy spin while waiting (default and fastest wait strategy)
						busySpinCount++; // save the number of busy-spins, just for extra info later
					}
					m.producerIndex = producerIndex;
					m.value = idToSend++; // sending an unique value so the messages sent are unique
					m.last = m.value == messagesToSend; // is it the last message I'll be sending?
				}
				mpmcBroadcaster.flush(producerIndex); // <=========
				remaining -= batchToSend;
			}
		}
	}
	
	public static class Consumer extends Thread {
		
		private final MpMcBroadcaster<Message> mpmcBroadcaster;
		private final List<Message> messagesReceived  = new ArrayList<Message>();
		private final List<Long> batchesReceived = new ArrayList<Long>();
		private long busySpinCount = 0;
		private final int consumerIndex;
		private int lastCount = 0;
		
		public Consumer(MpMcBroadcaster<Message> mpmcBroadcaster, int consumerIndex) {
			super(Consumer.class.getSimpleName() + "-" + consumerIndex); // name of the thread
			this.mpmcBroadcaster = mpmcBroadcaster;
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
				long avail = mpmcBroadcaster.availableToFetch(consumerIndex); // <=========
				if (avail > 0) {
					for(long i = 0; i < avail; i++) {
						Message m = mpmcBroadcaster.fetch(consumerIndex); // <=========
						messagesReceived.add(m.copy()); // save all messages received so we can later check them
						if (m.last && ++lastCount == mpmcBroadcaster.getNumberOfProducers()) isRunning = false; // wait to receive the done signal
					}
					mpmcBroadcaster.doneFetching(consumerIndex); // <=========
					batchesReceived.add(avail); // save the batch sizes received, just so we can double check
				} else {
					// busy spin while waiting (default and fastest wait strategy)
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
		
		MpMcBroadcaster<Message> mpmcBroadcaster = new AtomicMpMcBroadcaster<Message>(Message.class, numberOfProducers, numberOfConsumers);
		
		Producer[] producers = new Producer[numberOfProducers];
		for(int i = 0; i < producers.length; i++) {
			producers[i] = new Producer(mpmcBroadcaster, i, messagesToSend, batchSizeToSend);
		}
		
		Consumer[] consumers = new Consumer[numberOfConsumers];
		for(int i = 0; i < consumers.length; i++) {
			consumers[i] = new Consumer(mpmcBroadcaster, i);
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
		
		// Did we receive all messages?
		for(int i = 0; i < consumers.length; i++) {
			if (consumers[i].getMessagesReceived().size() == totalMessagesToSend) System.out.println("SUCCESS: " + consumers[i].getName() + " => All messages received! => " + totalMessagesToSend);
			else System.out.println("ERROR: " + consumers[i].getName() + " => Wrong number of messages received! => " + consumers[i].getMessagesReceived().size());
		}
		
		System.out.println();
		
		// Were there any duplicates?
		for(int i = 0; i < consumers.length; i++) {
			if (consumers[i].getMessagesReceived().stream().distinct().count() == consumers[i].getMessagesReceived().size()) System.out.println("SUCCESS: " + consumers[i].getName() + " => No duplicate messages were received!");
			else System.out.println("ERROR: " + consumers[i].getName() + " => Found duplicate messages!");
		}
		
		System.out.println();
		
		// If we sum all batches received do we get the correct number of messages?
		for(int i = 0; i < consumers.length; i++) {
			long sumOfAllBatches = consumers[i].getBatchesReceived().stream().mapToLong(Long::longValue).sum();
			if (sumOfAllBatches == totalMessagesToSend) System.out.println("SUCCESS: " + consumers[i].getName() + " => The sum of messages from the batches received is correct! => " + totalMessagesToSend);
			else System.out.println("ERROR: "  + consumers[i].getName() +  " => The sum of messages from the batches received is incorrect! => " + sumOfAllBatches);
		}
		
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