package com.nikondsl.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static com.nikondsl.cache.ErrorType.REMOVE;

/**
 * com.nikondsl.Main class for providing cache features and latching.  Typical usage will be like below;
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
	private static final String INTERRUPTION_DETECTED_STOPPING_CACHE = "Interruption detected. Stopping cache '{}'...";
	private static Logger LOG = LoggerFactory.getLogger(SingleCalculationLatch.class);
	private static long DEFAULT_SLEEP_DELETE = 30_000L;
	
	private CacheProvider<K, SimpleFuture<K, V, E>> cache;
	private ValueProvider<K, V, E> valueProvider;
	private SimpleCacheStatistics<K, V, E> statistics;
	private volatile CachingVeto<K, V> veto;
	
	private volatile boolean stop = false;
	private volatile long sleepBeforeDelete = DEFAULT_SLEEP_DELETE;
	private final AtomicReference<Reference<Object>> flagOutOfMemory = new AtomicReference<>();
	
	private void setUpFlagOutOfMemory() {
		if (valueProvider.getReferenceType() == ReferenceType.WEAK) {
			flagOutOfMemory.set(new WeakReference<>(new Object()));
		} else {
			flagOutOfMemory.set(new SoftReference(new Object()));
		}
	}
	
	private boolean isAboutOutOfMemory() {
		if (valueProvider.getReferenceType() == ReferenceType.STRONG) {
			return false;
		}
		if (flagOutOfMemory.get().get() == null) {
			setUpFlagOutOfMemory();
			return true;
		}
		return false;
	}
	
	private Thread cleaner = new Thread(() -> {
		try {
			TimeUnit.SECONDS.sleep(1);
		} catch (InterruptedException e) {
			stop = true;
			Thread.currentThread().interrupt();
			LOG.info(INTERRUPTION_DETECTED_STOPPING_CACHE, cache.getName());
			return;
		}
		LOG.info("Cache '{}' cleaner thread has started", cache.getName());
		while (!stop) {
			LOG.info(String.valueOf(statistics));
			try {
				if (!isAboutOutOfMemory()) {
					TimeUnit.MILLISECONDS.sleep(sleepBeforeDelete);
				} else {
					LOG.info("OutOfMemory warning event fired. Clearing cache '{}'...", cache.getName());
					//clear all, except busy
					cache.forEach(entry -> {
						try {
							if (entry.getValue().isDone()) {
								removeElement(entry.getKey(), entry.getValue(), false);
							}
						} catch (Exception ex) {
							LOG.error("Could not remove element '{}' from cache '{}'", entry.getKey(), cache.getName(), ex);
							statistics.error((E) ex, entry.getKey(), REMOVE);
						}
					});
					LOG.info("Cache '{}' is cleared.", cache.getName());
					setUpFlagOutOfMemory();
				}
			} catch (InterruptedException e) {
				stop = true;
				Thread.currentThread().interrupt();
				LOG.info(INTERRUPTION_DETECTED_STOPPING_CACHE, cache.getName());
				break;
			}
			removeAllExpired();
		}
		LOG.info("Cache '{}' cleaner thread has stopped", cache.getName());
	});
	
	void removeAllExpired() {
		LOG.debug("Running clearing expired elements from cache: '{}'", cache.getName());
		final AtomicInteger count = new AtomicInteger();
		cache.forEach(entry -> {
			try {
				removeElement(entry.getKey(), entry.getValue(), true);
			} catch (Exception ex) {
				LOG.error("Could not remove element '{}' from cache '{}'", entry.getKey(), cache.getName(), ex);
				statistics.error((E) ex, entry.getKey(), REMOVE);
			}
			count.incrementAndGet();
		});
		statistics.setTotalSize(count.get());
		LOG.info(statistics.toString());
	}
	
	private void removeElement(K key, SimpleFuture<K, V, E> value, boolean checkVeto) throws E {
		if (key == null || !value.isDone() || !value.isExpired()) {
			return;
		}
		if (veto == null || checkVeto && veto.removeAllowed(key, value.get(key, veto, statistics))) {
			cache.remove(key);
			statistics.remove(key);
			LOG.trace("Element with key: '{}' is removed from cache: '{}'", key, cache.getName());
		}
	}
	
	public SingleCalculationLatch(CacheProvider<K, SimpleFuture<K, V, E>> cache,
								  ValueProvider<K, V, E> valueProvider) {
		this(cache, valueProvider, new SimpleCacheStatistics<K, V, E>());
	}
	
	public SingleCalculationLatch(CacheProvider<K, SimpleFuture<K, V, E>> cache,
								  ValueProvider<K, V, E> valueProvider,
								  SimpleCacheStatistics<K, V, E> statistics) {
		Objects.requireNonNull(cache,"Cache should be provided");
		Objects.requireNonNull(valueProvider,"ValueProvider should be provided");
		Objects.requireNonNull(statistics,"Statistics should be provided");
		this.cache = cache;
		this.valueProvider = valueProvider;
		setUpFlagOutOfMemory();
		cleaner.setName("Cleaner thread for cache: '" + cache.getName() +"'");
		cleaner.start();
		this.statistics = statistics;
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
			LOG.debug("Adding '{}' into cache '{}' vetoed.", key, cache.getName());
			future.setValue(null);
		}
		return result;
	}
	
	public void stop() {
		this.stop = true;
		cleaner.interrupt();
		LOG.info("Final cache '{}' ratio: {} %, {}/{}/{}/{} (hit/miss/error/removed)",
				cache.getName(),
				statistics.ratio(),
				statistics.getHits(), statistics.getMisses(), statistics.getErrors(),
				statistics.getRemoves());
	}
	
	public void setVeto(final CachingVeto<K, V> veto) {
		if (veto == null) {
			throw new IllegalArgumentException("Veto cannot be null");
		}
		LOG.debug("Vetoing {} is set for cache '{}'", veto.getClass().getCanonicalName(), cache.getName());
		this.veto = veto;
	}
	
	public void setSleepBeforeDelete(final long sleepBeforeDelete) {
		if (sleepBeforeDelete <= 0) {
			return;
		}
		LOG.debug("Sleeping period between removing is set to: {} for cache: '{}'", sleepBeforeDelete, cache.getName());
		this.sleepBeforeDelete = sleepBeforeDelete;
	}
}
