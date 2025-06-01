package com.terminal_devilal.controllers.DataGathering.Service;

import java.util.List;

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

	public void processVwap(JsonNode jsonNode) {

		PriceDeliveryVolume pdv = this.priceDeliveryVolumeService.parseStockData(jsonNode);

		// Calc Vwap Proximity
		double vwapProximity = (pdv.getClose() - pdv.getVwap()) / pdv.getVwap() * 100;

		// Save Vwap
		VWAP vwap = new VWAP(pdv.getTicker(), pdv.getDate(), pdv.getClose(), pdv.getVwap(), vwapProximity);
		saveVWAP(vwap);
	}

}
