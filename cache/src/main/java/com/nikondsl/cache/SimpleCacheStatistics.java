package com.nikondsl.cache;

import java.util.concurrent.atomic.AtomicInteger;

public class SimpleCacheStatistics {
	private AtomicInteger hits = new AtomicInteger();
	private AtomicInteger misses = new AtomicInteger();
	private AtomicInteger errors = new AtomicInteger();
	private AtomicInteger removes = new AtomicInteger();
	
	public void hit() {
		hits.incrementAndGet();
	}
	
	public void miss() {
		misses.incrementAndGet();
	}
	
	public void error() {
		errors.incrementAndGet();
	}
	
	public void remove() {
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
	
	public int ratio() {
		return (int) (100.0 * getHits() / (getHits() + getMisses() + getErrors()));
	}
}
