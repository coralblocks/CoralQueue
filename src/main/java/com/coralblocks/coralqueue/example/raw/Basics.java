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
package com.coralblocks.coralqueue.example.raw;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.coralblocks.coralqueue.raw.ByteBufferRawQueue;
import com.coralblocks.coralqueue.raw.RawBytes;
import com.coralblocks.coralqueue.raw.RawQueue;

public class Basics {
	
	public static final int MSG_SIZE = 8 /* long */ + 1 /* boolean */;
	
	public static class Producer extends Thread {
		
		private final RawQueue queue;
		private final int messagesToSend;
		private final int batchSizeToSend;
		private int idToSend = 1; // each message from this producer will contain an unique value (id)
		private long busySpinCount = 0;
		
		public Producer(RawQueue queue, int messagesToSend, int batchSizeToSend) {
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
			
			int remaining = messagesToSend;
			
			while(remaining > 0) {
				
				int batchToSend = Math.min(batchSizeToSend, remaining);
				
				while(queue.availableToWrite() < MSG_SIZE * batchToSend) { // <=========
					// busy spin while waiting (default and fastest wait strategy)
					busySpinCount++;
				}
				
				RawBytes rawBytes = queue.getProducer();
				
				for(int i = 0; i < batchToSend; i++) {
					int number = idToSend++; // sending an unique value so the messages sent are unique
					boolean last = number == messagesToSend; // is it the last message I'll be sending?
					rawBytes.putLong(number);
					rawBytes.putByte(last ? (byte) 1 : (byte) 0);
				}
				
				queue.flush(); // <=========
				remaining -= batchToSend;
			}
		}
	}
	
	public static class Consumer extends Thread {
		
		private final RawQueue queue;
		private final List<Long> messagesReceived  = new ArrayList<Long>();
		private final List<Long> batchesReceived = new ArrayList<Long>();
		private long busySpinCount = 0;
		
		public Consumer(RawQueue queue) {
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
				long avail = queue.availableToRead(); // <=========
				if (avail >= MSG_SIZE) {
					long batchAvail = avail / MSG_SIZE;
					RawBytes rawBytes = queue.getConsumer();
					for(long i = 0; i < batchAvail; i++) {
						long number = rawBytes.getLong();
						boolean last = rawBytes.getByte() == 1;
						messagesReceived.add(number); // save just the long value from this message
						if (last) isRunning = false; // I'm done!
					}
					queue.doneReading(); // <=========
					batchesReceived.add(batchAvail); // save the batch sizes received, just so we can double check
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
		
		RawQueue queue = new ByteBufferRawQueue(Math.max(batchSizeToSend * MSG_SIZE * 2, 1024));
		
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
		
		// Were the messages received in order?
		List<Long> sortedList = new ArrayList<Long>(messagesReceived);
		Collections.sort(sortedList);
		if (sortedList.equals(messagesReceived)) System.out.println("SUCCESS: Messages were received in order!");
		else System.out.println("ERROR: Messages were received out of order!");
		
		// If we sum all batches received do we get the correct number of messages?
		long sumOfAllBatches = batchesReceived.stream().mapToLong(Long::longValue).sum();
		if (sumOfAllBatches == messagesToSend) System.out.println("SUCCESS: The sum of messages from the batches received is correct! => " + sumOfAllBatches);
		else System.out.println("ERROR: The sum of messages from the batches received is incorrect! => " + sumOfAllBatches);
		
		System.out.println("\nMore info:\n");
		
		System.out.println("Number of batches received: " + batchesReceived.size());
		System.out.println("Batches received: " + batchesReceived.toString());
		System.out.println("Producer busy-spin count: " + producer.getBusySpinCount());
		System.out.println("Consumer busy-spin count: " + consumer.getBusySpinCount());
	}
}