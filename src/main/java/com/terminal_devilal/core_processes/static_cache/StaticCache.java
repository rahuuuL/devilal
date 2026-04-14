package com.terminal_devilal.core_processes.static_cache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

@Service
public class StaticCache {

	public static final String COOKIE = "COOKIE";

	private final Map<String, String> cache = new ConcurrentHashMap<>();

	public StaticCache() {
		set(COOKIE, "");
	}

	public void set(String key, String value) {
		cache.put(key, value);
	}

	public String get(String key) {
		return cache.get(key);
	}
}
