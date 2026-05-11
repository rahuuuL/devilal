package com.terminal_devilal.indicators.vwap.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.terminal_devilal.indicators.pdv.entities.PriceDeliveryVolumeEntity;
import com.terminal_devilal.indicators.vwap.entities.VWAPEntity;
import com.terminal_devilal.indicators.vwap.repository.VWAPRepository;
import com.terminal_devilal.utils.resilientbatchservice.ResilientBatchService;

import jakarta.transaction.Transactional;

@Service
public class VWAPService extends ResilientBatchService<VWAPEntity> {

	private final VWAPRepository vWAPRepository;

	public VWAPService(VWAPRepository vWAPRepository) {
		super();
		this.vWAPRepository = vWAPRepository;
	}

	@Transactional
	public void saveAllVWAP(List<VWAPEntity> vwaps) {
		this.vWAPRepository.saveAll(vwaps);
	}

	@Transactional
	public void saveVWAP(VWAPEntity vWAPEntity) {
		this.vWAPRepository.save(vWAPEntity);
	}

	public Map<String, List<VWAPEntity>> getVwapDataFromDate(LocalDate fromDate) {
		List<VWAPEntity> data = vWAPRepository.findByDateGreaterThanEqualOrderByDateAsc(fromDate);
		return data.stream().collect(Collectors.groupingBy(VWAPEntity::getTicker));
	}

	public List<VWAPEntity> getVWAPForTickerFromDate(String ticker, LocalDate fromDate) {
		return vWAPRepository.findByTickerAndDateGreaterThanEqualOrderByDateAsc(ticker, fromDate);
	}

	public void processVwap(PriceDeliveryVolumeEntity pdv) {
		// Guard against division by zero if VWAP is missing or zero
		if (pdv.getVwap() == 0) {
			System.err.printf("[VWAP] Skipping ticker=%s date=%s — VWAP is zero%n", pdv.getTicker(), pdv.getDate());
			return;
		}

		double vwapProximity = (pdv.getClose() - pdv.getVwap()) / pdv.getVwap() * 100;

		VWAPEntity vWAPEntity = new VWAPEntity(pdv.getTicker(), pdv.getDate(), pdv.getClose(), pdv.getVwap(),
				vwapProximity);

		// Hand off to the resilient buffer — actual DB write happens in flushBuffer()
		enqueue(vWAPEntity);
	}

	// ─── ResilientBatchService implementation ─────────────────────────────────

	@Override
	protected void saveAll(List<VWAPEntity> batch) {
		vWAPRepository.saveAll(batch);
	}

	@Override
	protected void saveOne(VWAPEntity record) {
		vWAPRepository.save(record);
	}

	@Override
	protected void onPermanentFailure(VWAPEntity record, Exception e) {
		System.err.printf("[VWAP][PERMANENT_FAILURE] ticker=%s date=%s close=%s vwap=%s error=%s%n", record.getTicker(),
				record.getDate(), record.getClosePrice(), record.getVwap(), e.getMessage());
	}

}
