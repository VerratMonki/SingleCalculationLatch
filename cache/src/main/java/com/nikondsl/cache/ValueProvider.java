package com.nikondsl.cache;

import java.util.concurrent.TimeUnit;

/**
 * Factory for providing calculation for values by given keys.
 * @param <K>
 * @param <V>
 * @param <E>
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
	 * TimeTToLive newly created element in cache.
	 * @return milliseconds till element in cache is not expired and may be used.
	 */
	default long getTimeToLive() {
		return TimeUnit.SECONDS.toMillis(1L);
	}
	
	default ReferenceType getReferenceType() {
		return ReferenceType.STRONG;
	}
}
