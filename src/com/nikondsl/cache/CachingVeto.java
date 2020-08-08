package com.nikondsl.cache;

public interface CachingVeto<K, V> {
	boolean removeAllowed(K key, V value);
	boolean expireAllowed(K key, V value);
}