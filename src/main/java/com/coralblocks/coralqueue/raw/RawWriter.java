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
package com.coralblocks.coralqueue.raw;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;

class RawWriter extends ByteBufferRawBytes {

    private long nextSequenceToWrite;
    
    RawWriter(ByteBuffer backingBuffer) {
    	super(backingBuffer);
        this.nextSequenceToWrite = 1;
    }
    
    @Override
    public final void clear() {
    	super.clear();
    	this.nextSequenceToWrite = 1;
    }
    
    final int availableToWrite(long nextSequenceToRead) {
    	
    	final int toBeRead = (int) (nextSequenceToWrite - nextSequenceToRead);
    	
    	length = capacity - toBeRead; // save it here!
    	
    	return length;
    }
    
    final void resetPosition() {
    	
    	 this.position = calcPos(nextSequenceToWrite);
         
         this.pos = 0; // we are always zero-based for clarity
    }
    
    long updateWriteSequence() {
    	nextSequenceToWrite += pos;
    	return nextSequenceToWrite;
    }
    
    @Override
    final void throwSkipException() {
    	throw new BufferOverflowException();
    }
    
	@Override
	public byte getByte() {
		throw new UnsupportedOperationException("Write only");
	}

	@Override
	public short getShort() {
		throw new UnsupportedOperationException("Write only");
	}

	@Override
	public int getInt() {
		throw new UnsupportedOperationException("Write only");
	}

	@Override
	public long getLong() {
		throw new UnsupportedOperationException("Write only");
	}

	@Override
	public void getByteArray(byte[] dst, int offset, int len) {
		throw new UnsupportedOperationException("Write only");
	}

	@Override
	public void getByteBuffer(ByteBuffer dst, int len) {
		throw new UnsupportedOperationException("Write only");
	}
    
}