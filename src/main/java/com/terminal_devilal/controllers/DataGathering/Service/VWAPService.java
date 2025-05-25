package com.terminal_devilal.controllers.DataGathering.Service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.terminal_devilal.controllers.DataGathering.DAO.VWAPDAO;
import com.terminal_devilal.controllers.DataGathering.Model.VWAP;

import jakarta.transaction.Transactional;

@Service
public class VWAPService {

	private VWAPDAO vwapdao;

	public VWAPService(VWAPDAO vwapdao) {
		super();
		this.vwapdao = vwapdao;
	}

	@Transactional
	public void saveAllVWAP(List<VWAP> vwaps) {
		this.vwapdao.saveAll(vwaps);
	}

}
