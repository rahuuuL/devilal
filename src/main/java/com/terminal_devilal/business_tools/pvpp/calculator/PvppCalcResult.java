package com.terminal_devilal.business_tools.pvpp.calculator;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.terminal_devilal.business_tools.pvpp.entity.PvppResultHistoryEntity;

public class PvppCalcResult {

    private final List<PvppResultHistoryEntity> historyRows = new ArrayList<>();
    private final List<String> skippedTickers = new ArrayList<>();

    public void addHistoryRow(PvppResultHistoryEntity row) {
        historyRows.add(row);
    }

    public void addSkippedTicker(String ticker) {
        if (ticker != null && !ticker.isBlank() && !skippedTickers.contains(ticker)) {
            skippedTickers.add(ticker);
        }
    }

    public List<PvppResultHistoryEntity> getHistoryRows() {
        return historyRows;
    }

    public List<String> getSkippedTickers() {
        return skippedTickers;
    }

    public void clear() {
        historyRows.clear();
        skippedTickers.clear();
    }

    public boolean hasHistoryRows() {
        return !historyRows.isEmpty();
    }

    public static class PvppRow {
        private String ticker;
        private LocalDate date;
        private Double returnPct;
        private Double clv;
        private Double centeredClv;
        private Long volume;
        private Double rvol;
        private Double efficiency;

        public PvppRow() {
        }

        public String getTicker() {
            return ticker;
        }

        public void setTicker(String ticker) {
            this.ticker = ticker;
        }

        public LocalDate getDate() {
            return date;
        }

        public void setDate(LocalDate date) {
            this.date = date;
        }

        public Double getReturnPct() {
            return returnPct;
        }

        public void setReturnPct(Double returnPct) {
            this.returnPct = returnPct;
        }

        public Double getClv() {
            return clv;
        }

        public void setClv(Double clv) {
            this.clv = clv;
        }

        public Double getCenteredClv() {
            return centeredClv;
        }

        public void setCenteredClv(Double centeredClv) {
            this.centeredClv = centeredClv;
        }

        public Long getVolume() {
            return volume;
        }

        public void setVolume(Long volume) {
            this.volume = volume;
        }

        public Double getRvol() {
            return rvol;
        }

        public void setRvol(Double rvol) {
            this.rvol = rvol;
        }

        public Double getEfficiency() {
            return efficiency;
        }

        public void setEfficiency(Double efficiency) {
            this.efficiency = efficiency;
        }
    }
}
