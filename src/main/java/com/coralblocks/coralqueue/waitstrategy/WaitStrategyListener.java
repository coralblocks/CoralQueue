/* 
 * Copyright 2024 (c) CoralBlocks - http://www.coralblocks.com
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
package com.coralblocks.coralqueue.waitstrategy;

/**
 * A listener to receive callbacks from a {@link WaitStrategy}. A {@link WaitStrategy} can register one or more of these
 * listeners to provide callbacks from its main methods <code>block()</code> and <code>reset()</code>.
 */
public interface WaitStrategyListener {
	
	/**
	 * The wait strategy was blocked (i.e. method <code>block()</code> was called)
	 * 
	 * @param waitStrategy the wait strategy providing this callback
	 * @param isDone true if the wait strategy is done and its <code>block()</code> method will be returning true
	 */
	public void blocked(WaitStrategy waitStrategy, boolean isDone);
	
	/**
	 * The wait strategy was reset (i.e. method <code>reset()</code> was called)
	 * 
	 * @param waitStrategy the wait strategy providing this callback
	 */
	public void reset(WaitStrategy waitStrategy);
}