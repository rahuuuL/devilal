package com.terminal_devilal.decision.indicator;

import org.springframework.stereotype.Component;
import com.terminal_devilal.business_tools.mannkendall.entity.MkResultHistoryEntity;
import com.terminal_devilal.business_tools.mannkendall.service.MannKendallHistoryService;

@Component
public class MannKendallScoreProvider extends AbstractMannKendallProvider {
    public MannKendallScoreProvider(MannKendallHistoryService historyService) { super(historyService); }
    public String getIndicatorCode() { return "MK_SCORE"; }
    protected Object value(MkResultHistoryEntity record) { return record.getScore(); }
}