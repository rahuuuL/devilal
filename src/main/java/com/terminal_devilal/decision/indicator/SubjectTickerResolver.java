package com.terminal_devilal.decision.indicator;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.terminal_devilal.core_processes.dfht.service.DataFetchHistoryService;
import com.terminal_devilal.core_business.portfolio_management.service.PortfolioService;
import com.terminal_devilal.core_business.watchlist.service.WatchlistService;

@Component
public class SubjectTickerResolver {
    private static final Logger log = LoggerFactory.getLogger(SubjectTickerResolver.class);
    private final DataFetchHistoryService dataFetchHistoryService;
    private final WatchlistService watchlistService;
    private final PortfolioService portfolioService;

    public SubjectTickerResolver(DataFetchHistoryService dataFetchHistoryService, WatchlistService watchlistService, PortfolioService portfolioService) {
        this.dataFetchHistoryService = dataFetchHistoryService;
        this.watchlistService = watchlistService;
        this.portfolioService = portfolioService;
    }

    public List<String> resolve(IndicatorEvaluationContext context) {
        if (context == null || context.getSubjectType() == null) {
            log.debug("SubjectTickerResolver: context or subjectType is null");
            return List.of();
        }
        List<String> tickers = switch (context.getSubjectType().toUpperCase()) {
            case "MARKET" -> {
                List<String> marketTickers = dataFetchHistoryService.getAllTickers();
                log.info("SubjectTickerResolver: Resolved MARKET tickers from dfht table. Count: {}, Tickers: {}", marketTickers.size(), marketTickers);
                yield marketTickers;
            }
            case "TICKER" -> {
                if (context.getSubjectId() == null || context.getSubjectId().isBlank()) {
                    log.warn("SubjectTickerResolver: TICKER subject type but subjectId is null or blank");
                    yield List.of();
                }
                List<String> tickerList = List.of(context.getSubjectId());
                log.info("SubjectTickerResolver: Resolved TICKER subject. SubjectId: {}", context.getSubjectId());
                yield tickerList;
            }
            case "WATCHLIST" -> {
                List<String> watchlistTickers = watchlistService.getTickers(context.getSubjectId());
                log.info("SubjectTickerResolver: Resolved WATCHLIST tickers. WatchlistId: {}, Count: {}, Tickers: {}", context.getSubjectId(), watchlistTickers.size(), watchlistTickers);
                yield watchlistTickers;
            }
            case "PORTFOLIO" -> {
                List<String> portfolioTickers = portfolioService.getTickers(context.getSubjectId());
                log.info("SubjectTickerResolver: Resolved PORTFOLIO tickers. PortfolioId: {}, Count: {}, Tickers: {}", context.getSubjectId(), portfolioTickers.size(), portfolioTickers);
                yield portfolioTickers;
            }
            default -> throw new IllegalArgumentException("Unsupported subject type: " + context.getSubjectType());
        };
        return tickers;
    }
}
