package com.terminal_devilal.utils.deadletterqueue;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

public class DeadLetterQueue<T> {
	private final ConcurrentLinkedQueue<FailedRecord<T>> queue = new ConcurrentLinkedQueue<>();

	public void add(T record, Exception reason) {
		queue.add(new FailedRecord<>(record, reason, Instant.now()));
		System.err.printf("[DLQ] Failed to save %s | reason: %s%n", record.getClass().getSimpleName(),
				reason.getMessage());
	}

	public List<FailedRecord<T>> drainAll() {
		List<FailedRecord<T>> drained = new ArrayList<>();
		FailedRecord<T> record;
		while ((record = queue.poll()) != null) {
			drained.add(record);
		}
		return drained;
	}

	public int size() {
		return queue.size();
	}

	public boolean isEmpty() {
		return queue.isEmpty();
	}

	public record FailedRecord<T>(T record, Exception reason, Instant failedAt) {
	}

}
