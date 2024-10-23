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
package com.coralblocks.coralqueue.util;

import java.util.Iterator;

/**
 * An garbage-free identity hash map backed by a {@link LongMap} using <code>System.identityHashCode(keyObject)</code> has hashes.
 * 
 * @param <K> the key object to use
 * @param <V> the value object to use
 */
public class IdentityMap<K, V> implements Iterable<V> {
	
	private static final int INITIAL_CAPACITY = 64;
	private static final float LOAD_FACTOR = 0.75f;
	
	private final LongMap<V> map;
	
	public IdentityMap() {
		this(INITIAL_CAPACITY);
	}
	
	public IdentityMap(int initialCapacity) {
		this(initialCapacity, LOAD_FACTOR);
	}
	
	public IdentityMap(int initialCapacity, float loadFactor) {
		this.map = new LongMap<V>(initialCapacity, loadFactor);
	}
	
	public int size() {
		return map.size();
	}
	
	public boolean isEmpty() {
		return map.isEmpty();
	}
	
	public boolean contains(V v) {
		return map.contains(v);
	}
	
	private final int id(K k) {
		return System.identityHashCode(k);
	}
	
	public boolean containsKey(K k) {
		return map.containsKey(id(k));
	}
	
	public V get(K k) {
		return map.get(id(k));
	}
	
	public V put(K k, V v) {
		return map.put(id(k), v);
	}
	
	public V remove(K k) {
		return map.remove(id(k));
	}
	
	public void clear() {
		map.clear();
	}
	
	@Override
	public Iterator<V> iterator() {
		return map.iterator();
	}
}