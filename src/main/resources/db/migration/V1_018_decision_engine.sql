CREATE TABLE decision_profile (
    id BIGINT NOT NULL AUTO_INCREMENT,
    public_id BINARY(16) NOT NULL,
    owner_id BIGINT NOT NULL,
    code VARCHAR(100) NOT NULL,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    evaluation_mode VARCHAR(30) NOT NULL DEFAULT 'ACCUMULATE',
    subject_type VARCHAR(50) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_by VARCHAR(100),
    created_at DATETIME NOT NULL,
    updated_by VARCHAR(100),
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_decision_profile_public_id (public_id),
    UNIQUE KEY uk_decision_profile_owner_code (owner_id, code),
    KEY idx_decision_profile_owner (owner_id)
) ENGINE=InnoDB;

CREATE TABLE decision_indicator (
    code VARCHAR(100) NOT NULL,
    name VARCHAR(200) NOT NULL,
    category VARCHAR(100) NOT NULL,
    subject_type VARCHAR(50) NOT NULL,
    value_type VARCHAR(30) NOT NULL,
    unit VARCHAR(50),
    source_type VARCHAR(30) NOT NULL,
    source_reference VARCHAR(200),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    min_value DECIMAL(20,8),
    max_value DECIMAL(20,8),
    description TEXT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (code),
    KEY idx_decision_indicator_enabled (enabled),
    KEY idx_decision_indicator_subject_type (subject_type)
) ENGINE=InnoDB;

CREATE TABLE decision_rule (
    id BIGINT NOT NULL AUTO_INCREMENT,
    public_id BINARY(16) NOT NULL,
    owner_id BIGINT NOT NULL,
    profile_id BIGINT NOT NULL,
    code VARCHAR(100) NOT NULL,
    name VARCHAR(200) NOT NULL,
    rule_type VARCHAR(50) NOT NULL,
    priority INT NOT NULL DEFAULT 0,
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    current_version INT NOT NULL DEFAULT 1,
    definition_json JSON NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_decision_rule_public_id (public_id),
    UNIQUE KEY uk_decision_rule_owner_code (owner_id, code),
    KEY idx_decision_rule_profile (profile_id),
    CONSTRAINT fk_decision_rule_profile FOREIGN KEY (profile_id) REFERENCES decision_profile(id)
) ENGINE=InnoDB;

CREATE TABLE decision_output_variable (
    id BIGINT NOT NULL AUTO_INCREMENT,
    profile_id BIGINT NOT NULL,
    code VARCHAR(100) NOT NULL,
    name VARCHAR(200) NOT NULL,
    value_type VARCHAR(30) NOT NULL,
    initial_value VARCHAR(200),
    min_value DECIMAL(20,8),
    max_value DECIMAL(20,8),
    created_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_output_variable_profile_code (profile_id, code),
    CONSTRAINT fk_output_variable_profile FOREIGN KEY (profile_id) REFERENCES decision_profile(id)
) ENGINE=InnoDB;

CREATE TABLE decision_rule_version (
    id BIGINT NOT NULL AUTO_INCREMENT,
    rule_id BIGINT NOT NULL,
    version INT NOT NULL,
    definition_json JSON NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_decision_rule_version (rule_id, version),
    CONSTRAINT fk_decision_rule_version_rule FOREIGN KEY (rule_id) REFERENCES decision_rule(id)
) ENGINE=InnoDB;

CREATE TABLE decision_rule_condition (
    id BIGINT NOT NULL AUTO_INCREMENT,
    rule_version_id BIGINT NOT NULL,
    sequence_no INT NOT NULL,
    indicator_code VARCHAR(100) NOT NULL,
    operator VARCHAR(30) NOT NULL,
    comparison_type VARCHAR(30) NOT NULL,
    comparison_value_json JSON,
    comparison_indicator_code VARCHAR(100),
    logical_operator VARCHAR(10),
    created_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_rule_condition_version_seq (rule_version_id, sequence_no),
    KEY idx_rule_condition_indicator (indicator_code),
    CONSTRAINT fk_rule_condition_version FOREIGN KEY (rule_version_id) REFERENCES decision_rule_version(id),
    CONSTRAINT fk_rule_condition_indicator FOREIGN KEY (indicator_code) REFERENCES decision_indicator(code)
) ENGINE=InnoDB;

CREATE TABLE decision_rule_action (
    id BIGINT NOT NULL AUTO_INCREMENT,
    rule_version_id BIGINT NOT NULL,
    sequence_no INT NOT NULL,
    target VARCHAR(100) NOT NULL,
    action_type VARCHAR(50) NOT NULL,
    constant_value DECIMAL(30,12),
    expression_json JSON,
    created_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_rule_action_version_seq (rule_version_id, sequence_no),
    CONSTRAINT fk_rule_action_version FOREIGN KEY (rule_version_id) REFERENCES decision_rule_version(id)
) ENGINE=InnoDB;

INSERT INTO decision_indicator
    (code, name, category, subject_type, value_type, unit, source_type, source_reference, enabled, description, created_at, updated_at)
VALUES
    ('RSI', 'RSI', 'MOMENTUM', 'TICKER', 'NUMBER', 'VALUE', 'SERVICE', 'RSI', TRUE, 'Relative strength index', NOW(), NOW()),
    ('RSI_PERCENTILE', 'RSI Percentile', 'MOMENTUM', 'TICKER', 'NUMBER', 'PERCENTILE', 'SERVICE', 'RSI_PERCENTILE', TRUE, 'RSI percentile value', NOW(), NOW()),
    ('ATR', 'Average True Range', 'VOLATILITY', 'TICKER', 'NUMBER', 'VALUE', 'DB', 'ATR', TRUE, 'Average true range', NOW(), NOW()),
    ('ATR_PERCENTILE', 'ATR Percentile', 'VOLATILITY', 'TICKER', 'NUMBER', 'PERCENTILE', 'CALCULATED', 'ATR_PERCENTILE', TRUE, 'ATR percentile value', NOW(), NOW()),
    ('PVPP', 'PVPP Score', 'PRICE_VOLUME', 'TICKER', 'NUMBER', 'SCORE', 'DB', 'PVPP', TRUE, 'Price volume pressure profile score', NOW(), NOW()),
    ('RVOL', 'Relative Volume', 'VOLUME', 'TICKER', 'NUMBER', 'RATIO', 'DB', 'RVOL', TRUE, 'Relative volume ratio', NOW(), NOW()),
    ('MK_TAU', 'Mann-Kendall Tau', 'TREND', 'TICKER', 'NUMBER', 'SCORE', 'DB', 'MK_TAU', TRUE, 'Mann-Kendall tau', NOW(), NOW()),
    ('MK_SLOPE', 'Mann-Kendall Slope', 'TREND', 'TICKER', 'NUMBER', 'SCORE', 'DB', 'MK_SLOPE', TRUE, 'Mann-Kendall slope', NOW(), NOW()),
    ('VCP', 'VCP Signal', 'PATTERN', 'TICKER', 'BOOLEAN', 'BOOLEAN', 'SERVICE', 'VCP', TRUE, 'Volatility contraction pattern signal', NOW(), NOW()),
    ('SORTINO', 'Sortino Ratio', 'RISK', 'TICKER', 'NUMBER', 'RATIO', 'DB', 'SORTINO', TRUE, 'Sortino ratio', NOW(), NOW()),
    ('DELIVERY_PERCENT', 'Delivery Percent', 'VOLUME', 'TICKER', 'NUMBER', 'PERCENT', 'DB', 'DELIVERY_PERCENT', TRUE, 'Delivery percentage', NOW(), NOW())
ON DUPLICATE KEY UPDATE name = VALUES(name), enabled = VALUES(enabled), updated_at = NOW();
