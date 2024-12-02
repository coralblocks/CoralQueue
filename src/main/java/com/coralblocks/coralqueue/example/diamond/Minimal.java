package com.coralblocks.coralqueue.example.diamond;

import java.util.Random;

import com.coralblocks.coralqueue.diamond.AtomicDiamond;
import com.coralblocks.coralqueue.diamond.Diamond;
import com.coralblocks.coralqueue.diamond.Input;
import com.coralblocks.coralqueue.diamond.Output;
import com.coralblocks.coralqueue.diamond.Task;

public class Minimal {
	
    public static class AddTask extends Task {
    	
    	private final Random rand = new Random();
    	
    	public int id;
        public int x;
        public int y;
        public int result;
         
        @Override
        public boolean execute() {
        	randomSleep(10);
            this.result = x + y;
            return true; // successful!
        }
        
        private final void randomSleep(int maxSleepTime) {
        	try {
        		Thread.sleep(rand.nextInt(maxSleepTime) + 1);
        	} catch(InterruptedException e) {
        		throw new RuntimeException(e);
        	}
        }
    }
	
	public static void main(String[] args) throws InterruptedException {
		
		final int tasks = 10;
		
        Diamond<AddTask> diamond = new AtomicDiamond<AddTask>(AddTask.class, 2);
         
        Input<AddTask> input = diamond.getInput();
        Output<AddTask> output = diamond.getOutput();
         
		final Random rand = new Random();
		int ids = 0;
		
		diamond.start(false); // false = non-daemon thread...
		
		for(int i = 0; i < tasks; i += 2) { // note we are looping 2 by 2 (we are sending a batch of 2 messages)
			
			AddTask at;
			
			while((at = input.nextToDispatch()) == null); // busy spin
			at.id = ids++;
			at.x = rand.nextInt(100);
			at.y = rand.nextInt(100);
			
			while((at = input.nextToDispatch()) == null); // busy spin
			at.id = ids++;
			at.x = rand.nextInt(100);
			at.y = rand.nextInt(100);
			
			input.flush();
		}
		
		long received = 0;
		
		while(received != tasks) {
			
			long avail = output.availableToFetch();
			
			if (avail == 0) continue; // busy spin
			
			for(long i = 0; i < avail; i++) {
				AddTask at = output.fetch();
				System.out.println("AddTask! id=" + at.id + " successful=" + at.wasSuccessful() + " x=" + at.x + " y=" + at.y + " result=" + at.result);
			}
			
			output.doneFetching();
			
			received += avail;
		}
		
        diamond.stop(); // stop all worker threads...
        diamond.join();
        
	}
}