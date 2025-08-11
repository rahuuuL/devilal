package com.terminal_devilal.indicators.vwap.dto;

import java.time.LocalDate;

public record VWAPDTO(LocalDate date, double close, double vwap, double vwapProximity) {
}
