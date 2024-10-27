/*
* Copyright (c) CoralBlocks LLC (c) 2017
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