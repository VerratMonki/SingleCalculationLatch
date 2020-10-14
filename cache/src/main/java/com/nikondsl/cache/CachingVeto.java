package com.nikondsl.cache;

/**
 * Provides some permissions/rules for caching. Vetoing means that operation
 * is not permitted for particular key/value pair.
 * @param <K> class for key in cache.
 * @param <V> class for value in cache.
 */
@ApiReference(since ="1.0.0")
public interface CachingVeto<K, V> {
	
	/**
	 * Returns if removing from cache enabled.
	 * @param key
	 * @param value
	 * @return true only if this key,value pair is allowed to be removed.
	 */
	@ApiReference(since ="1.0.0")
	default boolean removeAllowed(K key, V value) {
		return true;
	}
	
	/**
	 * Returns whether element in cache may be expired.
	 * @param key
	 * @param value
	 * @return true only if this key,value pair is allowed to be expired.
	 */
	@ApiReference(since ="1.0.0")
	default boolean expireAllowed(K key, V value) {
		return true;
	}
	
	/**
	 * Returns whether putting into a cache enabled.
	 * @param key
	 * @param value
	 * @return true only if putting enabled.
	 */
	@ApiReference(since ="1.0.0")
	default boolean putInCasheAllowed(K key, V value) {
		return true;
	}
}