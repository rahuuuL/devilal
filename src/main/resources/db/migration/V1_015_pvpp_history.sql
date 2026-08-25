CREATE TABLE IF NOT EXISTS pvpp_config (
    days INT PRIMARY KEY,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    description VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS pvpp_daily (
    ticker VARCHAR(50) NOT NULL,
    date DATE NOT NULL,
    return_pct DOUBLE,
    volume BIGINT,
    clv DOUBLE,
    centered_clv DOUBLE,
    return_z DOUBLE,
    clv_z DOUBLE,
    PRIMARY KEY (ticker, date)
);

CREATE INDEX idx_pvpp_daily_date_ticker ON pvpp_daily(date, ticker);

CREATE TABLE IF NOT EXISTS pvpp_result_history (
    ticker VARCHAR(50) NOT NULL,
    date DATE NOT NULL,
    days INT NOT NULL,
    rvol DOUBLE,
    efficiency DOUBLE,
    log_rvol_z DOUBLE,
    pressure_score DOUBLE,
    PRIMARY KEY (ticker, date, days)
);

CREATE INDEX idx_pvpp_date_days_ticker ON pvpp_result_history(date, days, ticker);
