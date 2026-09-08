package com.terminal_devilal.decision.indicator;

import org.springframework.stereotype.Component;
import com.terminal_devilal.business_tools.mannkendall.entity.MkResultHistoryEntity;
import com.terminal_devilal.business_tools.mannkendall.service.MannKendallHistoryService;

@Component
public class MannKendallTauProvider extends AbstractMannKendallProvider {
    public MannKendallTauProvider(MannKendallHistoryService historyService) { super(historyService); }
    public String getIndicatorCode() { return "MK_TAU"; }
    protected Object value(MkResultHistoryEntity record) { return record.getTau(); }
}