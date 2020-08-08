package com.nikondsl.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.TimeUnit;

public class SingleCalculationLatch<K, V, E extends Exception> {
	private static Logger LOG = LoggerFactory.getLogger(SingleCalculationLatch.class);
	private static long DEFAULT_SLEEP_DELETE = 30_000L;
	
	private CacheProvider<K, SimpleFuture<K, V, E>> cache;
	private ValueProvider<K, V, E> valueProvider;
	private CachingVeto<K, V> veto;
	private volatile boolean stop = false;
	private long sleepBeforeDelete = DEFAULT_SLEEP_DELETE;
	private Thread cleaner = new Thread(() -> {
		LOG.info("Cache cleaner [{}] started", cache.getName());
		while (!stop) {
			removeAllExpired();
		}
		LOG.info("Cache cleaner [{}] stopped", cache.getName());
	});
	
	void removeAllExpired() {
		for(Map.Entry<K, SimpleFuture<K, V, E>> entry :  cache.getEntries()) {
			try {
				TimeUnit.MILLISECONDS.sleep(sleepBeforeDelete);
				removeElement(entry);
			} catch (InterruptedException e) {
				stop = true;
				Thread.currentThread().interrupt();
				break;
			} catch (Exception ex) {
				LOG.error("Could not clear cache", ex);
			}
		}
	}
	
	private void removeElement(Map.Entry<K, SimpleFuture<K, V, E>> entry) throws E {
		if (entry == null || !entry.getValue().isDone() || !entry.getValue().isExpired()) {
			return;
		}
		K key = entry.getKey();
		if (veto == null || veto.removeAllowed(key, entry.getValue().get(key, veto))) {
			cache.remove(key);
		}
	}
	
	public SingleCalculationLatch(CacheProvider<K, SimpleFuture<K, V, E>> cache, ValueProvider<K, V, E> valueProvider) {
		this.cache = cache;
		this.valueProvider = valueProvider;
		cleaner.setName("Cleaner for cache: " + cache.getName());
		cleaner.start();
	}
	
	public V get(K key) throws E {
		SimpleFuture<K, V, E> newFuture = new SimpleFuture<> (valueProvider);
		SimpleFuture<K, V, E> future = cache.putIfAbsent(key, newFuture);
		if (future == null) {
			future = newFuture;
		}
		V result = future.get(key, veto);
		if (veto != null && !veto.putInCasheAllowed(key, result)) {
			future.setValue(null);
		}
		return result;
	}
	
	public void stop() {
		this.stop = true;
		cleaner.interrupt();
	}
	
	public void setVeto(final CachingVeto<K, V> veto) {
		if (veto == null) {
			throw new IllegalArgumentException();
		}
		this.veto = veto;
	}
	
	public void setSleepBeforeDelete(final long sleepBeforeDelete) {
		this.sleepBeforeDelete = sleepBeforeDelete;
	}
}
