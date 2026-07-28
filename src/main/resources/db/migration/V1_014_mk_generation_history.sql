CREATE TABLE IF NOT EXISTS mk_generation_history (
    date DATE NOT NULL,
    days INT NOT NULL,
    status VARCHAR(20) NOT NULL,
    error_message VARCHAR(1024),
    PRIMARY KEY (date, days)
);
