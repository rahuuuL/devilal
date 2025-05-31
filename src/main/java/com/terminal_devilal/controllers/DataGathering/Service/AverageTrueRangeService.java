package com.terminal_devilal.controllers.DataGathering.Service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.terminal_devilal.controllers.DataGathering.DAO.AverageTrueRangeDAO;
import com.terminal_devilal.controllers.DataGathering.Model.AverageTrueRange;

import jakarta.transaction.Transactional;

@Service
public class AverageTrueRangeService {

	private AverageTrueRangeDAO averageTrueRangeDAO;

	public AverageTrueRangeService(AverageTrueRangeDAO averageTrueRangeDAO) {
		super();
		this.averageTrueRangeDAO = averageTrueRangeDAO;
	}
	
	@Transactional
	public void saveAllATR(List<AverageTrueRange> averageTrueRanges) {
		this.averageTrueRangeDAO.saveAll(averageTrueRanges);
	}
	
	@Transactional
	public void saveATR(AverageTrueRange averageTrueRanges) {
		this.averageTrueRangeDAO.save(averageTrueRanges);
	}
	
    /**
     * Calculates the True Range (TR) for a single period.
     *
     * @param high Today's high price
     * @param low Today's low price
     * @param prevClose Previous day's closing price
     * @return True Range
     */
    public double calculateTrueRange(double high, double low, double prevClose) {
        double range1 = high - low;
        double range2 = Math.abs(high - prevClose);
        double range3 = Math.abs(low - prevClose);

        return Math.max(range1, Math.max(range2, range3));
    }
}
