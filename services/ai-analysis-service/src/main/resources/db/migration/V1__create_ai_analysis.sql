CREATE TABLE IF NOT EXISTS ai_analysis (
    id                 VARCHAR(100) PRIMARY KEY,
    incident_id        VARCHAR(50)  NOT NULL UNIQUE,
    summary            TEXT,
    probable_cause     TEXT,
    remediation        TEXT,
    model              VARCHAR(100),
    analysis_timestamp BIGINT,
    success            BOOLEAN,
    raw_response       TEXT
);
