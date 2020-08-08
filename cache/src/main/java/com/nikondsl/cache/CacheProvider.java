package com.nikondsl.cache;

import java.util.Map;

public interface CacheProvider<K, V> {
	String getName();
	
	V get(K key);
	
	default void put(K key, V value) {
		putIfAbsent(key, value);
	}
	
	V putIfAbsent(K key, V value);
	
	V remove(K key);
	
	Iterable<Map.Entry<K, V>> getEntries();
}
