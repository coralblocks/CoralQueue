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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.coralblocks.coralqueue.example.raw.Basics.Consumer;
import com.coralblocks.coralqueue.example.raw.Basics.Producer;

public class ByteBufferRawQueueTest {

	private static final int BYTE_ORDER_TEST_VALUE = 0x01020304;

	@Test
	public void testDefaultsToNativeByteOrder() {
		assertByteOrder(new ByteBufferRawQueue(8, false), ByteOrder.nativeOrder());
	}

	@Test
	public void testByteOrderCanBeConfigured() {
		assertByteOrder(new ByteBufferRawQueue(8, false, ByteOrder.BIG_ENDIAN), ByteOrder.BIG_ENDIAN);
		assertByteOrder(new ByteBufferRawQueue(8, false, ByteOrder.LITTLE_ENDIAN), ByteOrder.LITTLE_ENDIAN);
	}

	private static void assertByteOrder(RawQueue queue, ByteOrder byteOrder) {
		byte[] expected = ByteBuffer.allocate(Integer.BYTES).order(byteOrder)
				.putInt(BYTE_ORDER_TEST_VALUE).array();

		queue.getProducer().putInt(BYTE_ORDER_TEST_VALUE);
		queue.flush();

		byte[] actual = new byte[Integer.BYTES];
		queue.getConsumer().getByteArray(actual, 0, actual.length);
		Assert.assertArrayEquals(expected, actual);

		queue.clear();
		queue.getProducer().putByteArray(expected, 0, expected.length);
		queue.flush();
		Assert.assertEquals(BYTE_ORDER_TEST_VALUE, queue.getConsumer().getInt());
	}

	@Test
	public void testGetProducerRefreshesWritableLength() {

		RawQueue queue = new ByteBufferRawQueue(16, false);

		Assert.assertEquals(16, queue.availableToWrite());
		queue.getProducer().putLong(123L);
		queue.flush();

		RawBytes producer = queue.getProducer();
		Assert.assertEquals(8, producer.getRemaining());
		producer.putLong(456L);

		try {
			producer.putLong(789L);
			Assert.fail("Expected BufferOverflowException");
		} catch(BufferOverflowException expected) {
			// expected
		}
	}

	@Test
	public void testGetConsumerRefreshesReadableLength() {

		RawQueue queue = new ByteBufferRawQueue(16, false);

		Assert.assertEquals(16, queue.availableToWrite());
		RawBytes producer = queue.getProducer();
		producer.putLong(123L);
		producer.putLong(456L);
		queue.flush();

		Assert.assertEquals(16, queue.availableToRead());
		Assert.assertEquals(123L, queue.getConsumer().getLong());
		queue.doneReading();

		RawBytes consumer = queue.getConsumer();
		Assert.assertEquals(8, consumer.getRemaining());
		Assert.assertEquals(456L, consumer.getLong());

		try {
			consumer.getLong();
			Assert.fail("Expected BufferUnderflowException");
		} catch(BufferUnderflowException expected) {
			// expected
		}
	}

	@Test
	public void testFlushIsIdempotent() {

		RawQueue queue = new ByteBufferRawQueue(16, false);

		Assert.assertEquals(16, queue.availableToWrite());
		queue.getProducer().putLong(123L);
		queue.flush();
		queue.flush();

		Assert.assertEquals(8, queue.availableToRead());
		Assert.assertEquals(123L, queue.getConsumer().getLong());
	}

	@Test
	public void testDoneReadingIsIdempotent() {

		RawQueue queue = new ByteBufferRawQueue(16, false);

		Assert.assertEquals(16, queue.availableToWrite());
		RawBytes producer = queue.getProducer();
		producer.putLong(123L);
		producer.putLong(456L);
		queue.flush();

		Assert.assertEquals(16, queue.availableToRead());
		Assert.assertEquals(123L, queue.getConsumer().getLong());
		queue.doneReading();
		queue.doneReading();

		Assert.assertEquals(8, queue.availableToRead());
		Assert.assertEquals(8, queue.availableToWrite());
		Assert.assertEquals(456L, queue.getConsumer().getLong());
	}
	
	@Test
	public void testAll() throws InterruptedException {
		
		final int messagesToSend = 100000;
		final int batchSizeToSend = 100;
		
		RawQueue queue = new ByteBufferRawQueue();
		
		Producer producer = new Producer(queue, messagesToSend, batchSizeToSend);
		Consumer consumer = new Consumer(queue);
		
		producer.start();
		consumer.start();
		
		producer.join();
		consumer.join();
		
		List<Long> messagesReceived = consumer.getMessagesReceived();
		List<Long> batchesReceived = consumer.getBatchesReceived();
		
		// Did we receive all messages?
		Assert.assertEquals(messagesToSend, messagesReceived.size());
		
		// Where there any duplicates?
		Assert.assertEquals(messagesReceived.size(), messagesReceived.stream().distinct().count());
		
		// Were the messages received in order?
		List<Long> sortedList = new ArrayList<Long>(messagesReceived);
		Collections.sort(sortedList);
		Assert.assertEquals(messagesReceived, sortedList);
		
		// If we sum all batches do we get the correct number of messages?
		long sumOfAllBatches = batchesReceived.stream().mapToLong(Long::longValue).sum();
		Assert.assertEquals(messagesToSend, sumOfAllBatches);
	}
}
