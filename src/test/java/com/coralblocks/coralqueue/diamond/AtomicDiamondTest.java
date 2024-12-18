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
package com.coralblocks.coralqueue.diamond;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;


public class AtomicDiamondTest {
    
    public static class AddTask extends Task {
    	
    	public int uniqueId;
    	public int workerThreadIndex;
        public int x;
        public int y;
        public int result;
         
        @Override
        public boolean execute() {
            this.result = x + y;
            return true; // successful!
        }
    }
     
    @Test
    public void testTwoThreads() throws InterruptedException {
    	
        Diamond<AddTask> diamond = new AtomicDiamond<AddTask>(AddTask.class, 4);
         
        Input<AddTask> input = diamond.getInput();
        Output<AddTask> output = diamond.getOutput();
         
        final int tasks = 500_000;
        
        Thread producer = new Thread(new Runnable() {
        	
			@Override
			public void run() {

				final Random rand = new Random();
				int ids = 1;
				
				int tasksSent = tasks;
				while(tasksSent > 0) {
					int batchSizeToSend = rand.nextInt(100) + 1;
					int batchToSend = Math.min(batchSizeToSend, tasksSent);
					for(int i = 0; i < batchToSend; i++) {
						AddTask at;
						while((at = input.nextToDispatch()) == null);
						at.uniqueId = ids++;
						at.x = rand.nextInt(1000);
						at.y = rand.nextInt(1000);
					}
					input.flush();
					tasksSent -= batchToSend;
				}
			}
        }, "Producer");
        
        Thread consumer = new Thread(new Runnable() {

			@Override
			public void run() {
				
				Set<Integer> testUnique = new HashSet<Integer>();
				
				long tasksReceived = 0;
				
				while(tasksReceived != tasks) {
					
					long avail = output.availableToFetch();
					
					if (avail == 0) continue;
					
					for(long i = 0; i < avail; i++) {
						
						AddTask at = output.fetch();
						Assert.assertEquals(at.x + at.y, at.result);
						Assert.assertEquals(true, testUnique.add(at.uniqueId));
					}
					
					tasksReceived += avail;
					
					output.doneFetching();
				}
				
				Assert.assertEquals(tasks, testUnique.size()); // sanity
			}
			
        }, "Consumer");
        
        diamond.start(false); // false = non-daemon thread...
        
        producer.start();
        consumer.start();
        
        producer.join();
        consumer.join();
         
        diamond.stop(); // stop all worker threads...
        diamond.join();
    }
    
    private final long receiveTasks(Output<AddTask> output, Set<Integer> testUnique) {
    	
		long avail = output.availableToFetch();
			
		if (avail == 0) return 0;
			
		for(long i = 0; i < avail; i++) {
				
			AddTask at = output.fetch();
			Assert.assertEquals(at.x + at.y, at.result);
			Assert.assertEquals(true, testUnique.add(at.uniqueId));
		}
			
		output.doneFetching();
		
		return avail;
    }
    
    @Test
    public void testOneThread() throws InterruptedException {
    	
        Diamond<AddTask> diamond = new AtomicDiamond<AddTask>(AddTask.class, 4);
         
        Input<AddTask> input = diamond.getInput();
        Output<AddTask> output = diamond.getOutput();
         
        final int tasks = 500_000;
        
		final Random rand = new Random();
		int ids = 1;
		final Set<Integer> testUnique = new HashSet<Integer>();
		
		diamond.start(false); // false = non-daemon thread...
		
		int tasksSent = tasks;
		int tasksReceived = 0;
		while(tasksSent > 0) {
			int batchSizeToSend = rand.nextInt(100) + 1;
			int batchToSend = Math.min(batchSizeToSend, tasksSent);
			for(int i = 0; i < batchToSend; i++) {
				AddTask at;
				while((at = input.nextToDispatch()) == null);
				at.uniqueId = ids++;
				at.x = rand.nextInt(1000);
				at.y = rand.nextInt(1000);
			}
			input.flush();
			tasksSent -= batchToSend;
			if (tasksReceived != tasks) tasksReceived += receiveTasks(output, testUnique); // also drain (if)
		}
		
		while(tasksReceived != tasks) tasksReceived += receiveTasks(output, testUnique); // finish draining (while)
		
		Assert.assertEquals(tasks, testUnique.size()); // sanity
        
        diamond.stop(); // stop all worker threads...
        diamond.join();
    }
    
    @Test
    public void testOrderThroughWorkerThreadIndex() throws InterruptedException {
    	
        Diamond<AddTask> diamond = new AtomicDiamond<AddTask>(AddTask.class, 4);
         
        Input<AddTask> input = diamond.getInput();
        Output<AddTask> output = diamond.getOutput();
        final int numberOfWorkerThreads = diamond.getNumberOfWorkerThreads();
         
        final int tasks = 500_000;
        
        Thread producer = new Thread(new Runnable() {
        	
			@Override
			public void run() {

				final Random rand = new Random();
				int ids = 1;
				
				int tasksSent = tasks;
				while(tasksSent > 0) {
					int batchSizeToSend = rand.nextInt(100) + 1;
					int batchToSend = Math.min(batchSizeToSend, tasksSent);
					int workerThreadIndex = rand.nextInt(numberOfWorkerThreads); // choose worker thread index
					for(int i = 0; i < batchToSend; i++) {
						AddTask at;
						while((at = input.nextToDispatch(workerThreadIndex)) == null); // notice work thread index
						at.uniqueId = ids++;
						at.workerThreadIndex = workerThreadIndex;
						at.x = rand.nextInt(1000);
						at.y = rand.nextInt(1000);
					}
					input.flush();
					tasksSent -= batchToSend;
				}
			}
        }, "Producer");
        
        Thread consumer = new Thread(new Runnable() {

			@Override
			public void run() {
				
				Set<Integer> testUnique = new HashSet<Integer>();
				Map<Integer, List<Integer>> testOrder = new HashMap<Integer, List<Integer>>();
				
				long tasksReceived = 0;
				
				while(tasksReceived != tasks) {
					
					long avail = output.availableToFetch();
					
					if (avail == 0) continue;
					
					for(long i = 0; i < avail; i++) {
						
						AddTask at = output.fetch();
						Assert.assertEquals(at.x + at.y, at.result);
						Assert.assertEquals(true, testUnique.add(at.uniqueId));
						
						List<Integer> orderedList = testOrder.get(at.workerThreadIndex);
						if (orderedList == null) {
							orderedList = new ArrayList<Integer>();
							testOrder.put(at.workerThreadIndex, orderedList);
						} else {
							int lastId = orderedList.get(orderedList.size() - 1);
							Assert.assertTrue(at.uniqueId > lastId);
						}
						orderedList.add(at.uniqueId);
					}
					
					tasksReceived += avail;
					
					output.doneFetching();
				}
				
				Assert.assertEquals(tasks, testUnique.size()); // sanity
			}
			
        }, "Consumer");
        
        diamond.start(false); // false = non-daemon thread...
        
        producer.start();
        consumer.start();
        
        producer.join();
        consumer.join();
         
        diamond.stop(); // stop all worker threads...
        diamond.join();
    }
}