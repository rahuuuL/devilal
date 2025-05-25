package com.terminal_devilal.controllers.DataGathering.Service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.terminal_devilal.controllers.DataGathering.DAO.RSIDAO;
import com.terminal_devilal.controllers.DataGathering.Model.RSI;

import jakarta.transaction.Transactional;

@Service
public class RSIService {

	private RSIDAO rsidao;

	public RSIService(RSIDAO rsidao) {
		super();
		this.rsidao = rsidao;
	}
	
	@Transactional
	public void saveAllRSI(List<RSI> rsis) {
		this.rsidao.saveAll(rsis);
	}
}
