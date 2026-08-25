package com.terminal_devilal.business_tools.pvpp.repository;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.terminal_devilal.business_tools.pvpp.entity.PvppDailyEntity;
import com.terminal_devilal.business_tools.pvpp.entity.PvppResultHistoryEntity;

import io.micrometer.core.annotation.Timed;

@Repository
public class PvppResultHistoryCustomRepositoryImpl implements PvppResultHistoryCustomRepository {

    private static final String DAILY_UPSERT_PREFIX_SQL = "INSERT INTO pvpp_daily ("
            + "ticker, date, return_pct, volume, clv, centered_clv, return_z, clv_z"
            + ") VALUES ";
    private static final String DAILY_UPSERT_SUFFIX_SQL =
            " ON DUPLICATE KEY UPDATE "
                    + "return_pct = VALUES(return_pct), "
                    + "volume = VALUES(volume), "
                    + "clv = VALUES(clv), "
                    + "centered_clv = VALUES(centered_clv), "
                    + "return_z = VALUES(return_z), "
                    + "clv_z = VALUES(clv_z)";
    private static final String DAILY_VALUE_TUPLE = "(?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String HISTORY_UPSERT_PREFIX_SQL = "INSERT INTO pvpp_result_history ("
            + "ticker, date, days, rvol, efficiency, log_rvol_z, pressure_score"
            + ") VALUES ";
    private static final String HISTORY_UPSERT_SUFFIX_SQL =
            " ON DUPLICATE KEY UPDATE "
                    + "rvol = VALUES(rvol), "
                    + "efficiency = VALUES(efficiency), "
                    + "log_rvol_z = VALUES(log_rvol_z), "
                    + "pressure_score = VALUES(pressure_score)";
    private static final String HISTORY_VALUE_TUPLE = "(?, ?, ?, ?, ?, ?, ?)";

    private final JdbcTemplate jdbcTemplate;
    private final int upsertChunkSize;

    public PvppResultHistoryCustomRepositoryImpl(JdbcTemplate jdbcTemplate,
            @Value("${pvpp.history.upsert.chunk-size:1000}") int upsertChunkSize) {
        this.jdbcTemplate = jdbcTemplate;
        this.upsertChunkSize = Math.max(1, upsertChunkSize);
    }

    @Override
    @Timed(value = "pvpp.daily.upsert", description = "PVPP daily upsert")
    public void upsertDailyBatch(List<PvppDailyEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return;
        }

        for (int start = 0; start < entities.size(); start += upsertChunkSize) {
            int end = Math.min(start + upsertChunkSize, entities.size());
            upsertDailyChunk(entities.subList(start, end));
        }
    }

    @Override
    @Timed(value = "pvpp.history.upsert", description = "PVPP history upsert")
    public void upsertHistoryBatch(List<PvppResultHistoryEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return;
        }

        for (int start = 0; start < entities.size(); start += upsertChunkSize) {
            int end = Math.min(start + upsertChunkSize, entities.size());
            upsertHistoryChunk(entities.subList(start, end));
        }
    }

    private void upsertDailyChunk(List<PvppDailyEntity> chunk) {
        StringBuilder sql = new StringBuilder(DAILY_UPSERT_PREFIX_SQL);
        List<Object> params = new ArrayList<>(chunk.size() * 8);

        for (int i = 0; i < chunk.size(); i++) {
            if (i > 0) {
                sql.append(", ");
            }
            sql.append(DAILY_VALUE_TUPLE);

            PvppDailyEntity entity = chunk.get(i);
            params.add(entity.getTicker());
            params.add(Date.valueOf(entity.getDate()));
            params.add(entity.getReturnPct());
            params.add(entity.getVolume());
            params.add(entity.getClv());
            params.add(entity.getCenteredClv());
            params.add(entity.getReturnZ());
            params.add(entity.getClvZ());
        }

        sql.append(DAILY_UPSERT_SUFFIX_SQL);
        jdbcTemplate.update(sql.toString(), params.toArray());
    }

    private void upsertHistoryChunk(List<PvppResultHistoryEntity> chunk) {
        StringBuilder sql = new StringBuilder(HISTORY_UPSERT_PREFIX_SQL);
        List<Object> params = new ArrayList<>(chunk.size() * 7);

        for (int i = 0; i < chunk.size(); i++) {
            if (i > 0) {
                sql.append(", ");
            }
            sql.append(HISTORY_VALUE_TUPLE);

            PvppResultHistoryEntity entity = chunk.get(i);
            params.add(entity.getTicker());
            params.add(Date.valueOf(entity.getDate()));
            params.add(entity.getDays());
            params.add(entity.getRvol());
            params.add(entity.getEfficiency());
            params.add(entity.getLogRvolZ());
            params.add(entity.getPressureScore());
        }

        sql.append(HISTORY_UPSERT_SUFFIX_SQL);
        jdbcTemplate.update(sql.toString(), params.toArray());
    }
}
