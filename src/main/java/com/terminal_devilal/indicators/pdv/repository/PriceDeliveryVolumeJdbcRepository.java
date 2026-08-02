package com.terminal_devilal.indicators.pdv.repository;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.terminal_devilal.indicators.common_entities.TickerValue;
import com.terminal_devilal.indicators.pdv.enum_.PriceVolumeDeliveryColumn;

@Repository
public class PriceDeliveryVolumeJdbcRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public PriceDeliveryVolumeJdbcRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<TickerValue> fetchTickerValuesByColumn(LocalDate fromDate, LocalDate toDate, String inputColumnName) {
        String columnName = resolveColumnName(inputColumnName);

        String sql = """
                SELECT ticker, date, %s AS value
                FROM pdvt
                WHERE date >= :fromDate
                  AND date <= :toDate
                ORDER BY ticker, date
                """.formatted(columnName);

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("fromDate", Date.valueOf(fromDate))
                .addValue("toDate", Date.valueOf(toDate));

        return jdbcTemplate.query(sql, params, (rs, rowNum) ->
                new TickerValue(rs.getString("ticker"), rs.getDate("date").toLocalDate(), rs.getDouble("value")));
    }

    public List<TickerValue> fetchTickerValuesByColumn(LocalDate fromDate, LocalDate toDate, String inputColumnName,
            List<String> tickers) {
        if (tickers == null || tickers.isEmpty()) {
            return List.of();
        }

        String columnName = resolveColumnName(inputColumnName);

        String sql = """
                SELECT ticker, date, %s AS value
                FROM pdvt
                WHERE date >= :fromDate
                  AND date <= :toDate
                  AND ticker IN (:tickers)
                ORDER BY ticker, date
                """.formatted(columnName);

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("fromDate", Date.valueOf(fromDate))
                .addValue("toDate", Date.valueOf(toDate))
                .addValue("tickers", tickers);

        return jdbcTemplate.query(sql, params, (rs, rowNum) ->
                new TickerValue(rs.getString("ticker"), rs.getDate("date").toLocalDate(), rs.getDouble("value")));
    }

    private String resolveColumnName(String inputColumnName) {
        if (inputColumnName == null || inputColumnName.isBlank()) {
            throw new IllegalArgumentException("Invalid column name: " + inputColumnName);
        }

        for (PriceVolumeDeliveryColumn column : PriceVolumeDeliveryColumn.values()) {
            if (column.getColumnName().equalsIgnoreCase(inputColumnName)) {
                return column.getColumnName();
            }
        }

        throw new IllegalArgumentException("Invalid column name: " + inputColumnName);
    }
}
