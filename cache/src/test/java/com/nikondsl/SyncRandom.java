package com.nikondsl;

import java.security.SecureRandom;

public class SyncRandom {
	private SecureRandom random = new SecureRandom();
	public synchronized double nextDouble() {
		return random.nextDouble();
	}
}
