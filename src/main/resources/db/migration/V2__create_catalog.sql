CREATE TABLE services (
    id                     BIGSERIAL PRIMARY KEY,
    name                   VARCHAR(150)   NOT NULL,
    description            TEXT,
    base_price             NUMERIC(10, 2) NOT NULL,
    estimated_time_minutes INTEGER,
    active                 BOOLEAN        NOT NULL DEFAULT TRUE
);

-- Estoque não fica aqui: pertence ao Execution Service (MongoDB)
CREATE TABLE parts (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(150)   NOT NULL,
    description TEXT,
    code        VARCHAR(50),
    unit_price  NUMERIC(10, 2) NOT NULL,
    active      BOOLEAN        NOT NULL DEFAULT TRUE
);

-- Catálogo inicial (os mesmos IDs de peças são usados no estoque do Execution Service)
INSERT INTO services (name, description, base_price, estimated_time_minutes) VALUES
    ('Troca de óleo', 'Troca de óleo e filtro', 150.00, 40),
    ('Alinhamento e balanceamento', 'Alinhamento 3D e balanceamento das 4 rodas', 120.00, 60),
    ('Revisão de freios', 'Inspeção e troca de pastilhas', 300.00, 90);

INSERT INTO parts (name, description, code, unit_price) VALUES
    ('Filtro de óleo', 'Filtro de óleo universal', 'FO-001', 45.00),
    ('Óleo 5W30 (litro)', 'Óleo sintético 5W30', 'OL-5W30', 38.00),
    ('Pastilha de freio dianteira', 'Jogo de pastilhas dianteiras', 'PF-100', 180.00);
