-- =============================================================================
-- OmiinQA Database Schema
-- Documents the expected table structure for all repositories in
-- com.omiinqa.database.repositories.*
--
-- Compatibility notes:
--   PostgreSQL: Use SERIAL or BIGSERIAL for auto-increment PKs. BOOLEAN is
--               a native type. NUMERIC(p,s) is exact.
--   MySQL:      Use BIGINT AUTO_INCREMENT for PKs. Use TINYINT(1) for
--               BOOLEAN (or BOOLEAN alias). NUMERIC(p,s) is exact.
--
-- Apply this schema to a local PostgreSQL/MySQL instance before running the
-- "database" TestNG group:
--   psql -U qa_user -d qa_db -f schema.sql        (PostgreSQL)
--   mysql -u qa_user -p qa_db < schema.sql        (MySQL)
--
-- The suite uses a single schema for both vendors; vendor-specific differences
-- are annotated inline.
-- =============================================================================

-- =====================================================================
-- TABLE: users
-- Managed by: com.omiinqa.database.repositories.UserRepository
-- =====================================================================
-- PostgreSQL
CREATE TABLE IF NOT EXISTS users (
    id      BIGSERIAL   PRIMARY KEY,          -- MySQL: BIGINT AUTO_INCREMENT PRIMARY KEY
    name    VARCHAR(255),                     -- nullable by design; use NULL for anonymous users
    email   VARCHAR(255) UNIQUE NOT NULL,
    status  VARCHAR(50)  NOT NULL DEFAULT 'active',
    created TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- MySQL equivalent (comment out the PostgreSQL DDL above and uncomment below):
-- CREATE TABLE IF NOT EXISTS users (
--     id      BIGINT       AUTO_INCREMENT PRIMARY KEY,
--     name    VARCHAR(255),
--     email   VARCHAR(255) UNIQUE NOT NULL,
--     status  VARCHAR(50)  NOT NULL DEFAULT 'active',
--     created TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
-- );

-- Index on email for fast lookups in UserRepository.findByEmail().
-- PostgreSQL creates a unique index automatically for UNIQUE columns;
-- MySQL does the same. Explicit index here is for documentation purposes.
-- CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);  -- PostgreSQL
-- CREATE INDEX idx_users_email ON users(email);                -- MySQL (no IF NOT EXISTS in older versions)


-- =====================================================================
-- TABLE: products
-- Managed by: com.omiinqa.database.repositories.ProductRepository
-- =====================================================================
-- PostgreSQL
CREATE TABLE IF NOT EXISTS products (
    id          BIGSERIAL       PRIMARY KEY,    -- MySQL: BIGINT AUTO_INCREMENT PRIMARY KEY
    name        VARCHAR(255)    NOT NULL,
    category    VARCHAR(100),
    price       NUMERIC(10, 2),                 -- exact monetary type; avoids IEEE-754 errors
    stock_qty   INT             NOT NULL DEFAULT 0,
    active      BOOLEAN         NOT NULL DEFAULT TRUE  -- MySQL: TINYINT(1) NOT NULL DEFAULT 1
);

-- MySQL equivalent:
-- CREATE TABLE IF NOT EXISTS products (
--     id        BIGINT          AUTO_INCREMENT PRIMARY KEY,
--     name      VARCHAR(255)    NOT NULL,
--     category  VARCHAR(100),
--     price     DECIMAL(10, 2),
--     stock_qty INT             NOT NULL DEFAULT 0,
--     active    TINYINT(1)      NOT NULL DEFAULT 1
-- );

-- Index for category lookups used in ProductRepository.findByCategory().
-- CREATE INDEX IF NOT EXISTS idx_products_category ON products(category);


-- =====================================================================
-- TABLE: orders
-- Managed by: com.omiinqa.database.repositories.OrderRepository
-- =====================================================================
-- The orders table has FK references to both users and products to
-- enforce referential integrity at the database level.
--
-- PostgreSQL
CREATE TABLE IF NOT EXISTS orders (
    id          BIGSERIAL       PRIMARY KEY,     -- MySQL: BIGINT AUTO_INCREMENT PRIMARY KEY
    user_id     BIGINT          NOT NULL REFERENCES users(id),
    product_id  BIGINT          NOT NULL REFERENCES products(id),
    quantity    INT             NOT NULL,
    total       NUMERIC(12, 2)  NOT NULL,         -- wider than price for multi-unit totals
    status      VARCHAR(50)     NOT NULL DEFAULT 'pending',
    created_at  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Integrity constraint: quantity must be positive.
    -- PostgreSQL and MySQL 8.0.16+ enforce CHECK constraints.
    CONSTRAINT chk_orders_quantity_positive CHECK (quantity > 0),
    -- Constraint: total must be non-negative.
    CONSTRAINT chk_orders_total_non_negative CHECK (total >= 0)
);

-- MySQL equivalent:
-- CREATE TABLE IF NOT EXISTS orders (
--     id          BIGINT          AUTO_INCREMENT PRIMARY KEY,
--     user_id     BIGINT          NOT NULL,
--     product_id  BIGINT          NOT NULL,
--     quantity    INT             NOT NULL,
--     total       DECIMAL(12, 2)  NOT NULL,
--     status      VARCHAR(50)     NOT NULL DEFAULT 'pending',
--     created_at  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
--     CONSTRAINT chk_orders_quantity_positive CHECK (quantity > 0),
--     CONSTRAINT chk_orders_total_non_negative CHECK (total >= 0),
--     FOREIGN KEY (user_id)    REFERENCES users(id),
--     FOREIGN KEY (product_id) REFERENCES products(id)
-- );

-- Indexes for the FK columns and status filter used in finders.
-- PostgreSQL does not auto-index FK columns; explicit indexes improve
-- performance on findByUserId / findByStatus queries.
-- CREATE INDEX IF NOT EXISTS idx_orders_user_id   ON orders(user_id);
-- CREATE INDEX IF NOT EXISTS idx_orders_product_id ON orders(product_id);
-- CREATE INDEX IF NOT EXISTS idx_orders_status     ON orders(status);


-- =====================================================================
-- TABLE: audit_log
-- Managed by: com.omiinqa.database.repositories.AuditRepository
-- =====================================================================
-- The audit_log table is append-only in production. In test environments,
-- rows are deleted by actor sentinel values for cleanup after each test.
--
-- Design note: no FK to users — actor is a free-text identity to support
-- system actors, service accounts, and deleted users (audit trail must
-- survive user deletion).
--
-- PostgreSQL
CREATE TABLE IF NOT EXISTS audit_log (
    id      BIGSERIAL       PRIMARY KEY,    -- MySQL: BIGINT AUTO_INCREMENT PRIMARY KEY
    entity  VARCHAR(100)    NOT NULL,       -- e.g. 'order', 'user', 'product'
    action  VARCHAR(50)     NOT NULL,       -- e.g. 'CREATE', 'UPDATE', 'DELETE', 'READ'
    actor   VARCHAR(255)    NOT NULL,       -- email, username, or system identifier
    at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- MySQL equivalent:
-- CREATE TABLE IF NOT EXISTS audit_log (
--     id      BIGINT          AUTO_INCREMENT PRIMARY KEY,
--     entity  VARCHAR(100)    NOT NULL,
--     action  VARCHAR(50)     NOT NULL,
--     actor   VARCHAR(255)    NOT NULL,
--     at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
-- );

-- Indexes for the most common audit query filters.
-- CREATE INDEX IF NOT EXISTS idx_audit_entity        ON audit_log(entity);
-- CREATE INDEX IF NOT EXISTS idx_audit_action        ON audit_log(action);
-- CREATE INDEX IF NOT EXISTS idx_audit_actor         ON audit_log(actor);
-- CREATE INDEX IF NOT EXISTS idx_audit_entity_action ON audit_log(entity, action);
-- CREATE INDEX IF NOT EXISTS idx_audit_at            ON audit_log(at);


-- =============================================================================
-- Optional: seed minimal reference data for smoke tests.
-- Uncomment and adjust for your local environment.
-- =============================================================================
-- INSERT INTO users (name, email, status) VALUES
--     ('Alice Seed',   'alice@omiinqa.test',   'active'),
--     ('Bob Seed',     'bob@omiinqa.test',     'active'),
--     ('Inactive Seed','inactive@omiinqa.test','inactive')
-- ON CONFLICT (email) DO NOTHING;  -- PostgreSQL; remove for MySQL

-- INSERT INTO products (name, category, price, stock_qty, active) VALUES
--     ('Widget A',  'electronics', 9.99,  100, TRUE),
--     ('Widget B',  'electronics', 19.99,  50, TRUE),
--     ('Gadget X',  'tools',       4.99,  200, TRUE),
--     ('Legacy Item','discontinued',0.01,    0, FALSE)
-- ON CONFLICT DO NOTHING;          -- PostgreSQL; adjust for MySQL
