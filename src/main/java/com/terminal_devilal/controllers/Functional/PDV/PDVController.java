package com.terminal_devilal.controllers.Functional.PDV;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.terminal_devilal.controllers.DataGathering.Model.PriceDeliveryVolume;
import com.terminal_devilal.controllers.DataGathering.Service.PriceDeliveryVolumeService;

@RestController
@RequestMapping("/api/devilal/pdv")
public class PDVController {

	private final PriceDeliveryVolumeService priceDeliveryVolumeService;

	public PDVController(PriceDeliveryVolumeService priceDeliveryVolumeService) {
		super();
		this.priceDeliveryVolumeService = priceDeliveryVolumeService;
	}

	@GetMapping("/getprices")
	public List<PriceDeliveryVolume> getHeatMap(@RequestParam("tickers") List<String> tickers) {
		return priceDeliveryVolumeService.getLatestRecordForTickers(tickers);
	}

}
