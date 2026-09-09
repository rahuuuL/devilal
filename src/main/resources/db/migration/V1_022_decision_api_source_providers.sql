UPDATE decision_indicator
SET
    source_provider_code = 'PVPP_SOURCE',
    row_filter_expression = 'date == #asOfDate',
    field_expression = 'efficiency',
    row_aggregation = 'MAX'
WHERE code = 'PVPP';

UPDATE decision_indicator
SET
    source_provider_code = 'PDV_SOURCE',
    row_filter_expression = 'date == #asOfDate',
    field_expression = 'deliveryPercentage',
    row_aggregation = 'MAX'
WHERE code = 'DELIVERY_PERCENT';

UPDATE decision_indicator
SET
    source_provider_code = 'SHARPE_RATIO_SOURCE',
    row_filter_expression = 'date == #asOfDate',
    field_expression = 'sharpeRatio',
    row_aggregation = 'MAX'
WHERE code = 'SHARPE_RATIO';

UPDATE decision_indicator
SET
    source_provider_code = 'SHARPE_RATIO_SOURCE',
    row_filter_expression = 'date == #asOfDate',
    field_expression = 'sortinoRatio',
    row_aggregation = 'MAX'
WHERE code = 'SORTINO';

UPDATE decision_indicator
SET
    source_provider_code = 'VWAP_SOURCE',
    row_filter_expression = 'date == #asOfDate',
    field_expression = 'vwap',
    row_aggregation = 'MAX'
WHERE code = 'VWAP';

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
    ('VWAP', 'VWAP', 'PRICE_VOLUME', 'TICKER', 'NUMBER', 'PRICE', 'PROVIDER', 'VWAP_SOURCE', 'VWAP_SOURCE', 'date == #asOfDate', 'vwap', 'MAX', TRUE, 'Volume weighted average price from VWAP source provider', NOW(), NOW()),
    ('SHARPE_RATIO', 'Sharpe Ratio', 'RISK', 'TICKER', 'NUMBER', 'RATIO', 'PROVIDER', 'SHARPE_RATIO_SOURCE', 'SHARPE_RATIO_SOURCE', 'date == #asOfDate', 'sharpeRatio', 'MAX', TRUE, 'Rolling Sharpe ratio from ratio source provider', NOW(), NOW())
ON DUPLICATE KEY UPDATE
    source_provider_code = VALUES(source_provider_code),
    row_filter_expression = VALUES(row_filter_expression),
    field_expression = VALUES(field_expression),
    row_aggregation = VALUES(row_aggregation),
    enabled = VALUES(enabled),
    updated_at = NOW();