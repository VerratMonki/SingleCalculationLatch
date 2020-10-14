package com.nikondsl.cache;

import java.util.concurrent.atomic.AtomicInteger;

@ApiReference(since ="1.0.0")
public class SimpleCacheStatistics<K, V, E extends Exception> implements CacheStatistics<K, E> {
	private AtomicInteger hits = new AtomicInteger();
	private AtomicInteger misses = new AtomicInteger();
	private AtomicInteger errors = new AtomicInteger();
	private AtomicInteger removes = new AtomicInteger();
	private AtomicInteger totalInCache = new AtomicInteger();
	private AtomicInteger maxHold = new AtomicInteger();
	
	@Override
	@ApiReference(since ="1.0.0")
	public void hit(K key) {
		hits.incrementAndGet();
	}
	
	@Override
	@ApiReference(since ="1.0.0")
	public void miss(K key) {
		misses.incrementAndGet();
	}
	
	@Override
	@ApiReference(since ="1.0.0")
	public void remove(K key) {
		removes.incrementAndGet();
	}
	
	public int getHits() {
		return this.hits.get();
	}
	
	public int getMisses() {
		return this.misses.get();
	}
	
	public int getErrors() {
		return this.errors.get();
	}
	
	public int getRemoves() {
		return removes.get();
	}
	
	public void setTotalSize(int size) {
		this.totalInCache.set(size);
	}
	
	public void setMaxHold(int count) {
		if (count > maxHold.get()) {
			maxHold.set(count);
		}
	}
	
	public int ratio() {
		int sum = getHits() + getMisses() + getErrors();
		if (sum == 0) return 0;
		return (int) (100.0 * getHits() / sum);
	}
	
	@Override
	@ApiReference(since ="1.0.0")
	public String toString() {
		return "ratio: "+ratio()+" %, "+getHits()+"/"+getMisses()+"/"+getErrors()+"/"+getRemoves()+"/"+totalInCache.get()+
				"/"+maxHold.get()+
				" (hit/miss/error/removed/total/max_hold)";
	}
	
	@Override
	@ApiReference(since ="1.0.0")
	public void error(E ex, K key, ErrorType remove) {
		errors.incrementAndGet();
	}
}
