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
}
