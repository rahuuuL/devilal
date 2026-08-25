package com.terminal_devilal.business_tools.pvpp.calculator;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.terminal_devilal.business_tools.pvpp.entity.PvppDailyEntity;
import com.terminal_devilal.business_tools.pvpp.entity.PvppResultHistoryEntity;
import com.terminal_devilal.indicators.pdv.entity.PriceDeliveryVolumeEntity;

@Component
public class PvppCalculatorImpl implements PvppCalculator {

    @Override
    public PvppCalcResult computeAllWindows(String ticker, List<PriceDeliveryVolumeEntity> data, List<Integer> enabledDays) {
        return compute(ticker, data, enabledDays, null);
    }

    @Override
    public PvppCalcResult computeForDate(String ticker, List<PriceDeliveryVolumeEntity> data, List<Integer> enabledDays,
            LocalDate targetDate) {
        if (targetDate == null) {
            throw new IllegalArgumentException("targetDate must not be null");
        }
        return compute(ticker, data, enabledDays, targetDate);
    }

    private PvppCalcResult compute(String ticker, List<PriceDeliveryVolumeEntity> data, List<Integer> enabledDays,
            LocalDate targetDate) {
        PvppCalcResult result = new PvppCalcResult();
        if (ticker == null || data == null || data.isEmpty() || enabledDays == null || enabledDays.isEmpty()) {
            return result;
        }

        List<PriceDeliveryVolumeEntity> sorted = new ArrayList<>(data);
        sorted.sort(Comparator.comparing(PriceDeliveryVolumeEntity::getDate));

        Map<Integer, double[]> rvolByDays = new HashMap<>();
        for (Integer days : enabledDays) {
            if (days == null || days <= 0) {
                continue;
            }
            double[] rvolForDay = new double[sorted.size()];
            double[] prefix = new double[sorted.size() + 1];
            for (int i = 0; i < sorted.size(); i++) {
                prefix[i + 1] = prefix[i] + sorted.get(i).getVolume();
            }

            for (int i = 0; i < sorted.size(); i++) {
                if (i + 1 < days) {
                    rvolForDay[i] = Double.NaN;
                    continue;
                }
                int startIndex = i - days + 1;
                double windowSum = prefix[i + 1] - prefix[startIndex];
                double averageVolume = windowSum / days;
                double currentVolume = sorted.get(i).getVolume();
                if (averageVolume <= 0.0d) {
                    rvolForDay[i] = Double.NaN;
                } else {
                    rvolForDay[i] = currentVolume / averageVolume;
                }
            }
            rvolByDays.put(days, rvolForDay);
        }

        for (int i = 0; i < sorted.size(); i++) {
            PriceDeliveryVolumeEntity current = sorted.get(i);

            // Only emit output rows for the requested date; earlier rows are only used to feed the rolling windows
            if (targetDate != null && !targetDate.equals(current.getDate())) {
                continue;
            }

            PvppCalcResult.PvppRow row = calculateDailyRow(current);

            if (row == null) {
                continue;
            }

            PvppDailyEntity dailyEntity = new PvppDailyEntity();
            dailyEntity.setTicker(ticker);
            dailyEntity.setDate(current.getDate());
            dailyEntity.setReturnPct(row.getReturnPct());
            dailyEntity.setVolume(current.getVolume());
            dailyEntity.setClv(row.getClv());
            dailyEntity.setCenteredClv(row.getCenteredClv());
            dailyEntity.setReturnZ(null);
            dailyEntity.setClvZ(null);
            result.addDailyRow(dailyEntity);

            for (Integer days : enabledDays) {
                if (days == null || days <= 0) {
                    continue;
                }
                double[] rvolSeries = rvolByDays.get(days);
                if (rvolSeries == null || i >= rvolSeries.length) {
                    continue;
                }
                double rvol = rvolSeries[i];
                if (Double.isNaN(rvol) || Double.isInfinite(rvol)) {
                    continue;
                }

                PvppResultHistoryEntity historyEntity = new PvppResultHistoryEntity();
                historyEntity.setTicker(ticker);
                historyEntity.setDate(current.getDate());
                historyEntity.setDays(days);
                historyEntity.setRvol(rvol);
                historyEntity.setEfficiency(calculateEfficiency(row.getReturnPct(), rvol));
                historyEntity.setLogRvolZ(null);
                historyEntity.setPressureScore(null);
                result.addHistoryRow(historyEntity);
            }
        }

        return result;
    }

    @Override
    public PvppCalcResult.PvppRow calculateDailyRow(PriceDeliveryVolumeEntity current) {
        if (current == null) {
            return null;
        }

        PvppCalcResult.PvppRow row = new PvppCalcResult.PvppRow();
        row.setTicker(current.getTicker());
        row.setDate(current.getDate());
        row.setVolume(current.getVolume());
        row.setReturnPct(calculateReturnPct(current));
        row.setClv(calculateClv(current));
        row.setCenteredClv(calculateCenteredClv(current));
        return row;
    }

    @Override
    public Double calculateReturnPct(PriceDeliveryVolumeEntity current) {
        if (current == null) {
            return null;
        }
        double prevClose = current.getPrevoiusClosePrice();
        if (prevClose == 0.0d) {
            return null;
        }
        return (current.getClose() / prevClose) - 1.0d;
    }

    @Override
    public Double calculateClv(PriceDeliveryVolumeEntity current) {
        if (current == null) {
            return null;
        }
        double high = current.getHigh();
        double low = current.getLow();
        if (high == low) {
            return 0.5d;
        }
        return (current.getClose() - low) / (high - low);
    }

    @Override
    public Double calculateCenteredClv(PriceDeliveryVolumeEntity current) {
        Double clv = calculateClv(current);
        if (clv == null) {
            return null;
        }
        return 2.0d * clv - 1.0d;
    }

    @Override
    public Double calculateRvol(List<PriceDeliveryVolumeEntity> data, int days, int index) {
        if (data == null || data.isEmpty() || index < 0 || days <= 0) {
            return null;
        }
        if (index + 1 < days) {
            return null;
        }

        int startIndex = index - days + 1;
        int endIndex = index + 1;
        double sum = 0.0d;
        int count = 0;
        for (int i = startIndex; i < endIndex; i++) {
            if (i < 0 || i >= data.size()) {
                continue;
            }
            sum += data.get(i).getVolume();
            count++;
        }

        if (count == 0) {
            return null;
        }
        double smaVolume = sum / count;
        if (smaVolume <= 0.0d) {
            return null;
        }
        return data.get(index).getVolume() / smaVolume;
    }

    @Override
    public Double calculateEfficiency(Double returnPct, Double rvol) {
        if (returnPct == null || rvol == null || rvol == 0.0d) {
            return null;
        }
        return Math.abs(returnPct) / rvol;
    }
}
