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