package com.terminal_devilal.core_processes.sync_data.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class NseQuoteResponse {

	private Info info;
	private IndustryInfo industryInfo;

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Info {
		private String symbol;
		private String companyName;
		private String isin;
		private String listingDate;
		private String industry;

		public String getSymbol() {
			return symbol;
		}

		public String getCompanyName() {
			return companyName;
		}

		public String getIsin() {
			return isin;
		}

		public String getListingDate() {
			return listingDate;
		}

		public String getIndustry() {
			return industry;
		}

	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class IndustryInfo {
		private String macro;
		private String sector;
		private String industry;
		private String basicIndustry;

		public String getMacro() {
			return macro;
		}

		public String getSector() {
			return sector;
		}

		public String getIndustry() {
			return industry;
		}

		public String getBasicIndustry() {
			return basicIndustry;
		}

	}

	public Info getInfo() {
		return info;
	}

	public IndustryInfo getIndustryInfo() {
		return industryInfo;
	}

}
