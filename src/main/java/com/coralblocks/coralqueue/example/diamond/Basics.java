package com.coralblocks.coralqueue.example.diamond;

import java.util.Random;

import com.coralblocks.coralqueue.diamond.AtomicDiamond;
import com.coralblocks.coralqueue.diamond.Diamond;
import com.coralblocks.coralqueue.diamond.Input;
import com.coralblocks.coralqueue.diamond.Output;
import com.coralblocks.coralqueue.diamond.Task;

public class Basics {
	
    public static class AddTask extends Task {
    	
        public int x;
        public int y;
        public int result;
         
        @Override
        public boolean execute() {
            this.result = x + y;
            return true; // successful!
        }
    }
	
    private static final long receiveTasks(Output<AddTask> output) {
		long avail = output.availableToFetch();
		for(long i = 0; i < avail; i++) {
			AddTask at = output.fetch();
			if (at.x + at.y != at.result) throw new RuntimeException("Wrong result!");
		}
		output.doneFetching();
		return avail;
    }
	
	public static void main(String[] args) throws InterruptedException {
		
		final int tasks = args.length > 0 ? Integer.parseInt(args[0]) : 100_000;
		final int batchSizeToSend = args.length > 1 ? Integer.parseInt(args[1]) : 100;
		final int numberOfWorkerThreads = args.length > 2 ? Integer.parseInt(args[2]) : 4;
		
		System.out.println("Creating a diamond with " + numberOfWorkerThreads + 
						   " worker threads to send " + tasks + " tasks in batches of "
						   + batchSizeToSend + "...");
		
        Diamond<AddTask> diamond = new AtomicDiamond<AddTask>(AddTask.class, numberOfWorkerThreads);
         
        Input<AddTask> input = diamond.getInput();
        Output<AddTask> output = diamond.getOutput();
         
		final Random rand = new Random();
		
		diamond.start(false); // false = non-daemon thread...
		
		int tasksSent = tasks;
		int tasksReceived = 0;
		while(tasksSent > 0) {
			int batchToSend = Math.min(batchSizeToSend, tasksSent);
			for(int i = 0; i < batchToSend; i++) {
				AddTask at;
				while((at = input.nextToDispatch()) == null); // busy spin
				at.x = rand.nextInt(1000);
				at.y = rand.nextInt(1000);
			}
			input.flush();
			tasksSent -= batchToSend;
			if (tasksReceived != tasks) tasksReceived += receiveTasks(output); // also drain (if => once)
		}
		
		System.out.println("Tasks received while sending: " + tasksReceived);
		
		while(tasksReceived != tasks) tasksReceived += receiveTasks(output); // finish draining (while => busy spin)
		
		System.out.println("Finished receiving all tasks: " + tasksReceived);
		
        diamond.stop(); // stop all worker threads...
        diamond.join();
        
	}
}