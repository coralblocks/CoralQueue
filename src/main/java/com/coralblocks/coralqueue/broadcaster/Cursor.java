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
	
	private long fetchCount = 0;
	private long lastFetchedSeq = 0;
	private final PaddedAtomicLong fetchSequence = new PaddedAtomicLong(0);
	
	final void clear() {
		fetchCount = 0;
		lastFetchedSeq = 0;
		fetchSequence.set(lastFetchedSeq);
	}
	
	final long getLastFetchedSeq() {
		return lastFetchedSeq;
	}
	
	final long incrementLastFetchedSeq() {
		return ++lastFetchedSeq;
	}
	
	final long getFetchCount() {
		return fetchCount;
	}
	
	final void incrementFetchCount() {
		fetchCount++;
	}
	
	final void resetFetchCount() {
		fetchCount = 0;
	}
	
	final void decrementFetchCount(long x) {
		fetchCount -= x;
	}
	
	final void decrementLastFetchedSeq(long x) {
		lastFetchedSeq -= x;
	}
	
	final void updateFetchSequence(boolean lazySet) {
		if (lazySet) {
			fetchSequence.lazySet(lastFetchedSeq);
		} else {
			fetchSequence.set(lastFetchedSeq);
		}
	}
	
	final long getFetchSequence() {
		return fetchSequence.get();
	}
	
	final void setFetchSequenceToMax() {
		fetchSequence.set(Long.MAX_VALUE);
	}
}