package com.terminal_devilal.business_tools.ratio_analysis.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.terminal_devilal.business_tools.ratio_analysis.dto.RatioTImeSeries;
import com.terminal_devilal.indicators.pdv.entities.projections.ClosePriceProjection;
import com.terminal_devilal.indicators.pdv.service.PriceDeliveryVolumeService;

@Service
public class SharpeRatioService {

	private final PriceDeliveryVolumeService priceDeliveryVolumeService;

	public SharpeRatioService(PriceDeliveryVolumeService priceDeliveryVolumeService) {
		super();
		this.priceDeliveryVolumeService = priceDeliveryVolumeService;
	}

	public List<RatioTImeSeries> computeRatiosForTimeFrame(List<String> tickers, LocalDate fromDate, LocalDate toDate,
			double riskFreeRate, int window) {

		final int WINDOW = Math.max(window, 20);

		List<ClosePriceProjection> allData = priceDeliveryVolumeService
				.ClosePricesWithBufferInDateRangeForTickers(fromDate, toDate, tickers, WINDOW);

		List<RatioTImeSeries> result = new ArrayList<>();

		String currentTicker = null;

		double previousClose = 0;

		RollingSharpeSortino rolling = new RollingSharpeSortino(WINDOW);

		boolean firstRowForTicker = true;

		for (ClosePriceProjection row : allData) {

			String ticker = row.getTicker();

			if (!ticker.equals(currentTicker)) {

				currentTicker = ticker;

				previousClose = 0;

				rolling = new RollingSharpeSortino(WINDOW);

				firstRowForTicker = true;
			}

			double close = row.getClose();

			if (firstRowForTicker) {

				previousClose = close;

				firstRowForTicker = false;

				continue;
			}

			double dailyReturn = (close - previousClose) / previousClose;

			rolling.add(dailyReturn);

			if (rolling.isReady()) {

				RatioTImeSeries out = new RatioTImeSeries();

				out.setTicker(ticker);
				out.setDate(row.getDate());
				out.setSharpeRatio(rolling.getSharpe(riskFreeRate));
				out.setSortinoRatio(rolling.getSortino(riskFreeRate));

				result.add(out);
			}

			previousClose = close;
		}

		return result;
	}

}
