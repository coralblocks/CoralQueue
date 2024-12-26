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

import java.util.concurrent.atomic.AtomicBoolean;

import com.coralblocks.coralqueue.demultiplexer.AtomicDemultiplexer;
import com.coralblocks.coralqueue.demultiplexer.Demultiplexer;
import com.coralblocks.coralqueue.multiplexer.AtomicMultiplexer;
import com.coralblocks.coralqueue.multiplexer.Multiplexer;
import com.coralblocks.coralqueue.util.Builder;
import com.coralblocks.coralqueue.waitstrategy.WaitStrategy;

public class AtomicDiamond<E extends Task> implements Diamond<E> {
	
	public static final int DEFAULT_CAPACITY = 1024;
	
	private final Demultiplexer<E> demux;
	private final Multiplexer<E> mux;
	private final Input<E> input;
	private final Output<E> output;
	private final Thread[] threads;
	private final AtomicBoolean[] isRunning;
	
	public AtomicDiamond(Class<E> klass, int workerThreads) {
		this(DEFAULT_CAPACITY, Builder.createBuilder(klass), workerThreads, null);
	}

	public AtomicDiamond(Class<E> klass, int workerThreads, final WorkerThreadListener listener) {
		this(DEFAULT_CAPACITY, Builder.createBuilder(klass), workerThreads, listener);
	}
	
	public AtomicDiamond(Builder<E> builder, int workerThreads) {
		this(DEFAULT_CAPACITY, builder, workerThreads, null);
	}
	
	public AtomicDiamond(Builder<E> builder, int workerThreads, final WorkerThreadListener listener) {
		this(DEFAULT_CAPACITY, builder, workerThreads, listener);
	}
	
	public AtomicDiamond(int capacity, Class<E> klass, int workerThreads) {
		this(capacity, Builder.createBuilder(klass), workerThreads, null);
	}

	public AtomicDiamond(int capacity, Class<E> klass, int workerThreads, final WorkerThreadListener listener) {
		this(capacity, Builder.createBuilder(klass), workerThreads, listener);
	}
	
	public AtomicDiamond(int capacity, Builder<E> builder, int workerThreads) {
		this(capacity, builder, workerThreads, null);
	}
	
	public AtomicDiamond(int capacity, Builder<E> builder, int workerThreads, final WorkerThreadListener listener) {
		
		this.demux = new AtomicDemultiplexer<E>(capacity, builder, workerThreads);
		this.mux = new AtomicMultiplexer<E>(capacity, builder, workerThreads);
		
		this.input = new Input<E>(demux);
		this.output = new Output<E>(mux);
		
		this.isRunning = new AtomicBoolean[workerThreads];
		
		this.threads = new Thread[workerThreads];
		
		for(int i = 0; i < workerThreads; i++) {
			
			final int index = i;
			
			isRunning[index] = new AtomicBoolean(true);
			
			threads[index] = new Thread(new Runnable() {
				@Override
				public void run() {
					
					WaitStrategy waitStrategy = null;
					if (listener != null) waitStrategy = listener.onStarted(index);
					
					while(isRunning[index].get()) {
					
						long avail = demux.availableToFetch(index);
						
						if (avail == 0) {
							if (waitStrategy != null) waitStrategy.await();
							continue;
						}
						
						if (waitStrategy != null) waitStrategy.reset();
						
						for(long x = 0; x < avail; x++) {
						
							E inVal = demux.fetch(index); // get object from demux
							
							inVal.exec(); // execute
							
							E outVal = null; // prepare to swap with mux
							
							while((outVal = mux.nextToDispatch(index, inVal)) == null) { // !!! note that we are swapping !!!
								if (waitStrategy != null) waitStrategy.await();
							}
							
							if (waitStrategy != null) waitStrategy.reset();
							
							demux.replace(index, outVal); // move from mux to demux
						}

						mux.flush(index);
						demux.doneFetching(index);
					}
					
					if (listener != null) listener.onDied(index);
				}
				
			}, AtomicDiamond.class.getSimpleName() + "-WorkerThread-" + index);
		}
	}
	
	@Override
	public int getNumberOfWorkerThreads() {
		return threads.length;
	}
	
	@Override
	public void start(boolean daemon) {
		for(int i = 0; i < threads.length; i++) {
			threads[i].setDaemon(daemon);
			threads[i].start();
		}
	}
	
	@Override
	public void stop() {
		for(int i = 0; i < isRunning.length; i++) {
			isRunning[i].set(false);
		}
	}
	
	@Override
	public void join() throws InterruptedException {
		for(int i = 0; i < isRunning.length; i++) {
			threads[i].join();
		}
	}
	
	@Override
	public Input<E> getInput() {
		return input;
	}

	@Override
	public Output<E> getOutput() {
		return output;
	}
}