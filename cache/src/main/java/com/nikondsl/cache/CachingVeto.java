package com.nikondsl.cache;

public interface CachingVeto<K, V> {
	
	
	default boolean removeAllowed(K key, V value) {
		return true;
	}
	
	default boolean expireAllowed(K key, V value) {
		return true;
	}
	
	default boolean putInCasheAllowed(K key, V value) {
		return true;
	}
}