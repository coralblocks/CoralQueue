package com.coralblocks.coralqueue.example.mpmcbroadcaster;

import com.coralblocks.coralqueue.mpmcbroadcaster.AtomicMpMcBroadcaster;
import com.coralblocks.coralqueue.mpmcbroadcaster.MpMcBroadcaster;
import com.coralblocks.coralqueue.util.MutableLong;

public class Minimal {
	
	public static void main(String[] args) throws InterruptedException {
		
		final int messagesToSend = 10;
		final int numberOfConsumers = 2;
		final int numberOfProducers = 2;
		
		final MpMcBroadcaster<MutableLong> mpmcBroadcaster = new AtomicMpMcBroadcaster<MutableLong>(MutableLong.class, numberOfProducers, numberOfConsumers); // default size is 1024
		
		Thread[] producers = new Thread[numberOfProducers];
		
		for(int index = 0; index < numberOfProducers; index++) {
			
			final int producerIndex = index;
		
			producers[index] = new Thread(new Runnable() {
	
				@Override
				public void run() {
	
					for(int i = 0; i < messagesToSend; i += 2) { // note we are looping 2 by 2 (we are sending a batch of 2 messages)
						
						MutableLong ml; // our data transfer mutable object
						
						while((ml = mpmcBroadcaster.nextToDispatch(producerIndex)) == null); // busy spin
						ml.set(i);
						
						while((ml = mpmcBroadcaster.nextToDispatch(producerIndex)) == null); // busy spin
						ml.set(i + 1);
						
						mpmcBroadcaster.flush(producerIndex); // don't forget to notify consumer
					}
					
					MutableLong ml;
						
					while((ml = mpmcBroadcaster.nextToDispatch(producerIndex)) == null); // busy spin
					ml.set(-1); // -1 to signal to the consumers to finish
					
					mpmcBroadcaster.flush(producerIndex);
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
						
						long avail = mpmcBroadcaster.availableToFetch(consumerIndex); // read available batches as fast as possible
						
						if (avail == 0) continue; // busy spin
						
						for(int i = 0; i < avail; i++) {
							
							MutableLong ml = mpmcBroadcaster.fetch(consumerIndex);
							
							if (ml.get() == -1) { // -1 means we need to finish
								if (++lastCount == numberOfProducers) isRunning = false; // done receiving all finish signals from all producers
							} else {
								System.out.print(ml.get());
							}
						}
						
						mpmcBroadcaster.doneFetching(consumerIndex); // don't forget to notify the producers
					}
				}
				
			}, "Consumer-" + index); // thread name
		}
		
		for(Thread producer : producers) producer.start(); // start the producer threads
		for(Thread consumer : consumers) consumer.start(); // start the consumer threads
		
		for(Thread producer : producers) producer.join(); // wait for threads to finish and die
		for(Thread consumer : consumers) consumer.join(); // wait for threads to finish and die
		
		System.out.println();
		
		// OUTPUT: 0012345126374859607182934015263748596789 // NOTE: Note that the order is completely undetermined
	}
}