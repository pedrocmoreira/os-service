CREATE TABLE customers (
    id            BIGSERIAL PRIMARY KEY,
    document      VARCHAR(20)  NOT NULL UNIQUE,
    document_type VARCHAR(10)  NOT NULL CHECK (document_type IN ('CPF', 'CNPJ')),
    name          VARCHAR(150) NOT NULL,
    phone         VARCHAR(20),
    email         VARCHAR(150)
);

CREATE TABLE vehicles (
    id          BIGSERIAL PRIMARY KEY,
    plate       VARCHAR(10)  NOT NULL UNIQUE,
    make        VARCHAR(80)  NOT NULL,
    model       VARCHAR(100) NOT NULL,
    year        INTEGER      NOT NULL,
    color       VARCHAR(50),
    customer_id BIGINT       NOT NULL REFERENCES customers(id)
);
CREATE INDEX idx_vehicles_customer ON vehicles(customer_id);

CREATE TABLE users (
    id        BIGSERIAL PRIMARY KEY,
    username  VARCHAR(80)  NOT NULL UNIQUE,
    password  VARCHAR(255) NOT NULL,
    name      VARCHAR(150) NOT NULL,
    user_type VARCHAR(20)  NOT NULL CHECK (user_type IN ('ADMIN', 'ATENDENTE', 'MECANICO')),
    active    BOOLEAN      NOT NULL DEFAULT TRUE
);
