DROP TABLE IF EXISTS pvpp_result_history;
DROP TABLE IF EXISTS pvpp_daily;

CREATE TABLE pvpp_result_history (
    ticker VARCHAR(50) NOT NULL,
    date DATE NOT NULL,
    days INT NOT NULL,
    return_pct DOUBLE,
    volume BIGINT,
    clv DOUBLE,
    centered_clv DOUBLE,
    rvol DOUBLE,
    efficiency DOUBLE,
    PRIMARY KEY (ticker, date, days)
);

CREATE INDEX idx_pvpp_date_days_ticker ON pvpp_result_history(date, days, ticker);
