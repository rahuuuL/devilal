ALTER TABLE sharpe_sortino_config
    ADD COLUMN risk_free_rate_percent DOUBLE NOT NULL DEFAULT 6.0;
