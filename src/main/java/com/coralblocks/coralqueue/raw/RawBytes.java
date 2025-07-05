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

import java.nio.ByteBuffer;

/**
 * An interface to write and read raw bytes from/to a contiguous piece of memory.
 * It should maintain an internal position pointer for reading/writing.
 */
public interface RawBytes {
	
	/**
	 * Return the number of bytes remaining that can be read from the contiguous piece of memory
	 * OR
	 * the size in bytes of the remaining space in the contiguous piece of memory that you can write bytes to
	 * 
	 * @return the number of bytes remaining
	 */
	public long getRemaining();
	
	/**
	 * Return true if there are remaining bytes to be read
	 * OR
	 * remaining space to be written
	 * 
	 * @return true if there are bytes/space remaining
	 */
	public boolean hasRemaining();
	
	/**
	 * Skip some bytes or space in bytes instead of reading from or writing to
	 * 
	 * @param bytes the number of byte/space to skip
	 */
	public void skipBytes(long bytes);
	
	/**
	 * Write a byte to memory
	 * 
	 * @param b the byte to write
	 */
	public void putByte(byte b);
	
	/**
	 * Write a short to memory
	 * 
	 * @param s the short to write
	 */
	public void putShort(short s);
	
	/**
	 * Write an in to memory
	 * 
	 * @param i the int to write
	 */
	public void putInt(int i);
	
	/**
	 * Write a long to memory
	 * 
	 * @param l the long to write
	 */
	public void putLong(long l);
	
	/**
	 * Read a byte from memory
	 * 
	 * @return the byte read
	 */
	public byte getByte();
	
	/**
	 * Read a short from memory
	 * 
	 * @return the short read
	 */
	public short getShort();
	
	/**
	 * Read an int from memory
	 * 
	 * @return the int read
	 */
	public int getInt();
	
	/**
	 * Read a long from memory
	 * 
	 * @return the long read
	 */
	public long getLong();
	
	/**
	 * Write a byte array to memory
	 * 
	 * @param src the source byte array to write
	 * @param offset the offset into the source byte array
	 * @param len the length to write from the source byte array
	 */
	public void putByteArray(byte[] src, int offset, int len);
	
	/**
	 * Read from memory into a byte array
	 * 
	 * @param dst the destination byte array to receive the bytes read from memory
	 * @param offset the offset into the destination byte array
	 * @param len the length to read into the destination byte array
	 */
	public void getByteArray(byte[] dst, int offset, int len);
	
	/**
	 * Write a <code>ByteBuffer</code> to memory
	 * 
	 * @param src the source <code>ByteBuffer</code> to write to memory
	 * @param len the length to write from the source <code>ByteBuffer</code>
	 */
	public void putByteBuffer(ByteBuffer src, int len);
	
	/**
	 * Read from memory into a <code>ByteBuffer</code>
	 * 
	 * @param dst the destination <code>ByteBuffer</code> that will receive the bytes read
	 * @param len the length to read into the destination <code>ByteBuffer</code>
	 */
	public void getByteBuffer(ByteBuffer dst, int len);
}