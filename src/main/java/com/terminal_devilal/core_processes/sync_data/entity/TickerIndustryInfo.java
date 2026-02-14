package com.terminal_devilal.core_processes.sync_data.entity;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.NamedNativeQuery;
import jakarta.persistence.Table;
import jakarta.persistence.SqlResultSetMapping;
import jakarta.persistence.ConstructorResult;
import jakarta.persistence.ColumnResult;

@Entity
@Table(name = "industry_details")
@NamedNativeQuery(name = "TickerIndustryInfo.fetchAllTickerDetailsFast", query = """
		    SELECT
		        i.ticker AS ticker,
		        i.company_name AS companyName,
		        i.isin AS isin,
		        i.macro AS macro,
		        i.sector AS sector,
		        i.industry AS industry,
		        i.basic_industry AS basicIndustry,

		        t.date AS detailsDate,
		        t.total_traded_volume AS totalTradedVolume,
		        t.total_traded_value AS totalTradedValue,
		        t.total_market_cap AS totalMarketCap,
		        t.ffmc AS ffmc,
		        t.impact_cost AS impactCost,
		        t.daily_volatility AS dailyVolatility,
		        t.annual_volatility AS annualVolatility

		    FROM industry_details i
		    LEFT JOIN (
		        SELECT *
		        FROM (
		            SELECT ti.*,
		                   ROW_NUMBER() OVER (PARTITION BY ti.ticker ORDER BY ti.date DESC) AS rn
		            FROM trade_Info ti
		        ) ranked
		        WHERE rn = 1
		    ) t
		    ON i.ticker = t.ticker
		""", resultSetMapping = "TickerDetailsMapping")

@SqlResultSetMapping(name = "TickerDetailsMapping", classes = @ConstructorResult(targetClass = com.terminal_devilal.core_processes.ticker_details.model.TickerDetailsResponse.class, columns = {
		@ColumnResult(name = "ticker", type = String.class), @ColumnResult(name = "companyName", type = String.class),
		@ColumnResult(name = "isin", type = String.class), @ColumnResult(name = "macro", type = String.class),
		@ColumnResult(name = "sector", type = String.class), @ColumnResult(name = "industry", type = String.class),
		@ColumnResult(name = "basicIndustry", type = String.class),

		@ColumnResult(name = "detailsDate", type = LocalDate.class),
		@ColumnResult(name = "totalTradedVolume", type = Double.class),
		@ColumnResult(name = "totalTradedValue", type = Double.class),
		@ColumnResult(name = "totalMarketCap", type = Double.class), @ColumnResult(name = "ffmc", type = Double.class),
		@ColumnResult(name = "impactCost", type = Double.class),
		@ColumnResult(name = "dailyVolatility", type = Double.class),
		@ColumnResult(name = "annualVolatility", type = Double.class) }))
public class TickerIndustryInfo {

	@Id
	@Column(name = "ticker", nullable = false, length = 20)
	private String ticker;

	@Column(name = "company_name")
	private String companyName;

	@Column(name = "isin")
	private String isin;

	@Column(name = "macro")
	private String macro;

	@Column(name = "sector")
	private String sector;

	@Column(name = "industry")
	private String industry;

	@Column(name = "basic_industry")
	private String basicIndustry;

	public String getTicker() {
		return ticker;
	}

	public void setTicker(String ticker) {
		this.ticker = ticker;
	}

	public String getCompanyName() {
		return companyName;
	}

	public void setCompanyName(String companyName) {
		this.companyName = companyName;
	}

	public String getIsin() {
		return isin;
	}

	public void setIsin(String isin) {
		this.isin = isin;
	}

	public String getMacro() {
		return macro;
	}

	public void setMacro(String macro) {
		this.macro = macro;
	}

	public String getSector() {
		return sector;
	}

	public void setSector(String sector) {
		this.sector = sector;
	}

	public String getIndustry() {
		return industry;
	}

	public void setIndustry(String industry) {
		this.industry = industry;
	}

	public String getBasicIndustry() {
		return basicIndustry;
	}

	public void setBasicIndustry(String basicIndustry) {
		this.basicIndustry = basicIndustry;
	}

	public TickerIndustryInfo() {
		super();
	}

	@Override
	public String toString() {
		return "TickerIndustryInfo [ticker=" + ticker + ", companyName=" + companyName + ", isin=" + isin + ", macro="
				+ macro + ", sector=" + sector + ", industry=" + industry + ", basicIndustry=" + basicIndustry + "]";
	}
}
