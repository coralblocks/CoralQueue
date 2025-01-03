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
package com.coralblocks.coralqueue.example.broadcaster;

import com.coralblocks.coralqueue.broadcaster.AtomicBroadcaster;
import com.coralblocks.coralqueue.broadcaster.Broadcaster;
import com.coralblocks.coralqueue.util.MutableLong;

public class Minimal {
	
	public static void main(String[] args) throws InterruptedException {
		
		final int messagesToSend = 10;
		final int numberOfConsumers = 2;
		
		final Broadcaster<MutableLong> broadcaster = new AtomicBroadcaster<MutableLong>(MutableLong.class, numberOfConsumers); // default size is 1024
		
		Thread producer = new Thread(new Runnable() {

			@Override
			public void run() {

				for(int i = 0; i < messagesToSend; i += 2) { // note we are looping 2 by 2 (we are sending a batch of 2 messages)
					
					MutableLong ml; // our data transfer mutable object
					
					while((ml = broadcaster.nextToDispatch()) == null); // busy spin
					ml.set(i);
					
					while((ml = broadcaster.nextToDispatch()) == null); // busy spin
					ml.set(i + 1);
					
					broadcaster.flush(); // don't forget to notify consumers
				}
				
				MutableLong ml;
					
				while((ml = broadcaster.nextToDispatch()) == null); // busy spin
				ml.set(-1); // -1 to signal to the consumers to finish
				
				broadcaster.flush();
			}
			
		}, "Producer"); // thread name
		
		Thread[] consumers = new Thread[numberOfConsumers];
		
		for(int index = 0; index < numberOfConsumers; index++) {
		
			final int consumerIndex = index;
			
			consumers[index] = new Thread(new Runnable() {
	
				@Override
				public void run() {
	
					boolean isRunning = true;
					
					while(isRunning) {
						
						long avail = broadcaster.availableToFetch(consumerIndex); // read available batches as fast as possible
						
						if (avail == 0) continue; // busy spin
						
						for(int i = 0; i < avail; i++) {
							
							MutableLong ml = broadcaster.fetch(consumerIndex);
							
							if (ml.get() == -1) { // -1 means we need to finish
								isRunning = false; // done receiving all messages from the producer
							} else {
								System.out.print(ml.get());
							}
						}
						
						broadcaster.doneFetching(consumerIndex); // don't forget to notify the producer
					}
				}
				
			}, "Consumer-" + index); // thread name
		}
		
		producer.start(); // start the producer thread
		for(Thread consumer : consumers) consumer.start(); // start the consumer threads
		
		producer.join(); // wait for thread to finish and die
		for(Thread consumer : consumers) consumer.join(); // wait for threads to finish and die
		
		System.out.println();
		
		// OUTPUT: 00123456718923456789 // NOTE: Note that the order is completely undetermined
	}
}