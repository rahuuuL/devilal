CREATE TABLE IF NOT EXISTS sharpe_sortino_config (
    days INT PRIMARY KEY,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    description VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS sharpe_sortino_generation_history (
    date DATE NOT NULL,
    days INT NOT NULL,
    status VARCHAR(20) NOT NULL,
    error_message VARCHAR(1024),
    skipped_tickers VARCHAR(2048),
    PRIMARY KEY (date, days)
);

CREATE TABLE IF NOT EXISTS sharpe_sortino_result_history (
    ticker VARCHAR(50) NOT NULL,
    date DATE NOT NULL,
    rolling_days INT NOT NULL,
    sortino_ratio DOUBLE NOT NULL,
    sharpe_ratio DOUBLE NOT NULL,
    PRIMARY KEY (ticker, date, rolling_days)
);

CREATE INDEX idx_sharpe_sortino_date_days_ticker
    ON sharpe_sortino_result_history(date, rolling_days, ticker);
