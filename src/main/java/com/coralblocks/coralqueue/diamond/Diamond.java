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

/**
 * The Diamond API that allows you to distribute tasks to a fixed set of worker threads.
 * A producer thread can use the input to send the tasks for execution by the worker threads.
 * A consumer thread can use the output to receive the executed tasks.
 * Producer and consumer can be the same thread, in other words, you can send tasks and receive results simultaneously.
 *
 * @param <E> an object inheriting from {@link Task}
 */
public interface Diamond<E extends Task> {
	
	/**
	 * Return the input of this diamond queue.
	 * 
	 * @return the input
	 */
	public Input<E> getInput();
	
	/**
	 * Return the output of this diamond queue.
	 * 
	 * @return the output
	 */
	public Output<E> getOutput();
	
	/**
	 * Return the number of worker threads for this diamond queue.
	 * 
	 * @return the number of worker threads
	 */
	public int getNumberOfWorkerThreads();
	
	/**
	 * Start the worker threads to begin receiving and executing tasks.
	 * 
	 * @param deamon true to make the worker threads deamon threads
	 */
	public void start(boolean deamon);
	
	/**
	 * Make all worker threads stop, exit and die.
	 */
	public void stop();

	/**
	 * Wait for all worker threads to die.
	 * 
	 * @throws InterruptedException
	 */
	public void join() throws InterruptedException;

}