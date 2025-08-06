package com.terminal_devilal.controllers.DataGathering.DAO;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

import com.terminal_devilal.controllers.DataGathering.Enums.PriceVolumeDeliveryColumn;
import com.terminal_devilal.controllers.DataGathering.Model.PriceDeliveryVolume;
import com.terminal_devilal.controllers.DataGathering.Model.TickerValue;

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

	public List<TickerValue> getTickerValues(LocalDate from, String columnName) {
		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<Tuple> cq = cb.createTupleQuery();
		Root<PriceDeliveryVolume> root = cq.from(PriceDeliveryVolume.class);

		cq.multiselect(root.get("ticker").alias("ticker"), root.get(columnName).alias("value"));
		cq.where(cb.greaterThanOrEqualTo(root.<LocalDate>get("date"), from));
		cq.orderBy(cb.asc(root.get("date")));

		return entityManager.createQuery(cq).getResultList().stream().map(tuple -> {
			String ticker = tuple.get("ticker", String.class);
			Object valueObj = tuple.get("value"); // get as Object

			Double value;
			if (valueObj instanceof Number) {
				value = ((Number) valueObj).doubleValue();
			} else {
				value = 0.0;
			}
			return new TickerValue(ticker, value);
		}).collect(Collectors.toList());
	}

	public Map<String, List<Double>> fetchTickerValuesByColumn(LocalDate from, String inputColumnName) {
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
		return getTickerValues(from, matchedColumn.getColumnName()).stream().collect(Collectors
				.groupingBy(TickerValue::getTicker, Collectors.mapping(TickerValue::getValue, Collectors.toList())));
	}

	public List<TickerValue> getTickerValues(LocalDate from, String columnName, List<String> tickers) {
		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<Tuple> cq = cb.createTupleQuery();
		Root<PriceDeliveryVolume> root = cq.from(PriceDeliveryVolume.class);

		cq.multiselect(root.get("ticker").alias("ticker"), root.get(columnName).alias("value"));
		cq.where(cb.and(cb.greaterThanOrEqualTo(root.get("date"), from), root.get("ticker").in(tickers) // filter by
																										// tickers list
		));
		cq.orderBy(cb.asc(root.get("date")));

		return entityManager.createQuery(cq).getResultList().stream().map(tuple -> {
			String ticker = tuple.get("ticker", String.class);
			Object valueObj = tuple.get("value");

			Double value;
			if (valueObj instanceof Number) {
				value = ((Number) valueObj).doubleValue();
			} else {
				value = 0.0; // or handle null/default as needed
			}

			return new TickerValue(ticker, value);
		}).collect(Collectors.toList());
	}

	public Map<String, List<Double>> fetchTickerValuesByColumn(LocalDate from, String inputColumnName,
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
		return getTickerValues(from, matchedColumn.getColumnName(), tickers).stream().collect(Collectors
				.groupingBy(TickerValue::getTicker, Collectors.mapping(TickerValue::getValue, Collectors.toList())));
	}

}
