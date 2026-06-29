-- =============================================================================
-- OmiinQA embedded H2 bootstrap (PostgreSQL-compat mode).
-- Mirrors src/test/resources/db/schema.sql but expressed in H2-native DDL so
-- the database layer can run with zero external infrastructure. Idempotent:
-- drops then recreates, then seeds deterministic reference data.
-- =============================================================================

DROP TABLE IF EXISTS orders;
DROP TABLE IF EXISTS audit_log;
DROP TABLE IF EXISTS products;
DROP TABLE IF EXISTS users;

CREATE TABLE users (
    id      BIGSERIAL PRIMARY KEY,
    name    VARCHAR(255),
    email   VARCHAR(255) UNIQUE NOT NULL,
    status  VARCHAR(50)  NOT NULL DEFAULT 'active',
    created TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE products (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(255)    NOT NULL,
    category    VARCHAR(100),
    price       NUMERIC(10, 2),
    stock_qty   INT             NOT NULL DEFAULT 0,
    active      BOOLEAN         NOT NULL DEFAULT TRUE
);

CREATE TABLE orders (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT          NOT NULL REFERENCES users(id),
    product_id  BIGINT          NOT NULL REFERENCES products(id),
    quantity    INT             NOT NULL,
    total       NUMERIC(12, 2)  NOT NULL,
    status      VARCHAR(50)     NOT NULL DEFAULT 'pending',
    created_at  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_orders_quantity_positive CHECK (quantity > 0),
    CONSTRAINT chk_orders_total_non_negative CHECK (total >= 0)
);

CREATE TABLE audit_log (
    id      BIGSERIAL PRIMARY KEY,
    entity  VARCHAR(100)    NOT NULL,
    action  VARCHAR(50)     NOT NULL,
    actor   VARCHAR(255)    NOT NULL,
    at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ----------------------------------------------------------------- seed data
INSERT INTO users (name, email, status) VALUES
    ('Alice Active',   'alice@omiinqa.test',    'active'),
    ('Bob Active',     'bob@omiinqa.test',      'active'),
    ('Carol Inactive', 'carol@omiinqa.test',    'inactive');

INSERT INTO products (name, category, price, stock_qty, active) VALUES
    ('Widget A',     'electronics',  9.99,  100, TRUE),
    ('Widget B',     'electronics', 19.99,   50, TRUE),
    ('Gadget X',     'tools',        4.99,  200, TRUE),
    ('Legacy Item',  'discontinued', 0.01,    0, FALSE);

INSERT INTO orders (user_id, product_id, quantity, total, status) VALUES
    (1, 1, 2, 19.98, 'paid'),
    (1, 2, 1, 19.99, 'pending'),
    (2, 3, 5, 24.95, 'paid');

INSERT INTO audit_log (entity, action, actor) VALUES
    ('user',    'CREATE', 'system'),
    ('order',   'CREATE', 'alice@omiinqa.test'),
    ('product', 'UPDATE', 'system');
