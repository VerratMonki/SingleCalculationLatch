package com.nikondsl.cache;

import java.util.concurrent.TimeUnit;

/**
 * Factory for providing value calculation by given key.
 * @param <K> class for specifying keys.
 * @param <V> class for specifying values.
 * @param <E> class for specifying exception if occurs during value creation.
 */
public interface ValueProvider<K, V, E extends Throwable> {
	/**
	 * This factory should return created element which is associated with given key.
	 * @param key
	 * @return constructed value for given key.
	 * @throws Exception if value cannot be created.
	 */
	V createValue(K key) throws E;
	
	/**
	 * TimeToLive for newly created elements in cache.
	 * @return milliseconds till element in cache is not expired and may be used further.
	 */
	default long getTimeToLive() {
		return TimeUnit.SECONDS.toMillis(1L);
	}
	
	default ReferenceType getReferenceType() {
		return ReferenceType.STRONG;
	}
}
