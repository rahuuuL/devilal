package com.terminal_devilal.indicators.vwap.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.terminal_devilal.indicators.pdv.entities.PriceDeliveryVolumeEntity;
import com.terminal_devilal.indicators.pdv.service.PriceDeliveryVolumeUtility;
import com.terminal_devilal.indicators.vwap.entities.VWAPEntity;
import com.terminal_devilal.indicators.vwap.repository.VWAPRepository;

import jakarta.transaction.Transactional;

@Service
public class VWAPService {

	private final VWAPRepository vWAPRepository;

	private final PriceDeliveryVolumeUtility priceDeliveryVolumeUtility;

	public VWAPService(VWAPRepository vWAPRepository, PriceDeliveryVolumeUtility priceDeliveryVolumeUtility) {
		super();
		this.vWAPRepository = vWAPRepository;
		this.priceDeliveryVolumeUtility = priceDeliveryVolumeUtility;
	}

	@Transactional
	public void saveAllVWAP(List<VWAPEntity> vwaps) {
		this.vWAPRepository.saveAll(vwaps);
	}

	@Transactional
	public void saveVWAP(VWAPEntity vWAPEntity) {
		this.vWAPRepository.save(vWAPEntity);
	}

	public Map<String, List<VWAPEntity>> getVwapDataFromDate(LocalDate fromDate) {
		List<VWAPEntity> data = vWAPRepository.findByDateGreaterThanEqualOrderByDateAsc(fromDate);
		return data.stream().collect(Collectors.groupingBy(VWAPEntity::getTicker));
	}

	public List<VWAPEntity> getVWAPForTickerFromDate(String ticker, LocalDate fromDate) {
		return vWAPRepository.findByTickerAndDateGreaterThanEqualOrderByDateAsc(ticker, fromDate);
	}

	public void processVwap(JsonNode jsonNode) {

		PriceDeliveryVolumeEntity pdv = this.priceDeliveryVolumeUtility.parseStockData(jsonNode);

		// Calc Vwap Proximity
		double vwapProximity = (pdv.getClose() - pdv.getVwap()) / pdv.getVwap() * 100;

		// Save Vwap
		VWAPEntity vWAPEntity = new VWAPEntity(pdv.getTicker(), pdv.getDate(), pdv.getClose(), pdv.getVwap(), vwapProximity);
		saveVWAP(vWAPEntity);
	}

}
