-- Example domain: table for the Example entity (template for new domains).
CREATE TABLE examples (
    id          UUID PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    description VARCHAR(1024),
    version     BIGINT       NOT NULL DEFAULT 0,
    created_at  TIMESTAMP    NOT NULL,
    updated_at  TIMESTAMP    NOT NULL
);

CREATE UNIQUE INDEX idx_examples_name ON examples (name);
