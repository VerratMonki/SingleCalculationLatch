package com.nikondsl.cache;

public class SimpleFuture<K, V, E extends Exception> {
	private long timeToLive = 0L;
	private Reference<V> value;
	private E exception;
	boolean done = false;
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
		boolean expired = isExpired() && (veto == null || value == null || veto.expireAllowed(key, value.getValue()));
		if (!isDone() || expired) {
			constructValue(key);
			done = true;
		}
		return value.getValue();
	}
	
	public boolean isDone() {
		return done;
	}
	
	void constructValue(K key) throws E {
		timeToLive = System.currentTimeMillis();
		try{
			setValue(valueProvider.createValue(key));
		} catch (Exception exception) {
			this.exception = (E) exception;
			throw exception;
		}
	}
	
	public boolean isExpired() {
		return System.currentTimeMillis() - timeToLive > valueProvider.getTimeToLive();
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
