package com.nikondsl.cache;

public class SimpleFuture<K, V, E extends Exception> {
	private long timeToLive = 0L;
	private V value;
	private E exception;
	private ValueProvider<K, V, E> valueProvider;
	
	public SimpleFuture(ValueProvider<K, V, E> valueProvider) {
		if (valueProvider == null) {
			throw new IllegalArgumentException();
		}
		this.valueProvider = valueProvider;
	}
	
	public synchronized V get(K key, CachingVeto<K, V> veto) throws E {
		if (key == null) {
			throw new IllegalArgumentException();
		}
		if (exception != null) {
			throw exception;
		}
		boolean expired = isExpired() && (veto == null || veto.expireAllowed(key, value));
		if (!isDone() || expired) {
			constructValue(key);
		}
		return value;
	}
	
	public boolean isDone() {
		return value != null || exception != null;
	}
	
	private void constructValue(K key) throws E {
		timeToLive = System.currentTimeMillis();
		try{
			value = valueProvider.createValue(key);
		} catch (Exception exception) {
			this.exception = (E) exception;
			throw exception;
		}
	}
	
	public boolean isExpired() {
		return System.currentTimeMillis() - timeToLive > valueProvider.getTimeToLive();
	}
}
