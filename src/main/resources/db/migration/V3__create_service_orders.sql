CREATE SEQUENCE service_order_number_seq START 1;

CREATE TABLE service_orders (
    id                   BIGSERIAL PRIMARY KEY,
    number               VARCHAR(20)    NOT NULL UNIQUE,
    status               VARCHAR(30)    NOT NULL DEFAULT 'RECEBIDA',
    total_value          NUMERIC(12, 2) NOT NULL DEFAULT 0,
    observations         TEXT,
    diagnosis_notes      TEXT,
    payment_url          VARCHAR(500),
    cancellation_reason  VARCHAR(500),
    created_at           TIMESTAMP      NOT NULL,
    diagnosis_start_date TIMESTAMP,
    execution_start_date TIMESTAMP,
    completion_date      TIMESTAMP,
    delivery_date        TIMESTAMP,
    customer_id          BIGINT         NOT NULL REFERENCES customers(id),
    vehicle_id           BIGINT         NOT NULL REFERENCES vehicles(id)
);
CREATE INDEX idx_so_status ON service_orders(status);
CREATE INDEX idx_so_customer ON service_orders(customer_id);

CREATE TABLE service_items (
    id               BIGSERIAL PRIMARY KEY,
    service_order_id BIGINT         NOT NULL REFERENCES service_orders(id) ON DELETE CASCADE,
    service_id       BIGINT         NOT NULL REFERENCES services(id),
    applied_price    NUMERIC(10, 2) NOT NULL
);

CREATE TABLE part_items (
    id                 BIGSERIAL PRIMARY KEY,
    service_order_id   BIGINT         NOT NULL REFERENCES service_orders(id) ON DELETE CASCADE,
    part_id            BIGINT         NOT NULL REFERENCES parts(id),
    quantity           INTEGER        NOT NULL,
    unit_price_applied NUMERIC(10, 2) NOT NULL
);

CREATE TABLE service_order_history (
    id               BIGSERIAL PRIMARY KEY,
    service_order_id BIGINT      NOT NULL REFERENCES service_orders(id) ON DELETE CASCADE,
    from_status      VARCHAR(30),
    to_status        VARCHAR(30) NOT NULL,
    note             VARCHAR(500),
    changed_at       TIMESTAMP   NOT NULL
);
CREATE INDEX idx_history_so ON service_order_history(service_order_id);
