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
package com.coralblocks.coralqueue.example.demultiplexer;

import com.coralblocks.coralqueue.demultiplexer.AtomicDemultiplexer;
import com.coralblocks.coralqueue.demultiplexer.Demultiplexer;
import com.coralblocks.coralqueue.util.MutableLong;

public class Minimal {
	
	public static void main(String[] args) throws InterruptedException {
		
		final int messagesToSend = 10;
		final int numberOfConsumers = 2;
		
		final Demultiplexer<MutableLong> demux = new AtomicDemultiplexer<MutableLong>(MutableLong.class, numberOfConsumers); // default size is 1024
		
		Thread producer = new Thread(new Runnable() {

			@Override
			public void run() {

				for(int i = 0; i < messagesToSend; i += 2) { // note we are looping 2 by 2 (we are sending a batch of 2 messages)
					
					MutableLong ml; // our data transfer mutable object
					
					while((ml = demux.nextToDispatch()) == null); // busy spin
					ml.set(i);
					
					while((ml = demux.nextToDispatch()) == null); // busy spin
					ml.set(i + 1);
					
					demux.flush(); // don't forget to notify consumers
				}
				
				for(int consumerIndex = 0; consumerIndex < numberOfConsumers; consumerIndex++) { // send a final message (-1) to each consumer
				
					MutableLong ml;
					
					while((ml = demux.nextToDispatch(consumerIndex)) == null); // busy spin (note we are sending to a specific consumer)
					ml.set(-1); // -1 to signal to the consumer to finish
				}
				
				demux.flush();
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
						
						long avail = demux.availableToFetch(consumerIndex); // read available batches as fast as possible
						
						if (avail == 0) continue; // busy spin
						
						for(int i = 0; i < avail; i++) {
							
							MutableLong ml = demux.fetch(consumerIndex);
							
							if (ml.get() == -1) { // -1 means we need to finish
								isRunning = false; // done receiving all messages from the producer
							} else {
								System.out.print(ml.get());
							}
						}
						
						demux.doneFetching(consumerIndex); // don't forget to notify the producer
					}
				}
				
			}, "Consumer-" + index); // thread name
		}
		
		producer.start(); // start the producer thread
		for(Thread consumer : consumers) consumer.start(); // start the consumer threads
		
		producer.join(); // wait for thread to finish and die
		for(Thread consumer : consumers) consumer.join(); // wait for threads to finish and die
		
		System.out.println();
		
		// OUTPUT: 0213574689 // NOTE: Note that the order is completely undetermined
	}
}