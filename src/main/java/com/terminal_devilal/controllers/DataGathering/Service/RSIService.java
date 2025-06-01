package com.terminal_devilal.controllers.DataGathering.Service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.terminal_devilal.controllers.DataGathering.DAO.RSIDAO;
import com.terminal_devilal.controllers.DataGathering.Model.PriceDeliveryVolume;
import com.terminal_devilal.controllers.DataGathering.Model.RSI;

import jakarta.transaction.Transactional;

@Service
public class RSIService {

	private final RSIDAO rsidao;
	private final PriceDeliveryVolumeService priceDeliveryVolumeService;
	
	public RSIService(RSIDAO rsidao, PriceDeliveryVolumeService priceDeliveryVolumeService) {
		super();
		this.rsidao = rsidao;
		this.priceDeliveryVolumeService = priceDeliveryVolumeService;
	}
	
	@Transactional
	public void saveAllRSI(List<RSI> rsis) {
		this.rsidao.saveAll(rsis);
	}
	
	@Transactional
	public void saveRSI(RSI rsis) {
		this.rsidao.save(rsis);
	}
	
	public void processRSI(JsonNode jsonNode) {
		PriceDeliveryVolume pdv = this.priceDeliveryVolumeService.parseStockData(jsonNode);
		
		// Calc close diff
		double closeDiff = pdv.getClose() - pdv.getPrevoiusClosePrice();
		
		// Save RSI
		RSI rsi = new RSI(pdv.getTicker(), pdv.getDate(),closeDiff);
		saveRSI(rsi);
	}
}
