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

import com.coralblocks.coralqueue.util.PaddedAtomicLong;

/**
 * An implementation of {@link RawQueue} that uses a {@link ByteBufferRawBytes}. 
 */
public class ByteBufferRawQueue implements RawQueue {
	
	/**
	 * The default capacity of the ByteBuffer (in bytes)
	 */
	public static final int DEFAULT_CAPACITY = 1024;
	
	/**
	 * The default isDirect for the ByteBuffer
	 */
	public static final boolean DEFAULT_DIRECT = true;

	private final ByteBuffer data;
	private final PaddedAtomicLong nextSequenceToWrite = new PaddedAtomicLong(1);
	private final PaddedAtomicLong nextSequenceToRead = new PaddedAtomicLong(1);
	private final RawReader rawReader;
	private final RawWriter rawWriter;
	
	/**
	 * Creates a new <code>ByteBufferRawQueue</code>.
	 * 
	 * @param capacity the capacity of the queue (and the underlying <code>ByteBuffer</code>)
	 * @param isDirect is the <code>ByteBuffer</code> used by this queue direct or in the heap?
	 */
	public ByteBufferRawQueue(int capacity, boolean isDirect) {
		this.data = isDirect ? ByteBuffer.allocateDirect(capacity) : ByteBuffer.allocate(capacity);
		this.rawReader = new RawReader(data);
		this.rawWriter = new RawWriter(data);
	}
	
	/**
	 * Creates a new <code>ByteBufferRawQueue</code> with the default isDirect for the <code>ByteBuffer</code>.
	 * 
	 * @param capacity the capacity of the queue (and the underlying <code>ByteBuffer</code>)
	 */
	public ByteBufferRawQueue(int capacity) {
		this(capacity, DEFAULT_DIRECT);
	}
	
	/**
	 * Creates a new <code>ByteBufferRawQueue</code> with the default size for the <code>ByteBuffer</code>.
	 * 
	 * @param isDirect is the <code>ByteBuffer</code> used by this queue direct or in the heap?
	 */
	public ByteBufferRawQueue(boolean isDirect) {
		this(DEFAULT_CAPACITY, isDirect);
	}
	
	/**
	 * Creates a new <code>ByteBufferRawQueue</code> with the default size for the <code>ByteBuffer</code> and 
	 * the default isDirect for the <code>ByteBuffer</code>.
	 */
	public ByteBufferRawQueue() {
		this(DEFAULT_CAPACITY, DEFAULT_DIRECT);
	}

	@Override
	public final void clear() {
		rawReader.clear();
		rawWriter.clear();
		nextSequenceToWrite.set(1);
		nextSequenceToRead.set(1);
	}
	
	@Override
	public final long availableToWrite() {
		return rawWriter.availableToWrite(nextSequenceToRead.get());
	}
	
	@Override
	public final RawBytes getProducer() {
		rawWriter.resetPosition();
		return rawWriter;
	}
	
	@Override
	public final long availableToRead() {
		return rawReader.availableToRead(nextSequenceToWrite.get());
	}
	
	@Override
	public final RawBytes getConsumer() {
		rawReader.resetPosition();
		return rawReader;
	}

	@Override
	public final void flush(boolean lazySet) {
		long writeSequence = rawWriter.updateWriteSequence();
		if (lazySet) {
			this.nextSequenceToWrite.lazySet(writeSequence);
		} else {
			this.nextSequenceToWrite.set(writeSequence);
		}
	}
	
	@Override
	public final void flush() {
		long writeSequence = rawWriter.updateWriteSequence();
		this.nextSequenceToWrite.set(writeSequence);
	}

	@Override
	public final void doneReading(boolean lazySet) {
		long readSequence = rawReader.updateReadSequence();
		if (lazySet) {
			this.nextSequenceToRead.lazySet(readSequence);
		} else {
			this.nextSequenceToRead.set(readSequence);
		}
	}
	
	@Override
	public final void doneReading() {
		long readSequence = rawReader.updateReadSequence();
		this.nextSequenceToRead.set(readSequence);
	}
}