package com.nikondsl.cache;

import java.util.concurrent.atomic.AtomicInteger;

public class SimpleCacheStatistics {
	private AtomicInteger hits = new AtomicInteger();
	private AtomicInteger misses = new AtomicInteger();
	private AtomicInteger errors = new AtomicInteger();
	private AtomicInteger vetoPut = new AtomicInteger();
	private AtomicInteger vetoRemove = new AtomicInteger();
	private AtomicInteger vetoExpire = new AtomicInteger();
	public void hit() {
		hits.incrementAndGet();
	}
	public void miss() {
		misses.incrementAndGet();
	}
	public void error() {
		errors.incrementAndGet();
	}
}
