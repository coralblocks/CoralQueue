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
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.coralblocks.coralqueue.util.MathUtils;

/**
 * A {@link RawBytes} backed by a Java <code>ByteBuffer</code>. 
 */
public abstract class ByteBufferRawBytes implements RawBytes {

    private final ByteBuffer viewBuffer;
    private final int capacityMinusOne;
    private final boolean isPowerOfTwo;
    
    int position;
    int length;
    int pos;
    final int capacity;
    
    ByteBufferRawBytes(ByteBuffer backingBuffer) {
        this.capacity = backingBuffer.capacity();
        this.isPowerOfTwo = MathUtils.isPowerOfTwo(capacity);
        this.capacityMinusOne = capacity - 1;
        this.viewBuffer = backingBuffer.duplicate().order(backingBuffer.order());
        this.viewBuffer.limit(viewBuffer.capacity()).position(0); // to be safe
    }
    
    /**
     * Clear this instance so that it can be re-used
     */
    public void clear() {
    	this.viewBuffer.limit(viewBuffer.capacity()).position(0);
    }
    
    @Override
    public void skipBytes(long bytes) {
    	long newPos = pos + bytes;
    	if (newPos > length) throwSkipException();
    	pos = (int) newPos; // safe to cast because of the check above
    }
    
    abstract void throwSkipException();
    
    @Override
    public long getRemaining() {
        return length - pos;
    }
    
    @Override
    public boolean hasRemaining() {
        return pos < length;
    }
    
    final int calcPos(long sequence) {
    	if (isPowerOfTwo) {
    		return (int) ((sequence - 1) & capacityMinusOne);
    	} else {
    		return (int) ((sequence - 1) % capacity);
    	}
    }
    
    private final int calcRealPos() {
    	
    	int realPos;
    	
    	if (isPowerOfTwo) {
    		realPos = (position + pos) & capacityMinusOne;
    	} else {
    		realPos = (position + pos) % capacity;
    	}
    	
    	// Integer overflow corner case
    	// (for when position is too large and modulus return a negative number)
    	if (realPos < 0) realPos += capacity;
    	
    	return realPos;
    }
    
    @Override
    public byte getByte() {
        if (pos >= length) throw new BufferUnderflowException();
        int realPos = calcRealPos();
        byte b = viewBuffer.get(realPos);
        pos++;
        return b;
    }
    
    @Override
    public short getShort() {
    	
        if (getRemaining() < 2) throw new BufferUnderflowException();
        
        int realPos = calcRealPos();
        
        if (realPos <= capacity - 2) {
            short value = viewBuffer.getShort(realPos);
            pos += 2;
            return value;
        } else {
            if (viewBuffer.order() == ByteOrder.BIG_ENDIAN) {
                return (short) (((getByte() & 0xFF) << 8) | (getByte() & 0xFF));
            } else {
                return (short) ((getByte() & 0xFF) | ((getByte() & 0xFF) << 8));
            }
        }
    }
    
    @Override
    public int getInt() {
    	
        if (getRemaining() < 4) throw new BufferUnderflowException();

        int realPos = calcRealPos();
        
        if (realPos <= capacity - 4) {
            int value = viewBuffer.getInt(realPos);
            pos += 4;
            return value;
        } else {
            if (viewBuffer.order() == ByteOrder.BIG_ENDIAN) {
                return ((getByte() & 0xFF) << 24) | 
                       ((getByte() & 0xFF) << 16) | 
                       ((getByte() & 0xFF) << 8) | 
                       (getByte() & 0xFF);
            } else {
                return (getByte() & 0xFF) | 
                       ((getByte() & 0xFF) << 8) | 
                       ((getByte() & 0xFF) << 16) | 
                       ((getByte() & 0xFF) << 24);
            }
        }
    }
    
    @Override
    public long getLong() {
    	
        if (getRemaining() < 8) throw new BufferUnderflowException();
        
        int realPos = calcRealPos();
        
        if (realPos <= capacity - 8) {
            long value = viewBuffer.getLong(realPos);
            pos += 8;
            return value;
        } else {
            if (viewBuffer.order() == ByteOrder.BIG_ENDIAN) {
                return ((long) (getByte() & 0xFF) << 56) |
                       ((long) (getByte() & 0xFF) << 48) |
                       ((long) (getByte() & 0xFF) << 40) |
                       ((long) (getByte() & 0xFF) << 32) |
                       ((long) (getByte() & 0xFF) << 24) |
                       ((long) (getByte() & 0xFF) << 16) |
                       ((long) (getByte() & 0xFF) << 8) |
                       (getByte() & 0xFF);
            } else {
                return (getByte() & 0xFF) |
                       ((long) (getByte() & 0xFF) << 8) |
                       ((long) (getByte() & 0xFF) << 16) |
                       ((long) (getByte() & 0xFF) << 24) |
                       ((long) (getByte() & 0xFF) << 32) |
                       ((long) (getByte() & 0xFF) << 40) |
                       ((long) (getByte() & 0xFF) << 48) |
                       ((long) (getByte() & 0xFF) << 56);
            }
        }
    }
    
    @Override
    public void getByteArray(byte[] dst, int offset, int len) {
    	
        if (len < 0 || offset < 0 || offset + len > dst.length) {
            throw new IllegalArgumentException();
        }
        
        if (len > getRemaining()) {
            throw new BufferUnderflowException();
        }
        
        int realPos = calcRealPos();
        int contig = Math.min(capacity - realPos, len);
        int remaining = len - contig;
        
        final int origLimit = viewBuffer.limit();
        
        // First segment (contiguous part)
        if (contig > 0) {
            // Set viewBuffer position and limit for contiguous segment
            viewBuffer.position(realPos);
            viewBuffer.limit(realPos + contig);
            viewBuffer.get(dst, offset, contig);
        }
        
        // Second segment (wrapped part)
        if (remaining > 0) {
            // Set viewBuffer position and limit for wrapped segment
            viewBuffer.position(0);
            viewBuffer.limit(remaining);
            viewBuffer.get(dst, offset + contig, remaining);
        }
        
        viewBuffer.limit(origLimit);
        
        pos += len;
    }
    
    @Override
    public void getByteBuffer(ByteBuffer dst, int len) {
    	
        if (len < 0) {
            throw new IllegalArgumentException();
        }
        
        if (len > getRemaining()) {
            throw new BufferUnderflowException();
        }
        
        if (len > dst.remaining()) {
            throw new BufferOverflowException();
        }
        
        int realPos = calcRealPos();
        int contig = Math.min(capacity - realPos, len);
        int remaining = len - contig;
        
        final int origLimit = viewBuffer.limit();
        
        // First segment (contiguous part)
        viewBuffer.position(realPos);
        viewBuffer.limit(realPos + contig);
        dst.put(viewBuffer);
        
        // Second segment (wrapped part)
        if (remaining > 0) {
            viewBuffer.position(0);
            viewBuffer.limit(remaining);
            dst.put(viewBuffer);
        }
        
        viewBuffer.limit(origLimit);
        
        pos += len;
    }
    
    @Override
    public void putByte(byte b) {
        if (pos >= length) throw new BufferOverflowException();
        int realPos = calcRealPos();
        viewBuffer.put(realPos, b);
        pos++;
    }
    
    @Override
    public void putShort(short s) {
    	
        if (getRemaining() < 2) throw new BufferOverflowException();
        
        int realPos = calcRealPos();
        
        if (realPos <= capacity - 2) {
            viewBuffer.putShort(realPos, s);
            pos += 2;
        } else {
            if (viewBuffer.order() == ByteOrder.BIG_ENDIAN) {
                putByte((byte) (s >>> 8));
                putByte((byte) s);
            } else {
                putByte((byte) s);
                putByte((byte) (s >>> 8));
            }
        }
    }
    
    @Override
    public void putInt(int i) {
    	
        if (getRemaining() < 4) throw new BufferOverflowException();
        
        int realPos = calcRealPos();
        
        if (realPos <= capacity - 4) {
            viewBuffer.putInt(realPos, i);
            pos += 4;
        } else {
            if (viewBuffer.order() == ByteOrder.BIG_ENDIAN) {
                putByte((byte) (i >>> 24));
                putByte((byte) (i >>> 16));
                putByte((byte) (i >>> 8));
                putByte((byte) i);
            } else {
                putByte((byte) i);
                putByte((byte) (i >>> 8));
                putByte((byte) (i >>> 16));
                putByte((byte) (i >>> 24));
            }
        }
    }
    
    @Override
    public void putLong(long l) {
    	
        if (getRemaining() < 8) throw new BufferOverflowException();
        
        int realPos = calcRealPos();
        
        if (realPos <= capacity - 8) {
            viewBuffer.putLong(realPos, l);
            pos += 8;
        } else {
            if (viewBuffer.order() == ByteOrder.BIG_ENDIAN) {
                putByte((byte) (l >>> 56));
                putByte((byte) (l >>> 48));
                putByte((byte) (l >>> 40));
                putByte((byte) (l >>> 32));
                putByte((byte) (l >>> 24));
                putByte((byte) (l >>> 16));
                putByte((byte) (l >>> 8));
                putByte((byte) l);
            } else {
                putByte((byte) l);
                putByte((byte) (l >>> 8));
                putByte((byte) (l >>> 16));
                putByte((byte) (l >>> 24));
                putByte((byte) (l >>> 32));
                putByte((byte) (l >>> 40));
                putByte((byte) (l >>> 48));
                putByte((byte) (l >>> 56));
            }
        }
    }
    
    @Override
    public void putByteArray(byte[] src, int offset, int len) {
    	
        if (len < 0 || offset < 0 || offset + len > src.length) {
            throw new IllegalArgumentException();
        }
        
        if (len > getRemaining()) {
            throw new BufferOverflowException();
        }
        
        int realPos = calcRealPos();
        int contig = Math.min(capacity - realPos, len);
        int remaining = len - contig;
        
        int origLimit = viewBuffer.limit();
        
        // First segment (contiguous part)
        if (contig > 0) {
            viewBuffer.position(realPos);
            viewBuffer.limit(realPos + contig);
            viewBuffer.put(src, offset, contig);
        }
        
        // Second segment (wrapped part)
        if (remaining > 0) {
            viewBuffer.position(0);
            viewBuffer.limit(remaining);
            viewBuffer.put(src, offset + contig, remaining);
        }

        viewBuffer.limit(origLimit);
        
        pos += len;
    }
    
    @Override
    public void putByteBuffer(ByteBuffer src, int len) {
    	
        if (len < 0) {
            throw new IllegalArgumentException();
        }
        
        if (len > getRemaining()) {
            throw new BufferOverflowException();
        }
        
        if (len > src.remaining()) {
            throw new BufferOverflowException();
        }
        
        int realPos = calcRealPos();
        int contig = Math.min(capacity - realPos, len);
        
        final int origSrcLimit = src.limit();
        final int origLimit = viewBuffer.limit();
        
        // First segment (contiguous part)
        viewBuffer.position(realPos);
        viewBuffer.limit(realPos + contig);
        int srcLimit = src.position() + contig;
        src.limit(srcLimit);
        viewBuffer.put(src);
        src.limit(origSrcLimit); // restore original limit
        
        // Second segment (wrapped part)
        int remaining = len - contig;
        if (remaining > 0) {
            viewBuffer.position(0);
            viewBuffer.limit(remaining);
            src.limit(src.position() + remaining);
            viewBuffer.put(src);
            src.limit(origSrcLimit); // restore original limit
        }
        
        viewBuffer.limit(origLimit);
        
        pos += len;
    }
}