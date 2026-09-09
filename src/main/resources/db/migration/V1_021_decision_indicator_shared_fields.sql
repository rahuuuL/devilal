ALTER TABLE decision_indicator
    ADD COLUMN source_provider_code VARCHAR(100) NULL AFTER source_reference;

ALTER TABLE decision_indicator
    ADD COLUMN row_filter_expression VARCHAR(200) NULL AFTER source_provider_code;

ALTER TABLE decision_indicator
    ADD COLUMN field_expression VARCHAR(200) NULL AFTER row_filter_expression;

ALTER TABLE decision_indicator
    ADD COLUMN row_aggregation VARCHAR(20) NULL AFTER field_expression;

UPDATE decision_indicator
SET
    source_provider_code = 'CONSISTENT_VOLUME_SOURCE',
    row_filter_expression = 'date == #asOfDate',
    field_expression = 'consistencyScore',
    row_aggregation = 'MAX'
WHERE code = 'CONSISTENT_VOLUME_SCORE';

UPDATE decision_indicator
SET
    source_provider_code = 'CONSISTENT_VOLUME_SOURCE',
    row_filter_expression = 'date == #asOfDate',
    field_expression = 'relativeVolumesCombinedAverage',
    row_aggregation = 'MAX'
WHERE code = 'RELATIVE_VOLUME_AVG';

UPDATE decision_indicator
SET
    source_provider_code = 'CONSISTENT_VOLUME_SOURCE',
    row_filter_expression = 'date == #asOfDate',
    field_expression = 'consistencyWindow',
    row_aggregation = 'FIRST'
WHERE code = 'CONSISTENT_VOLUME_WINDOW';

UPDATE decision_indicator
SET
    source_provider_code = 'MK_SOURCE',
    row_filter_expression = 'date == #asOfDate',
    field_expression = 'score',
    row_aggregation = 'MAX'
WHERE code = 'MK_SCORE';

UPDATE decision_indicator
SET
    source_provider_code = 'MK_SOURCE',
    row_filter_expression = 'date == #asOfDate',
    field_expression = 'tau',
    row_aggregation = 'MAX'
WHERE code = 'MK_TAU';

UPDATE decision_indicator
SET
    source_provider_code = 'MK_SOURCE',
    row_filter_expression = 'date == #asOfDate',
    field_expression = 'slope',
    row_aggregation = 'MAX'
WHERE code = 'MK_SLOPE';

UPDATE decision_indicator
SET
    source_provider_code = 'MK_SOURCE',
    row_filter_expression = 'date == #asOfDate',
    field_expression = 'trend',
    row_aggregation = 'FIRST'
WHERE code = 'MK_TREND';

INSERT INTO decision_indicator (
    code,
    name,
    category,
    subject_type,
    value_type,
    unit,
    source_type,
    source_reference,
    source_provider_code,
    row_filter_expression,
    field_expression,
    row_aggregation,
    enabled,
    description,
    created_at,
    updated_at
) VALUES
    ('RELATIVE_VOLUME_AVG', 'Relative Volume Avg', 'VOLUME', 'TICKER', 'NUMBER', 'RATIO', 'PROVIDER', 'CONSISTENT_VOLUME_SOURCE', 'CONSISTENT_VOLUME_SOURCE', 'date == #asOfDate', 'relativeVolumesCombinedAverage', 'MAX', TRUE, 'Average relative volume for the consistent-volume source', NOW(), NOW()),
    ('CONSISTENT_VOLUME_WINDOW', 'Consistent Volume Window', 'VOLUME', 'TICKER', 'NUMBER', 'COUNT', 'PROVIDER', 'CONSISTENT_VOLUME_SOURCE', 'CONSISTENT_VOLUME_SOURCE', 'date == #asOfDate', 'consistencyWindow', 'FIRST', TRUE, 'Consistency window from the volume signal source', NOW(), NOW())
ON DUPLICATE KEY UPDATE
    source_provider_code = VALUES(source_provider_code),
    row_filter_expression = VALUES(row_filter_expression),
    field_expression = VALUES(field_expression),
    row_aggregation = VALUES(row_aggregation),
    enabled = VALUES(enabled),
    updated_at = NOW();
