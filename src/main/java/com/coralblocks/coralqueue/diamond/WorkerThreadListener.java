package com.coralblocks.coralqueue.diamond;

import com.coralblocks.coralqueue.waitstrategy.WaitStrategy;

/**
 * <p>A callback listener for the worker threads from the diamond queue.</p>
 * 
 *  <p>NOTE: These methods will be called by the worker thread itself.</p>
 */
public  interface WorkerThreadListener {
		
	/**
	 * Called by the worker thread when the thread is started.
	 * 
	 * @param index the index of the worker threads from the diamond queue
	 * @return a wait strategy to be used by the worker thread or null to busy spin
	 */
	public WaitStrategy onStarted(int index);
		
	/**
	 * Called by the worker thread when the thread is dying (i.e. exiting).
	 * 
	 * @param index the index of the worker threads from the diamond queue
	 */
	public void onDied(int index);
}