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
package com.coralblocks.coralqueue;

import org.junit.Assert;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;

import com.coralblocks.coralqueue.broadcaster.AtomicBroadcaster;
import com.coralblocks.coralqueue.broadcaster.Broadcaster;
import com.coralblocks.coralqueue.broadcaster.BroadcasterDelegateQueue;
import com.coralblocks.coralqueue.demultiplexer.AtomicDemultiplexer;
import com.coralblocks.coralqueue.demultiplexer.Demultiplexer;
import com.coralblocks.coralqueue.diamond.AtomicDiamond;
import com.coralblocks.coralqueue.diamond.Task;
import com.coralblocks.coralqueue.mpmc.AtomicMpMc;
import com.coralblocks.coralqueue.mpmc.MpMc;
import com.coralblocks.coralqueue.mpmcbroadcaster.AtomicMpMcBroadcaster;
import com.coralblocks.coralqueue.mpmcbroadcaster.MpMcBroadcaster;
import com.coralblocks.coralqueue.multiplexer.AtomicMultiplexer;
import com.coralblocks.coralqueue.multiplexer.Multiplexer;
import com.coralblocks.coralqueue.queue.AtomicQueue;
import com.coralblocks.coralqueue.queue.Queue;
import com.coralblocks.coralqueue.util.MutableLong;

public class ArgumentValidationTest {

	public static class ValidationTask extends Task {

		@Override
		public boolean execute() {
			return true;
		}
	}

	@Test
	public void testCountsMustBePositive() {
		assertIllegalArgument(() -> new AtomicBroadcaster<MutableLong>(MutableLong.class, 0));
		assertIllegalArgument(() -> new AtomicBroadcaster<MutableLong>(MutableLong.class, -1));
		assertIllegalArgument(() -> new AtomicDemultiplexer<MutableLong>(MutableLong.class, 0));
		assertIllegalArgument(() -> new AtomicDemultiplexer<MutableLong>(MutableLong.class, -1));
		assertIllegalArgument(() -> new AtomicMultiplexer<MutableLong>(MutableLong.class, 0));
		assertIllegalArgument(() -> new AtomicMultiplexer<MutableLong>(MutableLong.class, -1));
		assertIllegalArgument(() -> new AtomicMpMc<MutableLong>(MutableLong.class, 0, 1));
		assertIllegalArgument(() -> new AtomicMpMc<MutableLong>(MutableLong.class, -1, 1));
		assertIllegalArgument(() -> new AtomicMpMc<MutableLong>(MutableLong.class, 1, 0));
		assertIllegalArgument(() -> new AtomicMpMc<MutableLong>(MutableLong.class, 1, -1));
		assertIllegalArgument(() -> new AtomicMpMcBroadcaster<MutableLong>(MutableLong.class, 0, 1));
		assertIllegalArgument(() -> new AtomicMpMcBroadcaster<MutableLong>(MutableLong.class, -1, 1));
		assertIllegalArgument(() -> new AtomicMpMcBroadcaster<MutableLong>(MutableLong.class, 1, 0));
		assertIllegalArgument(() -> new AtomicMpMcBroadcaster<MutableLong>(MutableLong.class, 1, -1));
		assertIllegalArgument(() -> new AtomicDiamond<ValidationTask>(ValidationTask.class, 0));
		assertIllegalArgument(() -> new AtomicDiamond<ValidationTask>(ValidationTask.class, -1));
	}

	@Test
	public void testBroadcasterIndexesAreValidated() {
		Broadcaster<MutableLong> broadcaster = new AtomicBroadcaster<MutableLong>(MutableLong.class, 1);

		assertIndexOutOfBounds(() -> broadcaster.getConsumer(-1));
		assertIndexOutOfBounds(() -> broadcaster.getConsumer(1));
		assertIndexOutOfBounds(() -> broadcaster.disableConsumer(-1));
		assertIndexOutOfBounds(() -> broadcaster.availableToFetch(-1));
		assertIndexOutOfBounds(() -> broadcaster.fetch(-1));
		assertIndexOutOfBounds(() -> broadcaster.doneFetching(-1));
		assertIndexOutOfBounds(() -> broadcaster.rollBack(-1));
		assertIndexOutOfBounds(() -> new BroadcasterDelegateQueue<MutableLong>(broadcaster, -1));
		assertIndexOutOfBounds(() -> new BroadcasterDelegateQueue<MutableLong>(broadcaster, 1));
	}

	@Test
	public void testDemultiplexerAndMultiplexerIndexesAreValidated() {
		Demultiplexer<MutableLong> demux = new AtomicDemultiplexer<MutableLong>(MutableLong.class, 1);
		Multiplexer<MutableLong> mux = new AtomicMultiplexer<MutableLong>(MutableLong.class, 1);

		assertIndexOutOfBounds(() -> demux.getConsumer(-1));
		assertIndexOutOfBounds(() -> demux.getConsumer(1));
		assertIndexOutOfBounds(() -> demux.nextToDispatch(-1));
		assertIndexOutOfBounds(() -> demux.nextToDispatch(1));
		assertIndexOutOfBounds(() -> demux.availableToFetch(-1));
		assertIndexOutOfBounds(() -> demux.fetch(-1));
		assertIndexOutOfBounds(() -> demux.replace(-1, new MutableLong()));
		assertIndexOutOfBounds(() -> demux.doneFetching(-1));

		assertIndexOutOfBounds(() -> mux.getProducer(-1));
		assertIndexOutOfBounds(() -> mux.getProducer(1));
		assertIndexOutOfBounds(() -> mux.nextToDispatch(-1));
		assertIndexOutOfBounds(() -> mux.nextToDispatch(-1, new MutableLong()));
		assertIndexOutOfBounds(() -> mux.flush(-1));
	}

	@Test
	public void testCompositeIndexesAreValidated() {
		MpMc<MutableLong> mpmc = new AtomicMpMc<MutableLong>(MutableLong.class, 1, 1);
		MpMcBroadcaster<MutableLong> broadcaster = new AtomicMpMcBroadcaster<MutableLong>(MutableLong.class, 1, 1);

		assertIndexOutOfBounds(() -> mpmc.getProducer(-1));
		assertIndexOutOfBounds(() -> mpmc.getProducer(1));
		assertIndexOutOfBounds(() -> mpmc.getConsumer(-1));
		assertIndexOutOfBounds(() -> mpmc.getConsumer(1));
		assertIndexOutOfBounds(() -> mpmc.nextToDispatch(0, -1));
		assertIndexOutOfBounds(() -> mpmc.nextToDispatch(0, 1));

		assertIndexOutOfBounds(() -> broadcaster.getProducer(-1));
		assertIndexOutOfBounds(() -> broadcaster.getProducer(1));
		assertIndexOutOfBounds(() -> broadcaster.getConsumer(-1));
		assertIndexOutOfBounds(() -> broadcaster.getConsumer(1));
		assertIndexOutOfBounds(() -> broadcaster.disableConsumer(-1));
	}

	@Test
	public void testInvalidRollbackUsesIllegalArgumentException() {
		Queue<MutableLong> queue = new AtomicQueue<MutableLong>(MutableLong.class);
		Broadcaster<MutableLong> broadcaster = new AtomicBroadcaster<MutableLong>(MutableLong.class, 1);

		assertIllegalArgument(() -> queue.rollBack(-1));
		assertIllegalArgument(() -> broadcaster.rollBack(0, -1));
	}

	private static void assertIllegalArgument(ThrowingRunnable action) {
		IllegalArgumentException exception = Assert.assertThrows(IllegalArgumentException.class, action);
		Assert.assertEquals(IllegalArgumentException.class, exception.getClass());
	}

	private static void assertIndexOutOfBounds(ThrowingRunnable action) {
		IndexOutOfBoundsException exception = Assert.assertThrows(IndexOutOfBoundsException.class, action);
		Assert.assertEquals(IndexOutOfBoundsException.class, exception.getClass());
	}
}
