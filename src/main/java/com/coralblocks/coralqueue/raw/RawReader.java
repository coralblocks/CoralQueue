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

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

class RawReader extends ByteBufferRawBytes {

    private long nextSequenceToRead;
    
    RawReader(ByteBuffer backingBuffer) {
    	super(backingBuffer);
        this.nextSequenceToRead = 1;
    }
    
    @Override
    public final void clear() {
    	super.clear();
    	this.nextSequenceToRead = 1;
    }
    
    final int availableToRead(long nextSequenceToWrite) {
    	
    	length = (int) (nextSequenceToWrite - nextSequenceToRead);
    	
    	return length;
    }
    
    final void resetPosition() {
    	
        this.position = calcPos(nextSequenceToRead);
        
        this.pos = 0; // we are always zero-based for clarity
    }
    
    long updateReadSequence() {
    	nextSequenceToRead += pos;
    	return nextSequenceToRead;
    }
    
    @Override
    final void throwSkipException() {
    	throw new BufferUnderflowException();
    }
    
    @Override
    public void putByte(byte b) {
        throw new UnsupportedOperationException("Read only");
    }
    
    @Override
    public void putShort(short s) {
        throw new UnsupportedOperationException("Read only");
    }
    
    @Override
    public void putInt(int i) {
        throw new UnsupportedOperationException("Read only");
    }
    
    @Override
    public void putLong(long l) {
        throw new UnsupportedOperationException("Read only");
    }
    
    @Override
    public void putByteArray(byte[] src, int offset, int len) {
        throw new UnsupportedOperationException("Read only");
    }
    
    @Override
    public void putByteBuffer(ByteBuffer src, int len) {
        throw new UnsupportedOperationException("Read only");
    }
}