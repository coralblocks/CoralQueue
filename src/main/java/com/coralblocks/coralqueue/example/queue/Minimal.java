package com.coralblocks.coralqueue.example.queue;

import com.coralblocks.coralqueue.queue.AtomicQueue;
import com.coralblocks.coralqueue.queue.Queue;
import com.coralblocks.coralqueue.util.MutableLong;

public class Minimal {
	
	public static void main(String[] args) throws InterruptedException {
		
		final int messagesToSend = 10;
		
		final Queue<MutableLong> queue = new AtomicQueue<MutableLong>(MutableLong.class); // default size is 1024
		
		Thread producer = new Thread(new Runnable() {

			@Override
			public void run() {

				for(int i = 0; i < messagesToSend; i += 2) { // note we are looping 2 by 2 (we are sending a batch of 2 messages)
					
					MutableLong ml; // our data transfer mutable object
					
					while((ml = queue.nextToDispatch()) == null); // busy spin
					ml.set(i);
					
					while((ml = queue.nextToDispatch()) == null); // busy spin
					ml.set(i + 1);
					
					queue.flush(); // don't forget to notify consumer
				}
			}
			
		}, "Producer"); // thread name
		
		Thread consumer = new Thread(new Runnable() {

			@Override
			public void run() {
				
				boolean isRunning = true;
				
				while(isRunning) {
					
					long avail = queue.availableToPoll(); // read available batches as fast as possible
					
					if (avail == 0) continue; // busy spin
					
					for(int i = 0; i < avail; i++) {
						
						MutableLong ml = queue.poll();
						
						System.out.print(ml.get());
						
						if (ml.get() == messagesToSend - 1) isRunning = false; // done receiving all messages
					}
					
					queue.donePolling(); // don't forget to notify producer
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