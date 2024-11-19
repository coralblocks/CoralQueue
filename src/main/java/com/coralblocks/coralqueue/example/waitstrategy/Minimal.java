package com.coralblocks.coralqueue.example.waitstrategy;

import com.coralblocks.coralqueue.queue.AtomicQueue;
import com.coralblocks.coralqueue.queue.Queue;
import com.coralblocks.coralqueue.util.MutableLong;
import com.coralblocks.coralqueue.waitstrategy.BusySleepBackOffWaitStrategy;
import com.coralblocks.coralqueue.waitstrategy.BusySpinYieldSleepWaitStrategy;
import com.coralblocks.coralqueue.waitstrategy.WaitStrategy;

public class Minimal {
	
	public static void main(String[] args) throws InterruptedException {
		
		final int messagesToSend = 10;
		
		final Queue<MutableLong> queue = new AtomicQueue<MutableLong>(MutableLong.class); // default size is 1024
		
		Thread producer = new Thread(new Runnable() {
			
			private final WaitStrategy producerWaitStrategy = new BusySleepBackOffWaitStrategy();

			@Override
			public void run() {

				for(int i = 0; i < messagesToSend; i += 2) { // note we are looping 2 by 2 (we are sending a batch of 2 messages)
					
					MutableLong ml; // our data transfer mutable object
					
					while((ml = queue.nextToDispatch()) == null) producerWaitStrategy.block();
					producerWaitStrategy.reset();
					ml.set(i);
					
					while((ml = queue.nextToDispatch()) == null) producerWaitStrategy.block();
					producerWaitStrategy.reset();
					ml.set(i + 1);
					
					queue.flush(); // don't forget to notify consumer
				}
			}
			
		}, "Producer"); // thread name
		
		Thread consumer = new Thread(new Runnable() {
			
			private final WaitStrategy consumerWaitStrategy = new BusySpinYieldSleepWaitStrategy();

			@Override
			public void run() {
				
				boolean isRunning = true;
				
				while(isRunning) {
					
					long avail = queue.availableToPoll(); // read available batches as fast as possible
					
					if (avail == 0) {
						consumerWaitStrategy.block();
						continue;
					}
					
					for(int i = 0; i < avail; i++) {
						
						MutableLong ml = queue.poll();
						
						System.out.print(ml.get());
						
						if (ml.get() == messagesToSend - 1) isRunning = false; // done receiving all messages
					}
					
					queue.donePolling(); // don't forget to notify producer
					
					consumerWaitStrategy.reset();
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