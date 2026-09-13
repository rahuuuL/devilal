package com.terminal_devilal.decision.indicator;

import java.util.List;

import org.springframework.stereotype.Component;

import com.terminal_devilal.core_processes.dfht.service.DataFetchHistoryService;
import com.terminal_devilal.core_business.portfolio_management.service.PortfolioService;
import com.terminal_devilal.core_business.watchlist.service.WatchlistService;

@Component
public class SubjectTickerResolver {
    private final DataFetchHistoryService dataFetchHistoryService;
    private final WatchlistService watchlistService;
    private final PortfolioService portfolioService;

    public SubjectTickerResolver(DataFetchHistoryService dataFetchHistoryService, WatchlistService watchlistService, PortfolioService portfolioService) {
        this.dataFetchHistoryService = dataFetchHistoryService;
        this.watchlistService = watchlistService;
        this.portfolioService = portfolioService;
    }

    public List<String> resolve(IndicatorEvaluationContext context) {
        if (context == null || context.getSubjectType() == null) return List.of();
        return switch (context.getSubjectType().toUpperCase()) {
            case "MARKET" -> dataFetchHistoryService.getAllTickers();
            case "TICKER" -> context.getSubjectId() == null || context.getSubjectId().isBlank()
                    ? List.of() : List.of(context.getSubjectId());
            case "WATCHLIST" -> watchlistService.getTickers(context.getSubjectId());
            case "PORTFOLIO" -> portfolioService.getTickers(context.getSubjectId());
            default -> throw new IllegalArgumentException("Unsupported subject type: " + context.getSubjectType());
        };
    }
}
