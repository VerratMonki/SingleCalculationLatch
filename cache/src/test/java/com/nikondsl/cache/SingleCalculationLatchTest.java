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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@RunWith(JUnitPlatform.class)
public class SingleCalculationLatchTest {
	private SingleCalculationLatch<String, Integer, Exception> latch;
	private CacheProvider<String, SimpleFuture<String, Integer, Exception>> cacheProvider;
	private ValueProvider<String, Integer, Exception> valueProvider;
	public static final CachingVeto<String, Integer> NO_EXPIRE_VETO = new CachingVeto<String, Integer>() {
		@Override
		public boolean removeAllowed(String key, Integer value) {
			return false;
		}
		
		@Override
		public boolean expireAllowed(String key, Integer value) {
			return false;
		}
	};
	
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
		latch.setSleepBeforeDelete(10_000L);
	}
	
	@Test
	public void testVetoException() throws Exception {
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
		
		assertThrows(IllegalArgumentException.class, () -> {
			latch.setVeto(null);
		});
	}
	
	@Test
	public void testVetoExpire() throws Exception {
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
	public void testVetoPut() throws Exception {
		CachingVeto<String, Integer> veto = new CachingVeto<String, Integer>() {
			@Override
			public boolean removeAllowed(String key, Integer value) {
				return false;
			}
			
			@Override
			public boolean expireAllowed(String key, Integer value) {
				return false;
			}
			
			@Override
			public boolean putInCasheAllowed(String key, Integer value) {
				return false;
			}
		};
		latch.setSleepBeforeDelete(1);
		latch.setVeto(veto);
		assertEquals(4, (int)latch.get("zero"));
		latch.stop();
		
		TimeUnit.MILLISECONDS.sleep(25);
		
		SimpleFuture<String, Integer, Exception> zero = cacheProvider.get("zero");
		assertNotNull(zero);
		assertTrue(zero.isDone());
		assertTrue(zero.isExpired());
		assertEquals(4, (int)zero.get("zero", null));
		verify(valueProvider, times(2)).createValue("zero");
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
	
	@Test
	public void testThreadShouldWaitWhileCalculates() throws Exception {
		latch.setVeto(NO_EXPIRE_VETO);
		ExecutorService service = Executors.newFixedThreadPool(3);
		long time =System.currentTimeMillis();
		Exception[] exceptions = new Exception[1];
		service.submit(()-> {
			try {
				latch.get("abc");
			} catch (Exception e) {
				exceptions[0] = e;
			}
		});
		service.submit(()-> {
			try {
				latch.get("abc");
			} catch (Exception e) {
				exceptions[0] = e;
			}
		});
		service.submit(()-> {
			try {
				latch.get("abc");
			} catch (Exception e) {
				exceptions[0] = e;
			}
		});
		
		service.shutdown();
		service.awaitTermination(1, TimeUnit.SECONDS);
		
		assertEquals(3, (int)latch.get("abc"));
		assertNull(exceptions[0]);
		
		verify(valueProvider).createValue("abc");
		
		latch.stop();
	}
}