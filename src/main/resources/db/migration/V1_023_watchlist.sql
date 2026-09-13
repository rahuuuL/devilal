CREATE TABLE watchlist (
    name VARCHAR(255) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    last_updated_date DATE NOT NULL,
    PRIMARY KEY (name)
);

CREATE TABLE watchlist_ticker_entry (
    watchlist_name VARCHAR(255) NOT NULL,
    ticker VARCHAR(255) NOT NULL,
    added_date DATE NOT NULL,
    PRIMARY KEY (watchlist_name, ticker),
    CONSTRAINT fk_watchlist_ticker_entry_watchlist
        FOREIGN KEY (watchlist_name) REFERENCES watchlist(name)
);
