package com.terminal_devilal.business_tools.mannkendall.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.terminal_devilal.business_tools.mannkendall.dto.MannKendallResponse;
import com.terminal_devilal.business_tools.ratio_analysis.dto.SharpeSortinoHistoryResponse;
import com.terminal_devilal.business_tools.ratio_analysis.service.SharpeSortinoHistoryService;
import com.terminal_devilal.indicators.pdv.service.PriceDeliveryVolumeService;

class AnalyzeMannKendallForTickerTest {

    private final PriceDeliveryVolumeService priceDeliveryVolumeService = mock(PriceDeliveryVolumeService.class);
    private final SharpeSortinoHistoryService sharpeSortinoHistoryService = mock(SharpeSortinoHistoryService.class);
    private final AnalyzeMannKendallForTicker analyzer = new AnalyzeMannKendallForTicker(
            priceDeliveryVolumeService, sharpeSortinoHistoryService);

    @Test
    void calculatesTrendFromChronologicalSortinoValuesWithoutLogTransform() {
        LocalDate fromDate = LocalDate.of(2025, 1, 1);
        LocalDate toDate = LocalDate.of(2025, 1, 3);
        List<SharpeSortinoHistoryResponse> history = List.of(
                row("TICKER", LocalDate.of(2025, 1, 3), -1.0d),
                row("TICKER", LocalDate.of(2025, 1, 1), -3.0d),
                row("TICKER", LocalDate.of(2025, 1, 2), -2.0d));
        when(sharpeSortinoHistoryService.getHistory(fromDate, toDate, 20, List.of("TICKER")))
                .thenReturn(history);

        List<MannKendallResponse> results = analyzer.getSortinoMannKendallTrendAnalysis(
                fromDate, toDate, 20, List.of("TICKER"));

        assertEquals(1, results.size());
        assertEquals("TICKER", results.get(0).getTicker());
        assertEquals("increasing", results.get(0).getTrend());
        assertEquals(1.0d, results.get(0).getSlope(), 1.0e-9d);
        verify(sharpeSortinoHistoryService).getHistory(fromDate, toDate, 20, List.of("TICKER"));
    }

    private static SharpeSortinoHistoryResponse row(String ticker, LocalDate date, Double sortinoRatio) {
        SharpeSortinoHistoryResponse response = new SharpeSortinoHistoryResponse();
        response.setTicker(ticker);
        response.setDate(date);
        response.setSortinoRatio(sortinoRatio);
        return response;
    }
}
