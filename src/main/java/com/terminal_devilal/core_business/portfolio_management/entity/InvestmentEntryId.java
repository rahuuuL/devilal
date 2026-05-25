package com.terminal_devilal.core_business.portfolio_management.entity;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Composite primary key for InvestmentEntry.
 *
 * A ticker is unique per portfolio — the same ticker (e.g. RELIANCE) can exist
 * in "Primary" and "Retirement" portfolios simultaneously, so the key must be
 * (ticker, portfolio_name) rather than ticker alone.
 */
@Embeddable
public class InvestmentEntryId implements Serializable {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Column(name = "ticker", nullable = false)
    private String ticker;

    @Column(name = "portfolio_name", nullable = false)
    private String portfolioName;

    public InvestmentEntryId() {
        super();
    }

    public InvestmentEntryId(String ticker, String portfolioName) {
        this.ticker = ticker;
        this.portfolioName = portfolioName;
    }

    public String getTicker() {
        return ticker;
    }

    public void setTicker(String ticker) {
        this.ticker = ticker;
    }

    public String getPortfolioName() {
        return portfolioName;
    }

    public void setPortfolioName(String portfolioName) {
        this.portfolioName = portfolioName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof InvestmentEntryId)) return false;
        InvestmentEntryId that = (InvestmentEntryId) o;
        return Objects.equals(ticker, that.ticker) &&
               Objects.equals(portfolioName, that.portfolioName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ticker, portfolioName);
    }
}