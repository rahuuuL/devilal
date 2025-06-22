package com.terminal_devilal.controllers.DataGathering.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.terminal_devilal.controllers.DataGathering.DAO.VWAPDAO;
import com.terminal_devilal.controllers.DataGathering.Model.PriceDeliveryVolume;
import com.terminal_devilal.controllers.DataGathering.Model.VWAP;

import jakarta.transaction.Transactional;

@Service
public class VWAPService {

	private final VWAPDAO vwapdao;

	private final PriceDeliveryVolumeService priceDeliveryVolumeService;

	public VWAPService(VWAPDAO vwapdao, PriceDeliveryVolumeService priceDeliveryVolumeService) {
		super();
		this.vwapdao = vwapdao;
		this.priceDeliveryVolumeService = priceDeliveryVolumeService;
	}

	@Transactional
	public void saveAllVWAP(List<VWAP> vwaps) {
		this.vwapdao.saveAll(vwaps);
	}

	@Transactional
	public void saveVWAP(VWAP vwap) {
		this.vwapdao.save(vwap);
	}

	public Map<String, List<VWAP>> getVwapDataFromDate(LocalDate fromDate) {
		List<VWAP> data = vwapdao.findByDateGreaterThanEqualOrderByDateAsc(fromDate);
		return data.stream().collect(Collectors.groupingBy(VWAP::getTicker));
	}

	public List<VWAP> getVWAPForTickerFromDate(String ticker, LocalDate fromDate) {
		return vwapdao.findByTickerAndDateGreaterThanEqualOrderByDateAsc(ticker, fromDate);
	}

	public void processVwap(JsonNode jsonNode) {

		PriceDeliveryVolume pdv = this.priceDeliveryVolumeService.parseStockData(jsonNode);

		// Calc Vwap Proximity
		double vwapProximity = (pdv.getClose() - pdv.getVwap()) / pdv.getVwap() * 100;

		// Save Vwap
		VWAP vwap = new VWAP(pdv.getTicker(), pdv.getDate(), pdv.getClose(), pdv.getVwap(), vwapProximity);
		saveVWAP(vwap);
	}

}
