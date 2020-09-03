package com.nikondsl.cache.adapter;

import com.nikondsl.cache.CacheProvider;
import org.ehcache.Cache;

import java.util.function.Consumer;

public class EhCacheProviderAdapter<K, V> implements CacheProvider<K, V> {
	private Cache<K, V> delegate;
	private String cacheName;
	
	public EhCacheProviderAdapter(String cacheName, Cache<K, V> cache) {
		if (cache == null) {
			throw new IllegalArgumentException("Cache is not provided");
		}
		delegate = cache;
		this.cacheName = cacheName;
	}
	
	
	@Override
	public String getName() {
		return cacheName;
	}
	
	@Override
	public V get(K key) {
		return delegate.get(key);
	}
	
	@Override
	public V putIfAbsent(K key, V value) {
		return delegate.putIfAbsent(key, value);
	}
	
	@Override
	public V remove(K key) {
		delegate.remove(key);
		return null;
	}
	
	@Override
	public void forEach(Consumer<Cache.Entry<K, V>> consumer) {
		delegate.forEach(consumer);
	}
}
