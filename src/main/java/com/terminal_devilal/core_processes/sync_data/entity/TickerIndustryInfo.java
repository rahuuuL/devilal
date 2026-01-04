package com.terminal_devilal.core_processes.sync_data.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "industry_details")
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
