package com.terminal_devilal.core_processes.sync_data.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class NseQuoteResponse {

	private List<EquityResponse> equityResponse;

	public List<EquityResponse> getEquityResponse() {
		return equityResponse;
	}

	public void setEquityResponse(List<EquityResponse> equityResponse) {
		this.equityResponse = equityResponse;
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class EquityResponse {
		private MetaData metaData;
		private SecInfo secInfo;

		public MetaData getMetaData() {
			return metaData;
		}

		public SecInfo getSecInfo() {
			return secInfo;
		}

		public void setMetaData(MetaData metaData) {
			this.metaData = metaData;
		}

		public void setSecInfo(SecInfo secInfo) {
			this.secInfo = secInfo;
		}
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class MetaData {
		private String symbol;
		private String companyName;
		private String isinCode;

		public String getSymbol() {
			return symbol;
		}

		public String getCompanyName() {
			return companyName;
		}

		public String getIsinCode() {
			return isinCode;
		}

		public void setSymbol(String symbol) {
			this.symbol = symbol;
		}

		public void setCompanyName(String companyName) {
			this.companyName = companyName;
		}

		public void setIsinCode(String isinCode) {
			this.isinCode = isinCode;
		}
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class SecInfo {
		private String macro;
		private String sector;

		@JsonProperty("industryInfo")
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

		public void setMacro(String macro) {
			this.macro = macro;
		}

		public void setSector(String sector) {
			this.sector = sector;
		}

		public void setIndustry(String industry) {
			this.industry = industry;
		}

		public void setBasicIndustry(String basicIndustry) {
			this.basicIndustry = basicIndustry;
		}
	}

	public EquityResponse getFirstEquityResponse() {
		if (equityResponse == null || equityResponse.isEmpty()) {
			return null;
		}
		return equityResponse.get(0);
	}

}
