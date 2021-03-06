package com.nikondsl.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@RunWith(JUnitPlatform.class)
public class SimpleFutureTest {
	private SimpleFuture<String, String, NoOpException> future;
	private ValueProvider<String, String, NoOpException> valueProvider;
	private SimpleCacheStatistics statistics = new SimpleCacheStatistics();
	
	@BeforeEach
	void setUp() {
		valueProvider = spy(new ValueProvider<String, String, NoOpException>() {
			@Override
			public String createValue(String s) throws NoOpException {
				return s.toUpperCase();
			}
		});
		future = spy(new SimpleFuture<>(valueProvider));
	}
	
	@Test
	public void testGetShouldCallCreate() throws NoOpException {
		doReturn(false).when(future).isExpired();
		doReturn(false).when(future).isDone();
		
		assertEquals("ABC", future.get("abc", null, statistics));
	
		verify(future).constructValue("abc", statistics);
	}
	
	@Test
	public void testExpiredShouldCallCreate() throws NoOpException {
		doReturn(true).when(future).isExpired();
		doReturn(true).when(future).isDone();
		
		assertEquals("ABC", future.get("abc", null, statistics));
		
		verify(future).constructValue("abc", statistics);
	}
	
	@Test
	public void testExceptionPreviousExceptionThrown() throws NoOpException {
		future.setException(new NoOpException());
		
		assertThrows(NoOpException.class, () -> {
			future.get("abc", null, null);
		});
	}
	
	@Test
	public void testExceptionCreateValue() throws NoOpException {
		doReturn(false).when(future).isExpired();
		doReturn(false).when(future).isDone();
		doThrow(NoOpException.class).when(valueProvider).createValue("abc");
		
		assertThrows(NoOpException.class, () -> {
			future.get("abc", null, statistics);
		});
	}
	
	@Test
	public void testGetExceptionWhenKeyIsNull() throws NoOpException {
		assertThrows(IllegalArgumentException.class, () -> {
			future.get(null, null, null);
		});
	}
	
	@Test
	public void testConstructorException() throws NoOpException {
		assertThrows(IllegalArgumentException.class, () -> {
			new SimpleFuture<>(null);
		});
	}
	
	@Test
	public void testIsExpired() {
		assertTrue(future.isExpired());
	}
	
	@Test
	public void testIsExpiredShouldCheckIsDone() {
		doReturn(Long.MAX_VALUE).when(valueProvider).getTimeToLive();
		doReturn(true).when(future).isDone();
		
		assertFalse(future.isExpired());
	}
}