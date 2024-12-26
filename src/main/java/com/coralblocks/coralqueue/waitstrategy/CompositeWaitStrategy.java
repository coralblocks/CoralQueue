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
 * <p>This class can be conveniently used to create wait strategies by combining other wait strategies.</p>
 * 
 * <p>For example, to create a wait strategy that first busy spins and then starts to sleep, you can easily combine the
 * {@link BusySpinWaitStrategy} and the {@link SleepWaitStrategy} to accomplish that.</p>
 * 
 * <p>Note that composite wait strategies can even contain other composite wait strategies.</p>
 */
public class CompositeWaitStrategy implements WaitStrategy {
	
	private final WaitStrategy[] waitStrategies;
	private int currIndex = 0;
	
	public CompositeWaitStrategy(WaitStrategy ... waitStrategies) {
		this.waitStrategies = waitStrategies;
	}
	
	@Override
	public void addListener(WaitStrategyListener listener) {
		for(WaitStrategy ws : waitStrategies) {
			ws.addListener(listener);
		}
	}
	
	@Override
	public void removeListener(WaitStrategyListener listener) {
		for(WaitStrategy ws : waitStrategies) {
			ws.removeListener(listener);
		}
	}

	@Override
	public boolean await() {
		WaitStrategy waitStrategy = waitStrategies[currIndex];
		boolean done = waitStrategy.await();
		if (!done) return false; // still going...
		else if (currIndex < waitStrategies.length - 1) { // is it not the last one?
			// move on to next wait strategy
			currIndex++;
			return false; // still going
		} else {
			return true; // done
		}
	}
	
	@Override
	public void reset() {
		for(WaitStrategy ws : waitStrategies) {
			ws.reset();
		}
		currIndex = 0;
	}
}