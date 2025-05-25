package com.terminal_devilal.controllers.DataGathering.Service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.terminal_devilal.controllers.DataGathering.DAO.DataFetchHistroyDAO;
import com.terminal_devilal.controllers.DataGathering.Model.DataFetchHistroy;

@Service
public class ProcessedDatesService {

	@Autowired
	private final DataFetchHistroyDAO repository;

	public ProcessedDatesService(DataFetchHistroyDAO repository) {
		this.repository = repository;
	}
	
	public List<DataFetchHistroy> getProcessedDatesForTickers() {
		return repository.findAll();
	}

}
