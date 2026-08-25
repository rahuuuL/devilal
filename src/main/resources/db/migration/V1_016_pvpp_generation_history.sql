CREATE TABLE IF NOT EXISTS pvpp_generation_history (
    date DATE NOT NULL,
    days INT NOT NULL,
    status VARCHAR(20) NOT NULL,
    error_message VARCHAR(1024),
    skipped_tickers VARCHAR(2048),
    PRIMARY KEY (date, days)
);
