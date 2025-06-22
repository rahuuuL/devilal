package com.terminal_devilal.controllers.Functional.VWAP.Model;

import java.time.LocalDate;

public record VWAPDTO(LocalDate date, double close, double vwap, double vwapProximity) {
}
