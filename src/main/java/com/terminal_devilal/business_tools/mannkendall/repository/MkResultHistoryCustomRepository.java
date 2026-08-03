package com.terminal_devilal.business_tools.mannkendall.repository;

import java.util.List;

import com.terminal_devilal.business_tools.mannkendall.entity.MkResultHistoryEntity;

public interface MkResultHistoryCustomRepository {

    void upsertBatch(List<MkResultHistoryEntity> entities);
}
