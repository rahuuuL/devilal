package com.terminal_devilal.controllers.DataGathering.Model;

import java.time.LocalDate;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode
public class PriceDeliveryVolumeId {
	
	@SuppressWarnings("unused")
	private String ticker;
    
	@SuppressWarnings("unused")
    private LocalDate date;

    // Default constructor
    public PriceDeliveryVolumeId() {}

    public PriceDeliveryVolumeId(String ticker, LocalDate date) {
        this.ticker = ticker;
        this.date = date;
    }

}
