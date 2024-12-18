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
 * A task to be sent to the diamond queue for execution by the worker threads. 
 */
public abstract class Task {
	
	private boolean ok;
	private Throwable t;
	
	/**
	 * This method will be called by a worker thread of the diamond queue.
	 * 
	 * @return true if the execution was successful
	 */
	public abstract boolean execute();
	
	final void exec() {
		this.ok = false;
		this.t = null;
		try {
			this.ok = execute();
		} catch(Throwable e) {
			this.t = e;
		}
	}
	
	/**
	 * Check if the execution was successful
	 * 
	 * @return true if the execution was successful
	 */
	public final boolean wasSuccessful() {
		return ok;
	}
	
	/**
	 * If the execution was not successful, you can use this method to retrieve the exception
	 * 
	 * @return the exception that occurred during the execution
	 */
	public final Throwable getException() {
		return t;
	}
}