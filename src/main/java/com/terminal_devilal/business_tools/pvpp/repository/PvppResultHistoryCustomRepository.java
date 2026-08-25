package com.terminal_devilal.business_tools.pvpp.repository;

import java.util.List;

import com.terminal_devilal.business_tools.pvpp.entity.PvppResultHistoryEntity;

public interface PvppResultHistoryCustomRepository {

    void upsertHistoryBatch(List<PvppResultHistoryEntity> entities);
}
