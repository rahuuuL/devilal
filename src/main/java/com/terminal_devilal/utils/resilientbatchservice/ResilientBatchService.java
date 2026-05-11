package com.terminal_devilal.utils.resilientbatchservice;

import org.springframework.scheduling.annotation.Scheduled;

import com.terminal_devilal.utils.deadletterqueue.DeadLetterQueue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Base class providing resilient buffered batch saving for any entity type.
 *
 * Strategy: 1. Buffer records in memory as they arrive. 2. Every 2s, flush the
 * buffer via saveAll() — one fast DB transaction. 3. If saveAll() throws, fall
 * back to per-record save() to isolate the bad record. 4. Any record that fails
 * individually goes to the DeadLetterQueue. 5. DLQ is retried every 60s.
 * Permanently failing records are logged with full context.
 *
 * Subclasses implement saveAll() and saveOne() pointing at their repository.
 */
public abstract class ResilientBatchService<T> {

	private final ConcurrentLinkedQueue<T> buffer = new ConcurrentLinkedQueue<>();
	protected final DeadLetterQueue<T> dlq = new DeadLetterQueue<>();

	protected void enqueue(T record) {
		buffer.add(record);
	}

	@Scheduled(fixedDelay = 2000)
	public void flushBuffer() {
		List<T> batch = new ArrayList<>();
		T item;
		while ((item = buffer.poll()) != null) {
			batch.add(item);
		}

		if (batch.isEmpty())
			return;

		try {
			// Fast path — single transaction for the whole batch
			saveAll(batch);
		} catch (Exception batchEx) {
			System.err.printf("[BATCH] saveAll failed for %d %s records, falling back to per-record save%n",
					batch.size(), batch.get(0).getClass().getSimpleName());

			// Slow path — each record gets its own transaction so one bad record can't kill
			// the rest
			for (T record : batch) {
				try {
					saveOne(record);
				} catch (Exception recordEx) {
					dlq.add(record, recordEx);
				}
			}
		}
	}

	@Scheduled(fixedDelay = 60000)
	public void retryDeadLetters() {
		List<DeadLetterQueue.FailedRecord<T>> failed = dlq.drainAll();
		if (failed.isEmpty())
			return;

		System.out.printf("[DLQ] Retrying %d failed record(s)%n", failed.size());

		for (DeadLetterQueue.FailedRecord<T> entry : failed) {
			try {
				saveOne(entry.record());
				System.out.printf("[DLQ] Retry succeeded for record originally failed at %s%n", entry.failedAt());
			} catch (Exception retryEx) {
				// Permanently failed — log everything so nothing is lost silently
				System.err.printf("[DLQ] Permanent failure | record: %s | originally failed at: %s | retry error: %s%n",
						entry.record(), entry.failedAt(), retryEx.getMessage());
				// Hook point: plug in your alerting, metrics, or file-based fallback here
				onPermanentFailure(entry.record(), retryEx);
			}
		}
	}

	/**
	 * Called when a record has failed both the batch save and the DLQ retry.
	 * Override in subclasses to push to monitoring, write to a file, send an alert,
	 * etc. Default implementation is a no-op (the error is already logged above).
	 */
	protected void onPermanentFailure(T record, Exception e) {
	}

	protected abstract void saveAll(List<T> batch);

	protected abstract void saveOne(T record);
}
