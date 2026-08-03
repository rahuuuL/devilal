package com.terminal_devilal.business_tools.mannkendall.repository;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.terminal_devilal.business_tools.mannkendall.entity.MkResultHistoryEntity;

@Repository
public class MkResultHistoryCustomRepositoryImpl implements MkResultHistoryCustomRepository {

    private static final int UPSERT_CHUNK_SIZE = 1000;
    private static final String UPSERT_PREFIX_SQL = "INSERT INTO mk_result_history (" +
        "ticker, date, days, score, trend, h, p, z, tau, s, var_s, slope, intercept" +
        ") VALUES ";
    private static final String UPSERT_SUFFIX_SQL =
        " ON DUPLICATE KEY UPDATE " +
        "score = VALUES(score), " +
        "trend = VALUES(trend), " +
        "h = VALUES(h), " +
        "p = VALUES(p), " +
        "z = VALUES(z), " +
        "tau = VALUES(tau), " +
        "s = VALUES(s), " +
        "var_s = VALUES(var_s), " +
        "slope = VALUES(slope), " +
        "intercept = VALUES(intercept)";
    private static final String VALUE_TUPLE = "(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private final JdbcTemplate jdbcTemplate;

    public MkResultHistoryCustomRepositoryImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void upsertBatch(List<MkResultHistoryEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return;
        }

        for (int start = 0; start < entities.size(); start += UPSERT_CHUNK_SIZE) {
            int end = Math.min(start + UPSERT_CHUNK_SIZE, entities.size());
            upsertChunk(entities.subList(start, end));
        }
    }

    private void upsertChunk(List<MkResultHistoryEntity> chunk) {
        StringBuilder sql = new StringBuilder(UPSERT_PREFIX_SQL);
        List<Object> params = new ArrayList<>(chunk.size() * 13);

        for (int i = 0; i < chunk.size(); i++) {
            if (i > 0) {
                sql.append(", ");
            }
            sql.append(VALUE_TUPLE);

            MkResultHistoryEntity entity = chunk.get(i);
            params.add(entity.getTicker());
            params.add(Date.valueOf(entity.getDate()));
            params.add(entity.getDays());
            params.add(entity.getScore());
            params.add(entity.getTrend());
            params.add(entity.getH());
            params.add(entity.getP());
            params.add(entity.getZ());
            params.add(entity.getTau());
            params.add(entity.getS());
            params.add(entity.getVar_s());
            params.add(entity.getSlope());
            params.add(entity.getIntercept());
        }

        sql.append(UPSERT_SUFFIX_SQL);
        jdbcTemplate.update(sql.toString(), params.toArray());
    }
}
