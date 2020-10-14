package com.nikondsl.cache;

@ApiReference(since ="1.0.1")
public interface CacheStatistics<K, E extends Exception> {
	/**
	 * Called each time when the same value will be got for the same key.
	 * @param key
	 */
	@ApiReference(since ="1.0.1")
	void hit(K key);
	
	/**
	 * Called each time when a new value should be calculated (i.e. not in cache).
	 * @param key
	 */
	@ApiReference(since ="1.0.1")
	void miss(K key);
	
	/**
	 * Called each time when element is meant to be evicted/removed.
	 * @param key is a key for which remove event is fired
	 */
	@ApiReference(since ="1.0.1")
	void remove(K key);
	
	/**
	 * Called each time when new exception is thrown.
	 * @param ex is an exception which is thrown.
	 * @param key is a key for which exception appears
	 * @param remove type of operation when exception appears
	 */
	@ApiReference(since ="1.0.1")
	void error(E ex, K key, ErrorType remove);
}
