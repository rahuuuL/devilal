package com.terminal_devilal.indicators.common_entities;

import java.time.LocalDate;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode
public class TickerDateId {
	
	@SuppressWarnings("unused")
	private String ticker;
    
	@SuppressWarnings("unused")
    private LocalDate date;

    // Default constructor
    public TickerDateId() {}

    public TickerDateId(String ticker, LocalDate date) {
        this.ticker = ticker;
        this.date = date;
    }

}
