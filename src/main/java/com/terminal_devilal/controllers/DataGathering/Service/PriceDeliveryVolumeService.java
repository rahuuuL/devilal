package com.terminal_devilal.controllers.DataGathering.Service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.terminal_devilal.controllers.DataGathering.DAO.PriceDeliveryVolumeDAO;
import com.terminal_devilal.controllers.DataGathering.Model.PriceDeliveryVolume;

@Service
public class PriceDeliveryVolumeService {

	
	private final PriceDeliveryVolumeDAO repository;

    public PriceDeliveryVolumeService(PriceDeliveryVolumeDAO repository) {
        this.repository = repository;
    }

    @Transactional
    public void savePdv(PriceDeliveryVolume data) {
        repository.save(data);
    }

    @Transactional
    public void saveAllPdvList(List<PriceDeliveryVolume> dataList) {
        repository.saveAll(dataList);
    }
    
    public List<PriceDeliveryVolume> getAllPdvWithinDate(String Ticker, LocalDate FromDate, LocalDate ToDate) {
        return repository.findByTickerAndDateBetween(Ticker, ToDate, ToDate);
    }
}