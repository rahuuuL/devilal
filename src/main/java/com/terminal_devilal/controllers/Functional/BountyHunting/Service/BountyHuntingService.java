package com.terminal_devilal.controllers.Functional.BountyHunting.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.terminal_devilal.controllers.DataGathering.DAO.TradeInfoRepository;
import com.terminal_devilal.controllers.DataGathering.Model.TradeInfo;
import com.terminal_devilal.controllers.DataGathering.Service.PriceDeliveryVolumeService;
import com.terminal_devilal.controllers.Functional.BountyHunting.Model.BountyHuntingDTO;
import com.terminal_devilal.controllers.Functional.SharpeRatio.Model.SharpeRatioDTO;
import com.terminal_devilal.controllers.Functional.SharpeRatio.Service.SharpeRatioService;

@Service
public class BountyHuntingService {

	private PriceDeliveryVolumeService priceDeliveryVolumeService;

	private TradeInfoRepository tradeInfoDao;

	private SharpeRatioService sharpeRatioService;

	public BountyHuntingService(PriceDeliveryVolumeService priceDeliveryVolumeService, TradeInfoRepository tradeInfoDao,
			SharpeRatioService sharpeRatioService) {
		super();
		this.priceDeliveryVolumeService = priceDeliveryVolumeService;
		this.tradeInfoDao = tradeInfoDao;
		this.sharpeRatioService = sharpeRatioService;
	}

	public List<BountyHuntingDTO> getNextHunt(LocalDate fromDate, double riskFreeRate) {

		List<BountyHuntingDTO> dtoList = new ArrayList<>();

		Map<String, List<Double>> tickerMap = this.priceDeliveryVolumeService.getGroupedClosePrices(fromDate);

		for (Map.Entry<String, List<Double>> entry : tickerMap.entrySet()) {
			String ticker = entry.getKey();
			List<Double> prices = entry.getValue();

			// Skip if not enough data to calculate percentage change
			if (prices == null || prices.size() < 2) {
				continue;
			}

			// Calculate percentage change (from oldest to newest)
			double oldPrice = prices.get(0);
			double newPrice = prices.get(prices.size() - 1);
			double percentageChange = ((newPrice - oldPrice) / oldPrice) * 100;

			// Fetch TradeInfo from DAO
			TradeInfo tradeInfo = new TradeInfo();
			Optional<TradeInfo> optionalTradeInfo = tradeInfoDao.findFirstByTickerOrderByDateDesc(ticker);

			if (optionalTradeInfo.isEmpty()) {
				continue;
			}

			tradeInfo = optionalTradeInfo.get();

			// get sharpe sortino
			SharpeRatioDTO sharpeSortino = this.sharpeRatioService.calculateSharpeSortino(prices, riskFreeRate);

			// Construct DTO
			BountyHuntingDTO dto = new BountyHuntingDTO(ticker, percentageChange, tradeInfo.getDate(),
					tradeInfo.getTotalTradedVolume(), tradeInfo.getTotalTradedValue(), tradeInfo.getTotalMarketCap(),
					tradeInfo.getFfmc(), tradeInfo.getImpactCost(), tradeInfo.getCmDailyVolatility(),
					tradeInfo.getCmAnnualVolatility(), sharpeSortino.getRawSharpe(), sharpeSortino.getRawSortino(),
					sharpeSortino.getDaysUsed()

			);

			dtoList.add(dto);
		}

		// Sort before returning
		dtoList.sort(
			    Comparator.comparingDouble(BountyHuntingDTO::getPercentageChange)
			        .thenComparingDouble(BountyHuntingDTO::getTotalMarketCap).reversed()
			        .thenComparingDouble(BountyHuntingDTO::getRawSharpe).reversed()
			        .thenComparingDouble(BountyHuntingDTO::getRawSortino).reversed()
			);

		return dtoList;
	}

}
