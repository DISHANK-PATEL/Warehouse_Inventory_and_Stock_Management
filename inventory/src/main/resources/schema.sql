-- ============================================================
-- Warehouse Inventory and Stock Management System
-- Schema — ordered by foreign key dependencies
-- ============================================================

-- 1. users
CREATE TABLE IF NOT EXISTS users (
                                     id              VARCHAR(36)     NOT NULL,
    full_name       VARCHAR(100)    NOT NULL,
    email           VARCHAR(120)    NOT NULL UNIQUE,
    password_hash   VARCHAR(255)    NOT NULL,
    role            ENUM('ADMIN', 'STAFF', 'PRODUCT_MANAGER') NOT NULL,
    is_active       TINYINT(1)      NOT NULL DEFAULT 1,
    created_at      DATETIME        NOT NULL,
    updated_at      DATETIME        NOT NULL,
    PRIMARY KEY (id)
    );

-- 2. products (depends on users)
CREATE TABLE IF NOT EXISTS products (
                                        id                  VARCHAR(36)     NOT NULL,
    name                VARCHAR(100)    NOT NULL UNIQUE,
    description         TEXT,
    sku                 VARCHAR(50)     NOT NULL UNIQUE,
    stock_quantity      INT             NOT NULL DEFAULT 0,
    reserved_quantity   INT             NOT NULL DEFAULT 0,
    min_threshold       INT,
    max_threshold       INT,
    breach_status       ENUM('NONE', 'BELOW_MIN', 'ABOVE_MAX') NOT NULL DEFAULT 'NONE',
    product_manager_id  VARCHAR(36),
    created_by          VARCHAR(36)     NOT NULL,
    created_at          DATETIME        NOT NULL,
    updated_at          DATETIME        NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_product_manager    FOREIGN KEY (product_manager_id) REFERENCES users(id),
    CONSTRAINT fk_product_created_by FOREIGN KEY (created_by)         REFERENCES users(id),
    INDEX idx_products_product_manager_id (product_manager_id),
    INDEX idx_products_breach_status      (breach_status)
    );

-- 3. stock_movements (depends on products, users)
CREATE TABLE IF NOT EXISTS stock_movements (
                                               id              VARCHAR(36)     NOT NULL,
    product_id      VARCHAR(36)     NOT NULL,
    performed_by    VARCHAR(36)     NOT NULL,
    movement_type   ENUM('ADD', 'REMOVE', 'RESERVE', 'RELEASE') NOT NULL,
    quantity        INT             NOT NULL,
    stock_before    INT             NOT NULL,
    stock_after     INT             NOT NULL,
    notes           TEXT,
    created_at      DATETIME        NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_movement_product      FOREIGN KEY (product_id)   REFERENCES products(id),
    CONSTRAINT fk_movement_performed_by FOREIGN KEY (performed_by) REFERENCES users(id),
    INDEX idx_stock_movements_product_id   (product_id),
    INDEX idx_stock_movements_created_at   (created_at),
    INDEX idx_stock_movements_performed_by (performed_by)
    );

-- 4. stock_reservations (depends on products, users)
CREATE TABLE IF NOT EXISTS stock_reservations (
                                                  id              VARCHAR(36)     NOT NULL,
    product_id      VARCHAR(36)     NOT NULL,
    reserved_by     VARCHAR(36)     NOT NULL,
    quantity        INT             NOT NULL,
    status          ENUM('ACTIVE', 'RELEASED', 'EXPIRED') NOT NULL DEFAULT 'ACTIVE',
    expires_at      DATETIME        NOT NULL,
    released_at     DATETIME,
    created_at      DATETIME        NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_reservation_product     FOREIGN KEY (product_id)  REFERENCES products(id),
    CONSTRAINT fk_reservation_reserved_by FOREIGN KEY (reserved_by) REFERENCES users(id),
    INDEX idx_stock_reservations_status_expires_at (status, expires_at),
    INDEX idx_stock_reservations_product_id        (product_id)
    );

-- 5. stock_alerts (depends on products)
CREATE TABLE IF NOT EXISTS stock_alerts (
                                            id              VARCHAR(36)     NOT NULL,
    product_id      VARCHAR(36)     NOT NULL,
    breach_type     ENUM('BELOW_MIN', 'ABOVE_MAX') NOT NULL,
    stock_at_breach INT             NOT NULL,
    threshold_value INT             NOT NULL,
    created_at      DATETIME        NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_alert_product FOREIGN KEY (product_id) REFERENCES products(id),
    INDEX idx_stock_alerts_product_id (product_id)
    );

-- 6. notification_logs (depends on stock_alerts, users)
CREATE TABLE IF NOT EXISTS notification_logs (
                                                 id                  VARCHAR(36)     NOT NULL,
    alert_id            VARCHAR(36)     NOT NULL,
    receiver_id         VARCHAR(36)     NOT NULL,
    status              ENUM('PENDING', 'DELIVERED', 'FAILED') NOT NULL DEFAULT 'PENDING',
    retry_count         INT             NOT NULL DEFAULT 0,
    failure_reason      TEXT,
    last_attempted_at   DATETIME,
    next_retry_at       DATETIME,
    delivered_at        DATETIME,
    created_at          DATETIME        NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_notification_alert    FOREIGN KEY (alert_id)    REFERENCES stock_alerts(id),
    CONSTRAINT fk_notification_receiver FOREIGN KEY (receiver_id) REFERENCES users(id),
    INDEX idx_notification_logs_status_retry (status, retry_count),
    INDEX idx_notification_logs_alert_id     (alert_id)
    );

-- 7. bulk_operation_jobs (depends on users)
CREATE TABLE IF NOT EXISTS bulk_operation_jobs (
                                                   id              VARCHAR(36)     NOT NULL,
    submitted_by_id VARCHAR(36)     NOT NULL,
    status          ENUM('PROCESSING', 'COMPLETED', 'FAILED') NOT NULL DEFAULT 'PROCESSING',
    total_rows      INT             NOT NULL DEFAULT 0,
    successful_rows INT             NOT NULL DEFAULT 0,
    failed_rows     INT             NOT NULL DEFAULT 0,
    row_results     TEXT,
    submitted_at    DATETIME        NOT NULL,
    completed_at    DATETIME,
    PRIMARY KEY (id),
    CONSTRAINT fk_bulk_job_submitted_by FOREIGN KEY (submitted_by_id) REFERENCES users(id)
    );