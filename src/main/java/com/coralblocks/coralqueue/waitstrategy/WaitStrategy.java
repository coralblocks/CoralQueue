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
 * Describes a wait strategy.
 * 
* 
 */
public interface WaitStrategy {
	
	/**
	 * Do something to wait: busy spin, park, yield, sleep, back off, etc.
	 */
	public void block();
	
	/**
	 * Reset the strategy because we have waited and we have accomplished what we were waiting for.
	 */
	public void reset();
	
	/**
	 * Optional method to return the number of times this wait strategy has blocked.
	 * 
	 * NOTE: To activate pass -DblockCount=true
	 * 
	 * @return the number of blocks or -1 if blocking is not activated.
	 */
	public long getTotalBlockCount();
	
	/**
	 * Optional method to reset the total block count number to zero.
	 * 
	 * NOTE: To activate pass -DblockCount=true
	 */
	public void resetTotalBlockCount();
}
