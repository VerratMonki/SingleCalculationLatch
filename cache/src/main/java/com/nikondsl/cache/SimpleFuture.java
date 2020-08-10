package com.nikondsl.cache;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class SimpleFuture<K, V, E extends Exception> {
	private volatile long createdTime = System.currentTimeMillis();
	private volatile Reference<V> value;
	private volatile E exception;
	volatile boolean done = false;
	private final ValueProvider<K, V, E> valueProvider;
	private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
	
	public SimpleFuture(ValueProvider<K, V, E> valueProvider) {
		if (valueProvider == null) {
			throw new IllegalArgumentException();
		}
		this.valueProvider = valueProvider;
	}
	
	/**
	 * Returns a calculated value if it's presented or calculate it and return.
	 * NOTE: synchronization here is needed to provide access to calculation for only single thread.
	 * @param key
	 * @param veto
	 * @return
	 * @throws E
	 */
	public V get(K key, CachingVeto<K, V> veto) throws E {
		if (key == null) {
			throw new IllegalArgumentException();
		}
		if (exception != null) {
			throw exception;
		}
		try{
			lock.readLock().lock();
			boolean expired = isExpired() && (veto == null || value == null || veto.expireAllowed(key, value.getValue()));
			if (isDone() && !expired) {
				return value.getValue();
			}
		} finally {
			lock.readLock().unlock();
		}
		try {
			lock.writeLock().lock();
			constructValue(key);
			done = true;
			return value.getValue();
		} finally {
			lock.writeLock().unlock();
		}
	}
	
	public boolean isDone() {
		return done;
	}
	
	void constructValue(K key) throws E {
		createdTime = System.currentTimeMillis();
		try{
			setValue(valueProvider.createValue(key));
		} catch (Exception exception) {
			this.exception = (E) exception;
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
				this.value = new StrongReference(value);
				return;
			case WEAK:
				this.value = new WeakReference(value);
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
