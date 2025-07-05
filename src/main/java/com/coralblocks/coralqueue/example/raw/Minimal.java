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

import com.coralblocks.coralqueue.raw.ByteBufferRawQueue;
import com.coralblocks.coralqueue.raw.RawBytes;
import com.coralblocks.coralqueue.raw.RawQueue;

public class Minimal {
	
	public static void main(String[] args) throws InterruptedException {
		
		final int messagesToSend = 10;
		
		final RawQueue queue = new ByteBufferRawQueue(); // default size is 1024
		
		Thread producer = new Thread(new Runnable() {

			@Override
			public void run() {

				for(int i = 0; i < messagesToSend; i += 2) { // note we are looping 2 by 2 (we are sending a batch of 2 messages)
					
					while(queue.availableToWrite() < 16); // busy spin
					
					RawBytes rawBytes = queue.getProducer();
					
					rawBytes.putLong(i);
					rawBytes.putLong(i + 1);
					
					queue.flush(); // don't forget to notify consumer
				}
			}
			
		}, "Producer"); // thread name
		
		Thread consumer = new Thread(new Runnable() {

			@Override
			public void run() {
				
				boolean isRunning = true;
				
				while(isRunning) {
					
					while(queue.availableToRead() < 8); // busy spin
					
					RawBytes rawBytes = queue.getConsumer();
					
					while(rawBytes.getRemaining() >= 8) {
						long value = rawBytes.getLong();
						System.out.print(value);
						if (value == messagesToSend - 1) isRunning = false; // done receiving all messages
					}
					
					queue.doneReading(); // don't forget to notify producer
				}
			}
			
		}, "Consumer"); // thread name
		
		producer.start(); // start the producer thread
		consumer.start(); // start the consumer thread
		
		producer.join(); // wait for thread to finish and die
		consumer.join(); // wait for thread to finish and die
		
		System.out.println();
		
		// OUTPUT: 0123456789
	}
}