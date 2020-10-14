package com.nikondsl.cache.adapter;

import com.nikondsl.cache.ApiReference;
import com.nikondsl.cache.CacheProvider;
import org.ehcache.Cache;

import java.util.function.Consumer;

@ApiReference(since ="1.0.0")
public class EhCacheProviderAdapter<K, V> implements CacheProvider<K, V> {
	private Cache<K, V> delegate;
	private String cacheName;
	
	@ApiReference(since ="1.0.0")
	public EhCacheProviderAdapter(String cacheName, Cache<K, V> cache) {
		if (cache == null) {
			throw new IllegalArgumentException("Cache is not provided");
		}
		delegate = cache;
		this.cacheName = cacheName;
	}
	
	
	@Override
	@ApiReference(since ="1.0.0")
	public String getName() {
		return cacheName;
	}
	
	@Override
	@ApiReference(since ="1.0.0")
	public V get(K key) {
		return delegate.get(key);
	}
	
	@Override
	@ApiReference(since ="1.0.0")
	public V putIfAbsent(K key, V value) {
		return delegate.putIfAbsent(key, value);
	}
	
	@Override
	@ApiReference(since ="1.0.0")
	public V remove(K key) {
		delegate.remove(key);
		return null;
	}
	
	@Override
	@ApiReference(since ="1.0.0")
	public void forEach(Consumer<Cache.Entry<K, V>> consumer) {
		delegate.forEach(consumer);
	}
}
