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
 * A wait strategy that calls <code>Thread.yield()</code> as its blocking operation.
 */
public class YieldWaitStrategy extends AbstractWaitStrategy {
	
	public YieldWaitStrategy(long maxBlockCount) {
		super(maxBlockCount);
	}
	
	public YieldWaitStrategy() {
		this(DEFAULT_MAX_BLOCK_COUNT);
	}

	@Override
	protected final void blockOperation() {
		Thread.yield();
	}
}