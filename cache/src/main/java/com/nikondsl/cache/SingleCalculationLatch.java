package com.nikondsl.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Main class for providing cache features and latching.  Typical usage will be like below;
 *
 * CacheProvider cacheProvider = ...
 * ValueProvider valueProvider = ...
 * SingleCalculationLatch latch = new SingleCalculationLatch(cacheProvider, valueProvider);
 * latch.setSleepBeforeDelete(5_000);
 * ...
 * latch.get(key);
 *
 * @param <K> class for specifying keys.
 * @param <V> class for specifying values.
 * @param <E> class for specifying exception if appears during value creation.
 */
public class SingleCalculationLatch<K, V, E extends Exception> {
	private static Logger LOG = LoggerFactory.getLogger(SingleCalculationLatch.class);
	private static long DEFAULT_SLEEP_DELETE = 30_000L;
	
	private CacheProvider<K, SimpleFuture<K, V, E>> cache;
	private ValueProvider<K, V, E> valueProvider;
	private CachingVeto<K, V> veto;
	private volatile boolean stop = false;
	private volatile long sleepBeforeDelete = DEFAULT_SLEEP_DELETE;
	private SimpleCacheStatistics statistics = new SimpleCacheStatistics();
	
	private Thread cleaner = new Thread(() -> {
		try {
			TimeUnit.SECONDS.sleep(1);
		} catch (InterruptedException e) {
			stop = true;
			Thread.currentThread().interrupt();
			return;
		}
		LOG.info("Cache cleaner [{}] started", cache.getName());
		while (!stop) {
			LOG.info(String.valueOf(statistics));
			try {
				TimeUnit.MILLISECONDS.sleep(sleepBeforeDelete);
			} catch (InterruptedException e) {
				stop = true;
				Thread.currentThread().interrupt();
				break;
			}
			try {
				removeAllExpired();
			} catch (Exception ex) {
				System.err.println("exc...");
				LOG.error("Could not clear cache", ex);
			}
		}
		LOG.info("Cache cleaner [{}] stopped", cache.getName());
	});
	
	void removeAllExpired() throws Exception {
		LOG.debug("Running clearing expired elements in cache: {}", cache.getName());
		int count = 0;
		for(Map.Entry<K, SimpleFuture<K, V, E>> entry :  cache.getEntries()) {
			removeElement(entry);
			count++;
			if (stop) {
				break;
			}
		}
		statistics.setTotalSize(count);
		LOG.info(statistics.toString());
	}
	
	private void removeElement(Map.Entry<K, SimpleFuture<K, V, E>> entry) throws E {
		if (entry == null || !entry.getValue().isDone() || !entry.getValue().isExpired()) {
			return;
		}
		K key = entry.getKey();
		if (veto == null || veto.removeAllowed(key, entry.getValue().get(key, veto, statistics))) {
			cache.remove(key);
			statistics.remove();
			LOG.trace("Element with key: {} is removed from cache: {}", key, cache.getName());
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
			LOG.trace("New cache item is being created and put into cache: {} with key: {}", cache.getName(), key);
			future = newFuture;
		}
		V result = future.get(key, veto, statistics);
		if (veto != null && !veto.putInCasheAllowed(key, result)) {
			LOG.debug("Adding '{}' into cache vetoed.", key);
			future.setValue(null);
		}
		return result;
	}
	
	public void stop() {
		this.stop = true;
		cleaner.interrupt();
		LOG.error("Final cache '{}' ratio: {} %, {}/{}/{}/{} (hit/miss/error/removed)",
				cache.getName(),
				statistics.ratio(),
				statistics.getHits(), statistics.getMisses(), statistics.getErrors(),
				statistics.getRemoves());
	}
	
	public void setVeto(final CachingVeto<K, V> veto) {
		if (veto == null) {
			throw new IllegalArgumentException();
		}
		LOG.debug("Vetoing {} is set for cache {}", veto.getClass().getCanonicalName(), cache.getName());
		this.veto = veto;
	}
	
	public void setSleepBeforeDelete(final long sleepBeforeDelete) {
		if (sleepBeforeDelete > 0) {
			LOG.debug("Sleeping period between removing is set to: {} for cache: {}",
					sleepBeforeDelete, cache.getName());
			this.sleepBeforeDelete = sleepBeforeDelete;
		}
	}
}
