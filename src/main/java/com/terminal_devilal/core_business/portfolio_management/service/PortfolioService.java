package com.terminal_devilal.core_business.portfolio_management.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.terminal_devilal.core_business.portfolio_management.repository.InvestmentEntryDAO;
import com.terminal_devilal.core_business.portfolio_management.repository.PortfolioDAO;
import com.terminal_devilal.core_business.portfolio_management.dto.InvestmentEntryDTO;
import com.terminal_devilal.core_business.portfolio_management.dto.PortfolioDTO;
import com.terminal_devilal.core_business.portfolio_management.entity.InvestmentEntry;
import com.terminal_devilal.core_business.portfolio_management.entity.Portfolio;

@Service
public class PortfolioService {

	private final PortfolioDAO portfolioDAO;
	private final InvestmentEntryDAO entryDAO;

	public PortfolioService(PortfolioDAO portfolioDAO, InvestmentEntryDAO entryDAO) {
		this.portfolioDAO = portfolioDAO;
		this.entryDAO = entryDAO;
	}

	public Portfolio createPortfolio(PortfolioDTO dto) {
		Portfolio p = new Portfolio();
		p.setName(dto.name());
		p.setDescription(dto.description());

		List<InvestmentEntry> entries = dto.investments().stream().map(e -> toEntity(e, p)).toList();

		p.setInvestments(entries);
		return portfolioDAO.save(p);
	}

	public Portfolio updatePortfolio(Long id, PortfolioDTO dto) {
		Portfolio p = portfolioDAO.findById(id).orElseThrow();
		p.setName(dto.name());
		p.setDescription(dto.description());
		return portfolioDAO.save(p);
	}

	public Portfolio updateInvestmentEntries(Long id, List<InvestmentEntryDTO> entries) {
		Portfolio p = portfolioDAO.findById(id).orElseThrow();

		p.getInvestments().clear(); // remove old ones (orphanRemoval = true)
		p.getInvestments().addAll(entries.stream().map(e -> toEntity(e, p)).toList());

		return portfolioDAO.save(p);
	}

	public void deleteInvestmentEntry(Long portfolioId, Long entryId) {
		InvestmentEntry entry = entryDAO.findById(entryId).orElseThrow();
		if (!entry.getPortfolio().getId().equals(portfolioId)) {
			throw new IllegalArgumentException("Entry doesn't belong to portfolio");
		}
		entryDAO.delete(entry);
	}

	public void deletePortfolio(Long id) {
		portfolioDAO.deleteById(id); // investments will be deleted via cascade
	}

	private InvestmentEntry toEntity(InvestmentEntryDTO dto, Portfolio p) {
		InvestmentEntry e = new InvestmentEntry();
		e.setTicker(dto.ticker());
		e.setPrice(dto.price());
		e.setQuantity(dto.quantity());
		e.setAddedDate(dto.addedDate());
		e.setRiskFreeRate(dto.riskFreeRate());
		e.setPortfolio(p);
		return e;
	}
}
