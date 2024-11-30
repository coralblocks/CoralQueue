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

import java.util.ArrayList;
import java.util.List;

/**
 * <p>An abstract implementation of the {@link WaitStrategy} interface that you can as the base class for
 * your wait strategy implementations. It takes care of most of the boilerplate code like registering and
 * unregistering listeners, calling the listeners, counting the number of blockings, returning false from
 * <code>block()</code>, etc.</p>
 * 
 * <p>By inheriting from this abstract base class, all you have to do is implement {@link blockOperation()} and
 * {@link resetOperation()}.
 */
public abstract class AbstractWaitStrategy implements WaitStrategy {
	
	public static final long DEFAULT_MAX_BLOCK_COUNT = -1;
	
	private final long maxBlockCount;
	private long blockCount = 0;
	
	private final List<WaitStrategyListener> listeners = new ArrayList<WaitStrategyListener>(8);

	/**
	 * Creates a new wait strategy using the given maximum number of blockings.
	 *
	 * @param maxBlockCount the maximum number of blockings before <code>block()</code> starts to return true
	 */
	public AbstractWaitStrategy(long maxBlockCount) {
		this.maxBlockCount = maxBlockCount;
	}
	
	/**
	 * Creates a new wait strategy using the default maximum number of blockings, which is <code>-1</code>, in other words,
	 * by default this wait strategy will not count the number of blockings and never return true from the <code>block()</code> method.
	 */
	public AbstractWaitStrategy() {
		this(DEFAULT_MAX_BLOCK_COUNT);
	}
	
	@Override
	public void addListener(WaitStrategyListener listener) {
		if (!listeners.contains(listener)) listeners.add(listener);
	}
	
	@Override
	public void removeListener(WaitStrategyListener listener) {
		listeners.remove(listener);
	}
	
	/**
	 * Return the maximum number of blockings configured for this wait strategy
	 * 
	 * @return the maximum number of blockings
	 */
	protected final long getMaxBlockCount() {
		return maxBlockCount;
	}
	
	/**
	 * Return the current number of blockings (block count)
	 * 
	 * @return the current number of blockings
	 */
	protected final long getBlockCount() {
		return blockCount;
	}

	@Override
	public final boolean block() {
		// Only increment blockCount if you really have to
		boolean done = false;
		if (maxBlockCount < 0) done = false; // we will be never done
		else if (blockCount == maxBlockCount) done = true; // don't increment forever
		else if (++blockCount == maxBlockCount) done = true; // increment
		
		blockOperation();
		
		for(int i = listeners.size() - 1; i >= 0; i--) {
			listeners.get(i).blocked(this, done);
		}
		
		return done;
	}
	
	@Override
	public final void reset() {
		
		blockCount = 0;
		
		resetOperation(); // this is useful for backing off wait strategies
		
		for(int i = listeners.size() - 1; i >= 0; i--) {
			listeners.get(i).reset(this);
		}
	}
	
	/**
	 * Implement this abstract method to perform the actually operation that will cause the blocking.
	 * For example, one such operation can be <code>Thread.sleep(long)</code>.
	 */
	protected abstract void blockOperation();
	
	/**
	 * Implement this method to reset any state of the wait strategy after blocking for one or more invocations of the <code>block()</code> method.
	 * This is usually used by a {@link CompositeWaitStrategy} to reset back to its first wait strategy. This is also used by a backing off
	 * wait strategy to reset its sleep/park time to its initial value. Therefore, for most wait strategies, this method is optional and does
	 * not need to be overridden.
	 */
	protected void resetOperation() {
		
	}
}