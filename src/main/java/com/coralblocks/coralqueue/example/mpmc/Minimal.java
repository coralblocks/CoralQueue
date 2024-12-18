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
package com.coralblocks.coralqueue.example.mpmc;

import com.coralblocks.coralqueue.mpmc.AtomicMpMc;
import com.coralblocks.coralqueue.mpmc.MpMc;
import com.coralblocks.coralqueue.util.MutableLong;

public class Minimal {
	
	public static void main(String[] args) throws InterruptedException {
		
		final int messagesToSend = 10;
		final int numberOfConsumers = 2;
		final int numberOfProducers = 2;
		
		final MpMc<MutableLong> mpmc = new AtomicMpMc<MutableLong>(MutableLong.class, numberOfProducers, numberOfConsumers); // default size is 1024
		
		Thread[] producers = new Thread[numberOfProducers];
		
		for(int index = 0; index < numberOfProducers; index++) {
			
			final int producerIndex = index;
		
			producers[index] = new Thread(new Runnable() {
	
				@Override
				public void run() {
	
					for(int i = 0; i < messagesToSend; i += 2) { // note we are looping 2 by 2 (we are sending a batch of 2 messages)
						
						MutableLong ml; // our data transfer mutable object
						
						while((ml = mpmc.nextToDispatch(producerIndex)) == null); // busy spin
						ml.set(i);
						
						while((ml = mpmc.nextToDispatch(producerIndex)) == null); // busy spin
						ml.set(i + 1);
						
						mpmc.flush(producerIndex); // don't forget to notify consumer
					}
					
					for(int consumerIndex = 0; consumerIndex < numberOfConsumers; consumerIndex++) { // send a final message (-1) to each consumer
					
						MutableLong ml;
						
						while((ml = mpmc.nextToDispatch(producerIndex, consumerIndex)) == null); // busy spin (note we are sending to a specific consumer)
						ml.set(-1); // -1 to signal to the consumer to finish
					}
					
					mpmc.flush(producerIndex);
				}
				
			}, "Producer-" + index); // thread name
		}
		
		Thread[] consumers = new Thread[numberOfConsumers];
		
		for(int index = 0; index < numberOfConsumers; index++) {
		
			final int consumerIndex = index;
			
			consumers[index] = new Thread(new Runnable() {
	
				@Override
				public void run() {
					
					int lastCount = 0; // count the number of exit signals from producers
	
					boolean isRunning = true;
					
					while(isRunning) {
						
						long avail = mpmc.availableToFetch(consumerIndex); // read available batches as fast as possible
						
						if (avail == 0) continue; // busy spin
						
						for(int i = 0; i < avail; i++) {
							
							MutableLong ml = mpmc.fetch(consumerIndex);
							
							if (ml.get() == -1) { // -1 means we need to finish
								if (++lastCount == numberOfProducers) isRunning = false; // done receiving all finish signals from all producers
							} else {
								System.out.print(ml.get());
							}
						}
						
						mpmc.doneFetching(consumerIndex); // don't forget to notify the producers
					}
				}
				
			}, "Consumer-" + index); // thread name
		}
		
		for(Thread producer : producers) producer.start(); // start the producer threads
		for(Thread consumer : consumers) consumer.start(); // start the consumer threads
		
		for(Thread producer : producers) producer.join(); // wait for threads to finish and die
		for(Thread consumer : consumers) consumer.join(); // wait for threads to finish and die
		
		System.out.println();
		
		// OUTPUT: 13025791357946802468 // NOTE: Note that the order is completely undetermined
	}
}