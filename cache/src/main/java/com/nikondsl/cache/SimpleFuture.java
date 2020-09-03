package com.nikondsl.cache;

import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Cache will use this future for managing creating values.
 * @param <K> class for specifying key.
 * @param <V> class for specifying value.
 * @param <E> class for specifying exception.
 */
public class SimpleFuture<K, V, E extends Exception> {
	private volatile long createdTime = System.currentTimeMillis();
	private volatile Reference<V> value;
	private volatile E exception;
	private volatile boolean done = false;
	private final ValueProvider<K, V, E> valueProvider;
	private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
	
	public SimpleFuture(ValueProvider<K, V, E> valueProvider) {
		if (valueProvider == null) {
			throw new IllegalArgumentException("No value provider presented");
		}
		this.valueProvider = valueProvider;
	}
	
	/**
	 * Returns a calculated value if it's presented or calculates it and returns.
	 * NOTE: synchronization here is needed to provide access to calculation for only single thread.
	 * @param key key for caching.
	 * @param veto
	 * @param statistics
	 * @return value in cashe if presented or calculates new value and returns it.
	 * @throws E
	 */
	public V get(K key, CachingVeto<K, V> veto, SimpleCacheStatistics statistics) throws E {
		if (key == null) {
			throw new IllegalArgumentException("Key is required, nulls are not supported.");
		}
		if (exception != null) {
			throw exception;
		}
		if (getInReadLock(key, veto)) {
			statistics.hit();
			return value.getValue();
		}
		lock.writeLock().lock();
		try {
			if (getInReadLock(key, veto)) {
				statistics.hit();
				return value.getValue();
			}
			constructValue(key, statistics);
			statistics.setMaxHold(lock.getReadLockCount());
			done = true;
			return value.getValue();
		} finally {
			lock.writeLock().unlock();
		}
	}
	
	private boolean getInReadLock(K key, CachingVeto<K, V> veto) {
		lock.readLock().lock();
		try {
			boolean expired = isExpired() && (veto == null || value == null || veto.expireAllowed(key, value.getValue()));
			if (isDone() && !expired) {
				return true;
			}
		} finally {
			lock.readLock().unlock();
		}
		return false;
	}
	
	public boolean isDone() {
		return done;
	}
	
	/**
	 * Creates a new value for a given key.
	 * @param key for caching.
	 * @param statistics
	 * @throws E if any exception occurs.
	 */
	void constructValue(K key, SimpleCacheStatistics statistics) throws E {
		createdTime = System.currentTimeMillis();
		try{
			setValue(valueProvider.createValue(key));
			statistics.miss();
		} catch (Exception exception) {
			this.exception = (E) exception;
			statistics.error();
			throw exception;
		}
	}
	
	public boolean isExpired() {
		if (isDone() || exception != null) {
			long remain = System.currentTimeMillis() - createdTime;
			return remain > valueProvider.getTimeToLive();
		}
		return true;
	}
	
	void setException(final E exception) {
		this.exception = exception;
	}
	
	void setValue(V value) {
		switch (valueProvider.getReferenceType()) {
			case STRONG:
				this.value = new StrongReference<>(value);
				return;
			case WEAK:
				this.value = new WeakReference<>(value);
				return;
			default:
				this.value = new SoftReference<>(value);
		}
	}
	
	interface Reference<T>{
		T getValue();
	}
	
	static class StrongReference<T> implements Reference<T> {
		private T value;
		
		public StrongReference(T value) {
			this.value = value;
		}
		
		@Override
		public T getValue() {
			return value;
		}
	}
	
	static class SoftReference<T> implements Reference<T> {
		private java.lang.ref.SoftReference<T> value;
		
		public SoftReference(T value) {
			this.value = new java.lang.ref.SoftReference<>(value);
		}
		
		@Override
		public T getValue() {
			return value.get();
		}
	}
	
	static class WeakReference<T> implements Reference<T> {
		private java.lang.ref.WeakReference<T> value;
		
		public WeakReference(T value) {
			this.value = new java.lang.ref.WeakReference<>(value);
		}
		
		@Override
		public T getValue() {
			return value.get();
		}
	}
}
