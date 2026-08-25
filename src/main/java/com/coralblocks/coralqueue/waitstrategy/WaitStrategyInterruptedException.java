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
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.coralblocks.coralqueue.waitstrategy;

/**
 * Thrown when a wait strategy observes that its thread has been interrupted.
 * The thread's interrupt status remains set when this exception is thrown.
 */
public class WaitStrategyInterruptedException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public WaitStrategyInterruptedException() {
		super("Wait strategy was interrupted");
	}

	public WaitStrategyInterruptedException(InterruptedException cause) {
		super("Wait strategy was interrupted", cause);
	}
}
