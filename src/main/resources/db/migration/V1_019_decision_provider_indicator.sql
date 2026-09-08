INSERT INTO decision_indicator
    (code, name, category, subject_type, value_type, unit, source_type, source_reference, enabled, description, created_at, updated_at)
VALUES
    ('CONSISTENT_VOLUME_SCORE', 'Consistent Volume Score', 'VOLUME', 'TICKER', 'NUMBER', 'COUNT', 'PROVIDER', 'CONSISTENT_VOLUME_SCORE_PROVIDER', TRUE, 'Indicator backed by consistent volume detector', NOW(), NOW())
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    category = VALUES(category),
    subject_type = VALUES(subject_type),
    value_type = VALUES(value_type),
    unit = VALUES(unit),
    source_type = VALUES(source_type),
    source_reference = VALUES(source_reference),
    enabled = VALUES(enabled),
    description = VALUES(description),
    updated_at = NOW();
