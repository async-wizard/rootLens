CREATE TABLE IF NOT EXISTS incidents (
    id                VARCHAR(50)  PRIMARY KEY,
    status            VARCHAR(30)  NOT NULL,
    severity          VARCHAR(20)  NOT NULL,
    services_impacted TEXT,
    trace_ids         TEXT,
    created_at        BIGINT       NOT NULL,
    updated_at        BIGINT       NOT NULL
);
