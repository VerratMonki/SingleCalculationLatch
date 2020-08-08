package com.nikondsl.cache;

import java.util.concurrent.TimeUnit;

public interface ValueProvider<K, V, E extends Throwable> {
	V createValue(K key) throws E;
	
	default long getTimeToLive() {
		return TimeUnit.SECONDS.toMillis(1L);
	}
}
