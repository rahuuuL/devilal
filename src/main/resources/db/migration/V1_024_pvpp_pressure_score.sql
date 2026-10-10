ALTER TABLE pvpp_result_history
    ADD COLUMN pressure DOUBLE,
    ADD COLUMN score DOUBLE;

UPDATE pvpp_result_history
SET pressure = CASE
        WHEN centered_clv IS NOT NULL AND rvol IS NOT NULL THEN centered_clv * rvol
        ELSE NULL
    END;

UPDATE pvpp_result_history
SET score = CASE
        WHEN efficiency IS NOT NULL AND pressure IS NOT NULL THEN efficiency * pressure
        ELSE NULL
    END;
