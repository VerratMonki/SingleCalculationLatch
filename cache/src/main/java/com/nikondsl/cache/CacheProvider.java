package com.nikondsl.cache;

import org.ehcache.Cache;

import java.util.Map;
import java.util.function.Consumer;

/**
 * This class provides cache common interface. Key should support valid equals/hashCode methods.
 * Note: main method for putting element into cache is atomic putIfAbsent, like it is in ConcurrentMap.
 * If atomic for putIfAbsent is not supported, there can be several callers which are creating values,
 * and this utility can me reduced to simplest Map, with TTL feature.
 * @param <K> class for keys, instances should follow the usual rules for hashCode/equals methods.
 * @param <V> class for values
 */

public interface CacheProvider<K, V> {
	/**
	 * Returns name for cache.
	 * @return string with cache name.
	 */
	String getName();
	
	/**
	 * Returns value from cache if any exists.
	 * @param key which is used as a cache key.
	 * @return value which will be used as a cache value.
	 */
	V get(K key);
	
	/**
	 * Puts value into a cache.
	 * @param key which is used as a cache key.
	 * @param value which is stored in a cache. If there is no value in cache it returns null.
	 */
	default void put(K key, V value) {
		putIfAbsent(key, value);
	}
	
	/**
	 * Atomic method to put into cache If only there is no element in cache.
	 * @param key which is used as a cache key.
	 * @param value which will be stored in a cache.
	 * @return old value if any in cache.
	 */
	V putIfAbsent(K key, V value);
	
	/**
	 * Deletes from cache.
	 * @param key which is used as a cache key.
	 * @return value which was stored before deletion, if any.
	 */
	V remove(K key);
	
	/**
	 * Returns all entries which are stored in cache for further deletion.
	 * @return entries in cache.
	 */
	void forEach(Consumer<Cache.Entry<K, V>> consumer);
}
