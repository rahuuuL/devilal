ALTER TABLE pvpp_result_history
    ADD COLUMN body_weight DOUBLE;

TRUNCATE TABLE pvpp_result_history;

TRUNCATE TABLE pvpp_generation_history;
