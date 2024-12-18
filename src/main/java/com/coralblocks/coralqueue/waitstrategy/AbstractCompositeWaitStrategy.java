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
 * <p>An abstract implementation of the {@link WaitStrategy} interface that you can as the base class for
 * your <i>composite</i> wait strategy implementations. It takes care of most of the boilerplate code like registering and
 * unregistering listeners, calling the listeners, counting the number of blockings, returning false from
 * <code>block()</code>, etc.</p>
 * 
 * <p>By inheriting from this abstract base class, all you have to do is implement {@link getCompositeWaitStrategy()} to return
 * the actual {@link CompositeWaitStrategy} that your class will contain through composition.</p>
 */
public abstract class AbstractCompositeWaitStrategy implements WaitStrategy {
	
	/**
	 * Subclasses will usually create their composite wait strategy inside their constructor to be returned by this method.
	 * 
	 * @return the {@link CompositeWaitStrategy} used by this composite wait strategy through composition
	 */
	protected abstract CompositeWaitStrategy getCompositeWaitStrategy();
	
	@Override
	public final void addListener(WaitStrategyListener listener) {
		getCompositeWaitStrategy().addListener(listener);
	}
	
	@Override
	public final void removeListener(WaitStrategyListener listener) {
		getCompositeWaitStrategy().removeListener(listener);
	}
	
	@Override
	public final boolean block() {
		return getCompositeWaitStrategy().block();
	}

	@Override
	public final void reset() {
		getCompositeWaitStrategy().reset();
	}
}