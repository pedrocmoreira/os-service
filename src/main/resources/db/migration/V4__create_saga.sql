CREATE TABLE saga_instances (
    id                VARCHAR(36)  PRIMARY KEY,
    service_order_id  BIGINT       NOT NULL UNIQUE REFERENCES service_orders(id),
    status            VARCHAR(20)  NOT NULL,
    current_step      VARCHAR(40)  NOT NULL,
    budget_created    BOOLEAN      NOT NULL DEFAULT FALSE,
    payment_confirmed BOOLEAN      NOT NULL DEFAULT FALSE,
    parts_reserved    BOOLEAN      NOT NULL DEFAULT FALSE,
    failure_reason    VARCHAR(500),
    started_at        TIMESTAMP    NOT NULL,
    updated_at        TIMESTAMP
);

CREATE TABLE processed_messages (
    message_id   VARCHAR(64) PRIMARY KEY,
    processed_at TIMESTAMP   NOT NULL
);
