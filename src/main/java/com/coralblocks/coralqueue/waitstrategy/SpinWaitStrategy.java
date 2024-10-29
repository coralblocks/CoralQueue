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
 * <p>A wait strategy that simply performs busy spinning. Its string type for the factory method {@link WaitStrategy#getWaitStrategy(String)} is "spin".</p>
 * 
 * <p>NOTE: You can optionally pass -DcoralQueueBlockCount=true to count the total number of blocks.</p>
 */
public class SpinWaitStrategy implements WaitStrategy {
	
	private final BlockCount blockCount = new BlockCount();
	
	/**
	 * Creates a new <code>SpinWaitStrategy</code>.
	 */
	public SpinWaitStrategy() {
		
	}

	@Override
	public final void block() {
		blockCount.increment();
	}

	@Override
	public final void reset() {
		blockCount.reset();
	}
	
	@Override
	public final long getTotalBlockCount() {
		return blockCount.getTotalBlockCount();
	}
	
	@Override
	public final void resetTotalBlockCount() {
		blockCount.resetTotalBlockCount();
	}

}
