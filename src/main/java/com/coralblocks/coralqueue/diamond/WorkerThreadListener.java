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