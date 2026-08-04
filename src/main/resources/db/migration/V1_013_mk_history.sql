CREATE TABLE IF NOT EXISTS mk_config (
    days INT PRIMARY KEY,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    description VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS mk_result_history (
    ticker VARCHAR(50) NOT NULL,
    date DATE NOT NULL,
    days INT NOT NULL,
    score DOUBLE,
    trend VARCHAR(50),
    h BOOLEAN,
    p DOUBLE,
    z DOUBLE,
    tau DOUBLE,
    s DOUBLE,
    var_s DOUBLE,
    slope DOUBLE,
    intercept DOUBLE,
    PRIMARY KEY (ticker, date, days)
);
-- Alternate Index 
CREATE INDEX idx_date_days_ticker ON mk_result_history(date, days, ticker);