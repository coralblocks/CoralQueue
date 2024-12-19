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
package com.coralblocks.coralqueue.waitstrategy;

/**
 * <p>A <code>WaitStrategy</code> interface describing how a producer and/or a consumer can choose to wait (i.e. block) while 
 * its CoralQueue data structure is full/empty.</p>
 * 
 * <p>Note that it is not mandatory to use a <code>WaitStrategy</code> as producers and consumers can simply choose to busy spin. 
 * However, as a CPU core is a scarce resource, there can be situations where you will not want to waste clock cycles by busy spinning.</p>
 */
public interface WaitStrategy {
	
	/**
	 * <p>Do something to wait: busy spin, busy sleep, park, yield, sleep, back off, etc.</p>
	 * 
	 * <p>It can return true to indicate that this wait strategy has finished.
	 * This is important to signal to a {@link CompositeWaitStrategy} to switch to the next wait strategy in its list.</p>
	 * 
	 * <p>This method can be called multiple times before {@link #reset()} is finally called.</p>
	 * 
	 * @return true if this wait strategy has finished
	 */
	public boolean block();
	
	/**
	 * <p>This method is used to indicate that after blocking for one or several times, we finally were able to accomplish what we were waiting for
	 * and we can now reset the state of the waiting strategy to get ready for another cycle. This is important for backing off wait strategies that
	 * increase their sleep/park time after each blocking. After <code>reset()</code> is called they will reset their sleep/park time to their initial value.
	 * A {@link CompositeWaitStrategy} also resets its current wait strategy to the first one in its list.</p>
	 */
	public void reset();
	
	/**
	 * <p>This is an optional operation to register a {@link WaitStrategyListener} to receive callbacks from <code>block()</code> and <code>reset()</code>.
	 * Note that all our provided wait strategies implement this method but when you are implementing your own wait strategies you might choose not to provide
	 * this functionality, if you will not be registering any listener to your wait strategy.</p>
	 * 
	 * @param listener the {@link WaitStrategyListener} to register
	 */
	default public void addListener(WaitStrategyListener listener) {
		throw new UnsupportedOperationException();
	}
	
	/**
	 * <p>This is an optional operation to unregister a {@link WaitStrategyListener} to receive callbacks from <code>block()</code> and <code>reset()</code>.
	 * Note that all our provided wait strategies implement this method but when you are implementing your own wait strategies you might choose not to provide
	 * this functionality, if you will not be registering any listener to your wait strategy.</p>
	 * 
	 * @param listener the {@link WaitStrategyListener} to unregister
	 */
	default public void removeListener(WaitStrategyListener listener) {
		throw new UnsupportedOperationException();
	}
}
