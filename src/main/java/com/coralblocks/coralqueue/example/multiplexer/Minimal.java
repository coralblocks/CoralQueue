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
package com.coralblocks.coralqueue.example.multiplexer;

import com.coralblocks.coralqueue.multiplexer.AtomicMultiplexer;
import com.coralblocks.coralqueue.multiplexer.Multiplexer;
import com.coralblocks.coralqueue.util.MutableLong;

public class Minimal {
	
	public static void main(String[] args) throws InterruptedException {
		
		final int messagesToSend = 10;
		final int numberOfProducers = 2;
		final int messagesToReceive = messagesToSend * numberOfProducers;
		
		final Multiplexer<MutableLong> mux = new AtomicMultiplexer<MutableLong>(MutableLong.class, numberOfProducers); // default size is 1024
		
		Thread[] producers = new Thread[numberOfProducers];
		
		for(int index = 0; index < numberOfProducers; index++) { // index = producer index
			
			final int producerIndex = index;
		
			producers[index] = new Thread(new Runnable() {
	
				@Override
				public void run() {
	
					for(int i = 0; i < messagesToSend; i += 2) { // note we are looping 2 by 2 (we are sending a batch of 2 messages)
						
						MutableLong ml; // our data transfer mutable object
						
						while((ml = mux.nextToDispatch(producerIndex)) == null); // busy spin
						ml.set(i);
						
						while((ml = mux.nextToDispatch(producerIndex)) == null); // busy spin
						ml.set(i + 1);
						
						mux.flush(producerIndex); // don't forget to notify consumer
					}
				}
				
			}, "Producer-" + index); // thread name
		}
		
		Thread consumer = new Thread(new Runnable() {

			@Override
			public void run() {

				int messagesReceived = 0;
				
				boolean isRunning = true;
				
				while(isRunning) {
					
					long avail = mux.availableToFetch(); // read available batches as fast as possible
					
					if (avail == 0) continue; // busy spin
					
					for(int i = 0; i < avail; i++) {
						
						MutableLong ml = mux.fetch();
						
						System.out.print(ml.get());
						
						if (++messagesReceived == messagesToReceive) isRunning = false; // done receiving all messages from all producers
					}
					
					mux.doneFetching(); // don't forget to notify producers
				}
			}
			
		}, "Consumer"); // thread name
		
		for(Thread producer : producers) producer.start(); // start the producer threads
		consumer.start(); // start the consumer thread
		
		for(Thread producer : producers) producer.join(); // wait for threads to finish and die
		consumer.join(); // wait for thread to finish and die
		
		System.out.println();
		
		// OUTPUT: 00112233445566778899 // NOTE: This order is NOT GUARANTEED as in the real world a producer might lag behind
	}
}