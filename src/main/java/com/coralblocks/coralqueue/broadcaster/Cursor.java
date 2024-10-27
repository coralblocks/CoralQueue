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
package com.coralblocks.coralqueue.broadcaster;

import com.coralblocks.coralqueue.util.PaddedAtomicLong;

class Cursor {
	
	private long pollCount = 0;
	private long lastPolledSeq = -1;
	private final PaddedAtomicLong pollSequence = new PaddedAtomicLong(-1);
	
	final void clear() {
		pollCount = 0;
		lastPolledSeq = -1;
		pollSequence.set(-1);
	}
	
	final long getLastPolledSeq() {
		return lastPolledSeq;
	}
	
	final long incrementLastPolledSeq() {
		return ++lastPolledSeq;
	}
	
	final long getPollCount() {
		return pollCount;
	}
	
	final void incrementPollCount() {
		pollCount++;
	}
	
	final void resetPollCount() {
		pollCount = 0;
	}
	
	final void decrementPollCount(long x) {
		pollCount -= x;
	}
	
	final void decrementLastPolledSeq(long x) {
		lastPolledSeq -= x;
	}
	
	final void updatePollSequence(boolean lazySet) {
		if (lazySet) {
			pollSequence.lazySet(lastPolledSeq);
		} else {
			pollSequence.set(lastPolledSeq);
		}
	}
	
	final long getPollSequence() {
		return pollSequence.get();
	}
	
	final void setPollSequenceToMax() {
		pollSequence.set(Long.MAX_VALUE);
	}
}