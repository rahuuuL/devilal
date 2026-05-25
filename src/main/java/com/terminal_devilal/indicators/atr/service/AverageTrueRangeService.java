package com.terminal_devilal.indicators.atr.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.terminal_devilal.indicators.atr.entities.AverageTrueRangeEntity;
import com.terminal_devilal.indicators.atr.entities.projections.TrueRangeProjection;
import com.terminal_devilal.indicators.atr.repository.AverageTrueRangeRepository;
import com.terminal_devilal.indicators.pdv.entities.PriceDeliveryVolumeEntity;
import com.terminal_devilal.utils.resilientbatchservice.ResilientBatchService;

import jakarta.transaction.Transactional;

@Service
public class AverageTrueRangeService extends ResilientBatchService<AverageTrueRangeEntity> {

	private AverageTrueRangeRepository averageTrueRangeRepository;

	public AverageTrueRangeService(AverageTrueRangeRepository averageTrueRangeRepository) {
		super();
		this.averageTrueRangeRepository = averageTrueRangeRepository;
	}

	// ── Your existing methods — completely unchanged ───────────────────────────

	@Transactional
	public void saveAllATR(List<AverageTrueRangeEntity> averageTrueRangeEntities) {
		this.averageTrueRangeRepository.saveAll(averageTrueRangeEntities);
	}

	@Transactional
	public void saveATR(AverageTrueRangeEntity averageTrueRangeEntities) {
		this.averageTrueRangeRepository.save(averageTrueRangeEntities);
	}

	public List<TrueRangeProjection> getLastNRecordsPerTicker(List<String> tickers, int n) {
		return averageTrueRangeRepository.findLastNRecordsPerTicker(tickers, n);
	}

	public double calculateTrueRange(double high, double low, double prevClose) {
		double range1 = high - low;
		double range2 = Math.abs(high - prevClose);
		double range3 = Math.abs(low - prevClose);
		return Math.max(range1, Math.max(range2, range3));
	}

	// ── Updated processATR — cache + resilient buffer ─────────────────────────

	public void processATR(PriceDeliveryVolumeEntity pdv) {
		double trueRange = calculateTrueRange(pdv.getHigh(), pdv.getLow(), pdv.getPrevoiusClosePrice());
		AverageTrueRangeEntity newEntity = new AverageTrueRangeEntity(pdv.getTicker(), pdv.getDate(), trueRange);
		enqueue(newEntity);
	}

	// ── ResilientBatchService wiring ──────────────────────────────────────────

	@Override
	protected void saveAll(List<AverageTrueRangeEntity> batch) {
		averageTrueRangeRepository.saveAll(batch);
	}

	@Override
	protected void saveOne(AverageTrueRangeEntity record) {
		averageTrueRangeRepository.save(record);
	}

	@Override
	protected void onPermanentFailure(AverageTrueRangeEntity record, Exception e) {
		System.err.printf("[ATR][PERMANENT_FAILURE] ticker=%s date=%s trueRange=%s error=%s%n", record.getTicker(),
				record.getDate(), record.getTrueRange(), e.getMessage());
	}
}
