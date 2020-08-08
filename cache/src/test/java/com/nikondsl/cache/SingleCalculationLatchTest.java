package com.nikondsl.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@RunWith(JUnitPlatform.class)
public class SingleCalculationLatchTest {
	private SingleCalculationLatch<String, Integer, Exception> latch;
	private CacheProvider<String, SimpleFuture<String, Integer, Exception>> cacheProvider;
	private ValueProvider<String, Integer, Exception> valueProvider;
	
	@BeforeEach
	public void setUp() {
		cacheProvider = spy(new CacheProvider<String, SimpleFuture<String, Integer, Exception>>() {
			private ConcurrentMap<String, SimpleFuture<String, Integer, Exception> > cache = new ConcurrentHashMap<>();
			
			@Override
			public String getName() {
				return "default";
			}
			
			@Override
			public SimpleFuture<String, Integer, Exception>  get(String key) {
				return cache.get(key);
			}
			
			@Override
			public SimpleFuture<String, Integer, Exception>  putIfAbsent(String key, SimpleFuture<String, Integer, Exception>  value) {
				return cache.putIfAbsent(key, value);
			}
			
			@Override
			public SimpleFuture<String, Integer, Exception>  remove(String key) {
				return cache.remove(key);
			}
			
			@Override
			public Iterable<Map.Entry<String, SimpleFuture<String, Integer, Exception> >> getEntries() {
				return cache.entrySet();
			}
		});
		valueProvider = spy(new ValueProvider<String, Integer, Exception>() {
			@Override
			public Integer createValue(String key) throws Exception {
				TimeUnit.MILLISECONDS.sleep(10);
				return key.length();
			}
			
			@Override
			public long getTimeToLive() {
				return 1L;
			}
		});
		latch = spy(new SingleCalculationLatch(cacheProvider, valueProvider));
	}
	
	@Test
	public void testVeto() throws Exception {
		CachingVeto<String, Integer> veto = new CachingVeto<String, Integer>() {
			@Override
			public boolean removeAllowed(String key, Integer value) {
				return false;
			}
			
			@Override
			public boolean expireAllowed(String key, Integer value) {
				return false;
			}
		};
		latch.setSleepBeforeDelete(1);
		latch.setVeto(veto);
		assertEquals(4, (int)latch.get("zero"));
		latch.stop();
		
		latch.removeAllExpired();
		
		TimeUnit.MILLISECONDS.sleep(25);
		verify(cacheProvider, never()).remove("zero");
	}
	
	@Test
	public void testNoVeto() throws Exception {
		CachingVeto<String, Integer> veto = new CachingVeto<String, Integer>() {};
		latch.setSleepBeforeDelete(1);
		latch.setVeto(veto);
		assertEquals(4, (int)latch.get("zero"));
		latch.stop();
		
		latch.removeAllExpired();
		
		TimeUnit.MILLISECONDS.sleep(25);
		verify(cacheProvider).remove("zero");
	}
}