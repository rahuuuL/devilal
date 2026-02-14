package com.terminal_devilal.indicators.pdv.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

import com.terminal_devilal.indicators.common_entities.TickerValue;
import com.terminal_devilal.indicators.pdv.entities.PriceDeliveryVolumeEntity;
import com.terminal_devilal.indicators.pdv.enum_.PriceVolumeDeliveryColumn;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

@Repository
public class PriceDeliveryVolumeRepositoryCustomImpl {

	@PersistenceContext
	private EntityManager entityManager;

	public List<TickerValue> getTickerValues(LocalDate from, LocalDate to, String columnName) {

		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<Tuple> cq = cb.createTupleQuery();
		Root<PriceDeliveryVolumeEntity> root = cq.from(PriceDeliveryVolumeEntity.class);

		cq.multiselect(root.get("ticker").alias("ticker"), root.get("date").alias("date"),
				root.get(columnName).alias("value"));

		cq.where(cb.and(cb.greaterThanOrEqualTo(root.get("date"), from), cb.lessThanOrEqualTo(root.get("date"), to)));

		// IMPORTANT: order by ticker + date for rolling logic
		cq.orderBy(cb.asc(root.get("ticker")), cb.asc(root.get("date")));

		return entityManager.createQuery(cq).getResultList().stream().map(tuple -> {
			String ticker = tuple.get("ticker", String.class);
			LocalDate date = tuple.get("date", LocalDate.class);

			Object valueObj = tuple.get("value");
			double value = (valueObj instanceof Number) ? ((Number) valueObj).doubleValue() : 0.0;

			return new TickerValue(ticker, date, value);
		}).collect(Collectors.toList());
	}

	public Map<String, List<Double>> fetchTickerValuesByColumn(LocalDate from, LocalDate to, String inputColumnName) {
		// Match input string with enum
		PriceVolumeDeliveryColumn matchedColumn = null;
		for (PriceVolumeDeliveryColumn col : PriceVolumeDeliveryColumn.values()) {
			if (col.getColumnName().equalsIgnoreCase(inputColumnName)) {
				matchedColumn = col;
				break;
			}
		}

		// If no valid column found, handle gracefully
		if (matchedColumn == null) {
			throw new IllegalArgumentException("Invalid column name: " + inputColumnName);
		}

		// Call the method with matched column name
		return getTickerValues(from, to, matchedColumn.getColumnName()).stream().collect(Collectors
				.groupingBy(TickerValue::getTicker, Collectors.mapping(TickerValue::getValue, Collectors.toList())));
	}

	public List<TickerValue> getTickerValues(LocalDate from, LocalDate to, String columnName, List<String> tickers) {

		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<Tuple> cq = cb.createTupleQuery();
		Root<PriceDeliveryVolumeEntity> root = cq.from(PriceDeliveryVolumeEntity.class);

		cq.multiselect(root.get("ticker").alias("ticker"), root.get("date").alias("date"),
				root.get(columnName).alias("value"));

		cq.where(cb.and(cb.greaterThanOrEqualTo(root.get("date"), from), cb.lessThanOrEqualTo(root.get("date"), to),
				root.get("ticker").in(tickers)));

		// CRITICAL: correct ordering for single-pass rolling logic
		cq.orderBy(cb.asc(root.get("ticker")), cb.asc(root.get("date")));

		return entityManager.createQuery(cq).getResultList().stream()
				.map(tuple -> new TickerValue(tuple.get("ticker", String.class), tuple.get("date", LocalDate.class),
						tuple.get("value") instanceof Number ? ((Number) tuple.get("value")).doubleValue() : 0.0))
				.collect(Collectors.toList());
	}

	public Map<String, List<Double>> fetchTickerValuesByColumn(LocalDate from, LocalDate to, String inputColumnName,
			List<String> tickers) {
		// Match input string with enum
		PriceVolumeDeliveryColumn matchedColumn = null;
		for (PriceVolumeDeliveryColumn col : PriceVolumeDeliveryColumn.values()) {
			if (col.getColumnName().equalsIgnoreCase(inputColumnName)) {
				matchedColumn = col;
				break;
			}
		}

		// If no valid column found, handle gracefully
		if (matchedColumn == null) {
			throw new IllegalArgumentException("Invalid column name: " + inputColumnName);
		}

		// Call the method with matched column name and filtered tickers
		return getTickerValues(from, to, matchedColumn.getColumnName(), tickers).stream().collect(Collectors
				.groupingBy(TickerValue::getTicker, Collectors.mapping(TickerValue::getValue, Collectors.toList())));
	}

}
