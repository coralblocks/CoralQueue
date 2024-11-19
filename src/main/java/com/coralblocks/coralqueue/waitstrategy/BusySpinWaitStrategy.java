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
 * A wait strategy that does nothing as its blocking operation, in other words, it simply busy spins.
 */
public class BusySpinWaitStrategy extends AbstractWaitStrategy {
	
	public BusySpinWaitStrategy(long maxBlockCount) {
		super(maxBlockCount);
	}
	
	public BusySpinWaitStrategy() {
		this(DEFAULT_MAX_BLOCK_COUNT);
	}

	@Override
	protected final void blockOperation() {
		// busy spinning so do nothing
	}
}