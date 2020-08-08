package com.nikondsl.cache;

import java.util.Map;
import java.util.concurrent.TimeUnit;

public class SingleCalculationLatch<K, V, E extends Exception> {
	
	private CacheProvider<K, SimpleFuture<K, V, E>> cache;
	private ValueProvider<K, V, E> valueProvider;
	private CachingVeto<K, V> veto;
	private volatile boolean stop = false;
	private Thread cleaner = new Thread(() -> {
		while (!stop) {
			for(Map.Entry<K, SimpleFuture<K, V, E>> entry :  cache.getEntries()) {
				try {
					TimeUnit.SECONDS.sleep(30);
					removeElement(entry);
				} catch (InterruptedException e) {
					stop = true;
					Thread.currentThread().interrupt();
					break;
				} catch (Exception ex) {
					//@ToDo fixme
				}
			}
		}
	});
	
	private void removeElement(Map.Entry<K, SimpleFuture<K, V, E>> entry) throws E {
		if (entry != null && entry.getValue().isDone() && entry.getValue().isExpired()) {
			K key = entry.getKey();
			if (veto == null || veto.removeAllowed(key, entry.getValue().get(key, veto))) {
				cache.remove(key);
			}
		}
	}
	
	public SingleCalculationLatch(CacheProvider<K, SimpleFuture<K, V, E>> cache, ValueProvider<K, V, E> valueProvider) {
		this.cache = cache;
		this.valueProvider = valueProvider;
		cleaner.setName("Cleaner for cache: " + cache.getName());
		cleaner.start();
	}
	
	V get(K key) throws E {
		SimpleFuture<K, V, E> newFuture = new SimpleFuture<> (valueProvider);
		SimpleFuture<K, V, E> future = cache.putIfAbsent(key, newFuture);
		if (future == null) {
			future = newFuture;
		}
		return future.get(key, veto);
	}
	
	V create(K key) throws E {
		return valueProvider.createValue(key);
	}
	
	public void stop() {
		this.stop = true;
		cleaner.interrupt();
	}
}
