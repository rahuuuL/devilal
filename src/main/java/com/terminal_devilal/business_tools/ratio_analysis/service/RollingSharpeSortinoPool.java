package com.terminal_devilal.business_tools.ratio_analysis.service;

final class RollingSharpeSortinoPool {

    private final Entry[] entries;

    RollingSharpeSortinoPool(int threadCount) {
        if (threadCount <= 0) {
            throw new IllegalArgumentException("threadCount must be positive");
        }
        int capacity = Math.max(threadCount, (int) Math.ceil(threadCount * 1.5));
        entries = new Entry[capacity];
        for (int i = 0; i < entries.length; i++) {
            entries[i] = new Entry();
        }
    }

    synchronized Lease acquire(int window, double riskFreeRateAnnual) {
        for (Entry entry : entries) {
            if (!entry.inUse) {
                entry.rolling.configure(window, riskFreeRateAnnual);
                entry.inUse = true;
                return new Lease(this, entry);
            }
        }
        throw new IllegalStateException("No rolling Sharpe/Sortino calculator is available in the pool");
    }

    private synchronized void release(Entry entry) {
        if (!entry.inUse) {
            throw new IllegalStateException("Rolling Sharpe/Sortino calculator was already released");
        }
        entry.rolling.reset();
        entry.inUse = false;
    }

    private static final class Entry {
        private final RollingSharpeSortino rolling = new RollingSharpeSortino(1, 0.0);
        private boolean inUse;
    }

    static final class Lease implements AutoCloseable {
        private final RollingSharpeSortinoPool pool;
        private final Entry entry;
        private boolean closed;

        private Lease(RollingSharpeSortinoPool pool, Entry entry) {
            this.pool = pool;
            this.entry = entry;
        }

        RollingSharpeSortino getRolling() {
            if (closed) {
                throw new IllegalStateException("Rolling Sharpe/Sortino calculator lease is closed");
            }
            return entry.rolling;
        }

        @Override
        public void close() {
            if (!closed) {
                pool.release(entry);
                closed = true;
            }
        }
    }
}
