package com.terminal_devilal.core_processes.dfht.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.terminal_devilal.core_processes.dfht.entities.DataFetchEntity;
import com.terminal_devilal.core_processes.dfht.repository.DataFetchHistroyRepository;

@Service
public class DataFetchHistoryService {

	@Autowired
	private final DataFetchHistroyRepository repository;

	public DataFetchHistoryService(DataFetchHistroyRepository repository) {
		this.repository = repository;
	}

	public List<DataFetchEntity> getProcessedDatesForTickers() {
		return repository.findAll();
	}

	public void updateLastDateForPdvt(String ticker, LocalDate pdvtLastProcessedDate) {
		Optional<DataFetchEntity> dataFetchEntity = this.repository.findById(ticker);
		if (dataFetchEntity.isPresent()) {
			DataFetchEntity updateData = dataFetchEntity.get();
			updateData.setPdvtLastDate(pdvtLastProcessedDate);
			repository.save(updateData);
		}
	}
	
	public List<String> getAllTickers(){
		return repository.findAllTickers();
	}

}
