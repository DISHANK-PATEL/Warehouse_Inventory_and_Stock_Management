-- ============================================================
-- Warehouse Inventory & Stock Management System
-- Sample Data Script
-- Run AFTER schema.sql and data.sql have been applied.
-- ============================================================

-- ============================================================
-- ADDITIONAL USERS
-- ============================================================

-- Extra Staff user  (password: staff456)
INSERT IGNORE INTO users (id, email, password_hash, full_name, role, is_active, created_at, updated_at)
VALUES (
    'a1b2c3d4-0001-0001-0001-000000000001',
    'staff2@warehouse.com',
    '$2a$12$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi.',
    'Sarah Jones',
    'STAFF',
    1,
    NOW(), NOW()
);

-- Extra Product Manager  (password: productmanager123)
INSERT IGNORE INTO users (id, email, password_hash, full_name, role, is_active, created_at, updated_at)
VALUES (
    'a1b2c3d4-0002-0002-0002-000000000002',
    'pm2@warehouse.com',
    '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/AwE1xK7XU5o5bJbfe',
    'David Miller',
    'PRODUCT_MANAGER',
    1,
    NOW(), NOW()
);

-- ============================================================
-- PRODUCTS
-- 10 realistic products, mix of assigned and unassigned PMs,
-- different breach states and stock levels.
-- ============================================================

-- Fetch admin and PM IDs into variables for FK references
SET @admin_id = (SELECT id FROM users WHERE email = 'admin@warehouse.com' LIMIT 1);
SET @pm1_id   = (SELECT id FROM users WHERE email = 'pm@warehouse.com'    LIMIT 1);
SET @pm2_id   = 'a1b2c3d4-0002-0002-0002-000000000002';

-- 1. Healthy stock, assigned to PM1
INSERT IGNORE INTO products
    (id, name, description, sku, stock_quantity, reserved_quantity,
     min_threshold, max_threshold, breach_status, product_manager_id, created_by, created_at, updated_at)
VALUES (
    'prod-0001-0001-0001-000000000001',
    'Industrial Gloves (L)',
    'Heavy-duty latex industrial gloves, size Large',
    'SKU-GLOVE-L',
    250, 20, 50, 500,
    'NONE',
    @pm1_id, @admin_id, NOW(), NOW()
);

-- 2. BELOW_MIN breach — stock dropped below minimum threshold
INSERT IGNORE INTO products
    (id, name, description, sku, stock_quantity, reserved_quantity,
     min_threshold, max_threshold, breach_status, product_manager_id, created_by, created_at, updated_at)
VALUES (
    'prod-0002-0002-0002-000000000002',
    'Safety Helmet Type-B',
    'ANSI-certified hard hat, yellow',
    'SKU-HELMET-B',
    8, 0, 50, 300,
    'BELOW_MIN',
    @pm1_id, @admin_id, NOW(), NOW()
);

-- 3. ABOVE_MAX breach — stock exceeded maximum threshold
INSERT IGNORE INTO products
    (id, name, description, sku, stock_quantity, reserved_quantity,
     min_threshold, max_threshold, breach_status, product_manager_id, created_by, created_at, updated_at)
VALUES (
    'prod-0003-0003-0003-000000000003',
    'Cable Tie 300mm Pack',
    'Nylon cable ties, black, pack of 100',
    'SKU-CABLE-300',
    620, 0, 20, 400,
    'ABOVE_MAX',
    @pm2_id, @admin_id, NOW(), NOW()
);

-- 4. Unassigned product (no PM)
INSERT IGNORE INTO products
    (id, name, description, sku, stock_quantity, reserved_quantity,
     min_threshold, max_threshold, breach_status, product_manager_id, created_by, created_at, updated_at)
VALUES (
    'prod-0004-0004-0004-000000000004',
    'Packing Tape 48mm',
    'Heavy-duty clear packing tape roll',
    'SKU-TAPE-48',
    180, 10, 30, 600,
    'NONE',
    NULL, @admin_id, NOW(), NOW()
);

-- 5. Low stock, no thresholds set
INSERT IGNORE INTO products
    (id, name, description, sku, stock_quantity, reserved_quantity,
     min_threshold, max_threshold, breach_status, product_manager_id, created_by, created_at, updated_at)
VALUES (
    'prod-0005-0005-0005-000000000005',
    'Barcode Scanner ZB-200',
    'Wireless 2D barcode scanner',
    'SKU-SCAN-ZB200',
    3, 1, NULL, NULL,
    'NONE',
    @pm2_id, @admin_id, NOW(), NOW()
);

-- 6. Assigned to PM2, healthy
INSERT IGNORE INTO products
    (id, name, description, sku, stock_quantity, reserved_quantity,
     min_threshold, max_threshold, breach_status, product_manager_id, created_by, created_at, updated_at)
VALUES (
    'prod-0006-0006-0006-000000000006',
    'Forklift Battery Pack',
    '48V lithium battery pack for counterbalance forklifts',
    'SKU-BATT-FORK48',
    45, 5, 10, 100,
    'NONE',
    @pm2_id, @admin_id, NOW(), NOW()
);

-- 7. Exactly at min threshold (borderline, NONE)
INSERT IGNORE INTO products
    (id, name, description, sku, stock_quantity, reserved_quantity,
     min_threshold, max_threshold, breach_status, product_manager_id, created_by, created_at, updated_at)
VALUES (
    'prod-0007-0007-0007-000000000007',
    'Steel Pallet Rack Beam',
    '2700mm x 100mm galvanised beam for pallet racking',
    'SKU-RACK-BEAM',
    50, 0, 50, 200,
    'NONE',
    @pm1_id, @admin_id, NOW(), NOW()
);

-- 8. BELOW_MIN, PM2 assigned
INSERT IGNORE INTO products
    (id, name, description, sku, stock_quantity, reserved_quantity,
     min_threshold, max_threshold, breach_status, product_manager_id, created_by, created_at, updated_at)
VALUES (
    'prod-0008-0008-0008-000000000008',
    'Shrink Wrap Roll 500mm',
    'Clear stretch film, 500mm x 300m, 23 microns',
    'SKU-WRAP-500',
    4, 0, 25, 150,
    'BELOW_MIN',
    @pm2_id, @admin_id, NOW(), NOW()
);

-- 9. High reserved quantity
INSERT IGNORE INTO products
    (id, name, description, sku, stock_quantity, reserved_quantity,
     min_threshold, max_threshold, breach_status, product_manager_id, created_by, created_at, updated_at)
VALUES (
    'prod-0009-0009-0009-000000000009',
    'Hydraulic Pallet Truck',
    '2500kg capacity, manual pump jack',
    'SKU-PALLET-HYD',
    30, 15, 5, 50,
    'NONE',
    @pm1_id, @admin_id, NOW(), NOW()
);

-- 10. Zero stock, BELOW_MIN
INSERT IGNORE INTO products
    (id, name, description, sku, stock_quantity, reserved_quantity,
     min_threshold, max_threshold, breach_status, product_manager_id, created_by, created_at, updated_at)
VALUES (
    'prod-0010-0010-0010-000000000010',
    'High-Viz Vest (M)',
    'EN ISO 20471 Class 2, medium, yellow',
    'SKU-HIVIZ-M',
    0, 0, 20, 200,
    'BELOW_MIN',
    @pm2_id, @admin_id, NOW(), NOW()
);

-- ============================================================
-- STOCK MOVEMENTS
-- Simulate realistic history for a few products
-- ============================================================

SET @staff_id  = (SELECT id FROM users WHERE email = 'staff@warehouse.com' LIMIT 1);
SET @staff2_id = 'a1b2c3d4-0001-0001-0001-000000000001';

-- Product 1 (Gloves): initial stock-in, then two removals
INSERT IGNORE INTO stock_movements
    (id, product_id, performed_by, movement_type, quantity, stock_before, stock_after, notes, created_at)
VALUES
    ('mov-0001', 'prod-0001-0001-0001-000000000001', @admin_id,  'ADD',    300, 0,   300, 'Initial stock receipt — PO#4821',              DATE_SUB(NOW(), INTERVAL 10 DAY)),
    ('mov-0002', 'prod-0001-0001-0001-000000000001', @staff_id,  'REMOVE',  30, 300, 270, 'Issued to assembly line A',                    DATE_SUB(NOW(), INTERVAL 7  DAY)),
    ('mov-0003', 'prod-0001-0001-0001-000000000001', @staff2_id, 'REMOVE',  20, 270, 250, 'Issued to maintenance team',                   DATE_SUB(NOW(), INTERVAL 2  DAY));

-- Product 2 (Safety Helmet): received, then heavily issued causing breach
INSERT IGNORE INTO stock_movements
    (id, product_id, performed_by, movement_type, quantity, stock_before, stock_after, notes, created_at)
VALUES
    ('mov-0004', 'prod-0002-0002-0002-000000000002', @admin_id, 'ADD',    100, 0,   100, 'Stock received — PO#4890',                     DATE_SUB(NOW(), INTERVAL 14 DAY)),
    ('mov-0005', 'prod-0002-0002-0002-000000000002', @staff_id, 'REMOVE',  40, 100,  60, 'Dispatched to site B',                         DATE_SUB(NOW(), INTERVAL 10 DAY)),
    ('mov-0006', 'prod-0002-0002-0002-000000000002', @staff_id, 'REMOVE',  52,  60,   8, 'Emergency dispatch — safety inspection',        DATE_SUB(NOW(), INTERVAL 3  DAY));

-- Product 3 (Cable Ties): over-stocked by large addition
INSERT IGNORE INTO stock_movements
    (id, product_id, performed_by, movement_type, quantity, stock_before, stock_after, notes, created_at)
VALUES
    ('mov-0007', 'prod-0003-0003-0003-000000000003', @admin_id, 'ADD', 200,   0, 200, 'Initial receipt',                                DATE_SUB(NOW(), INTERVAL 20 DAY)),
    ('mov-0008', 'prod-0003-0003-0003-000000000003', @admin_id, 'ADD', 420, 200, 620, 'Bulk purchase — supplier discount (over-stock)', DATE_SUB(NOW(), INTERVAL 5  DAY));

-- Product 6 (Forklift Battery): normal add and reserve cycle
INSERT IGNORE INTO stock_movements
    (id, product_id, performed_by, movement_type, quantity, stock_before, stock_after, notes, created_at)
VALUES
    ('mov-0009', 'prod-0006-0006-0006-000000000006', @admin_id, 'ADD',     50,  0, 50, 'Received from supplier ElectroPower Ltd',        DATE_SUB(NOW(), INTERVAL 8 DAY)),
    ('mov-0010', 'prod-0006-0006-0006-000000000006', @staff_id, 'RESERVE',  5, 50, 45, 'Reserved for planned maintenance 15-Mar',       DATE_SUB(NOW(), INTERVAL 2 DAY));

-- ============================================================
-- STOCK RESERVATIONS
-- ============================================================

INSERT IGNORE INTO stock_reservations
    (id, product_id, reserved_by, quantity, status, expires_at, released_at, created_at)
VALUES
    -- Active reservation on Forklift Battery
    ('res-0001', 'prod-0006-0006-0006-000000000006', @staff_id,  5, 'ACTIVE',   DATE_ADD(NOW(), INTERVAL 7 DAY), NULL, DATE_SUB(NOW(), INTERVAL 2 DAY)),
    -- Active reservation on Pallet Truck (product 9)
    ('res-0002', 'prod-0009-0009-0009-000000000009', @staff2_id, 10, 'ACTIVE',  DATE_ADD(NOW(), INTERVAL 3 DAY), NULL, DATE_SUB(NOW(), INTERVAL 1 DAY)),
    -- Released reservation (historical)
    ('res-0003', 'prod-0009-0009-0009-000000000009', @staff_id,  5, 'RELEASED', DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY)),
    -- Expired reservation
    ('res-0004', 'prod-0004-0004-0004-000000000004', @staff_id,  10, 'EXPIRED', DATE_SUB(NOW(), INTERVAL 1 DAY), NULL, DATE_SUB(NOW(), INTERVAL 4 DAY));

-- ============================================================
-- STOCK ALERTS
-- One alert per breached product
-- ============================================================

INSERT IGNORE INTO stock_alerts
    (id, product_id, breach_type, stock_at_breach, threshold_value, created_at)
VALUES
    -- Safety Helmet went BELOW_MIN (threshold = 50, stock dropped to 8)
    ('alrt-0001', 'prod-0002-0002-0002-000000000002', 'BELOW_MIN', 8,   50,  DATE_SUB(NOW(), INTERVAL 3 DAY)),
    -- Cable Tie went ABOVE_MAX (threshold = 400, stock jumped to 620)
    ('alrt-0002', 'prod-0003-0003-0003-000000000003', 'ABOVE_MAX', 620, 400, DATE_SUB(NOW(), INTERVAL 5 DAY)),
    -- Shrink Wrap BELOW_MIN
    ('alrt-0003', 'prod-0008-0008-0008-000000000008', 'BELOW_MIN', 4,   25,  DATE_SUB(NOW(), INTERVAL 6 DAY)),
    -- High-Viz Vest BELOW_MIN
    ('alrt-0004', 'prod-0010-0010-0010-000000000010', 'BELOW_MIN', 0,   20,  DATE_SUB(NOW(), INTERVAL 1 DAY));

-- ============================================================
-- NOTIFICATION LOGS
-- Simulate delivered and failed notification attempts
-- ============================================================

INSERT IGNORE INTO notification_logs
    (id, alert_id, receiver_id, status, retry_count, failure_reason, last_attempted_at, next_retry_at, delivered_at, created_at)
VALUES
    -- Safety Helmet alert: delivered to PM1
    ('nlog-0001', 'alrt-0001', @pm1_id, 'DELIVERED', 0, NULL,
     DATE_SUB(NOW(), INTERVAL 3 DAY), NULL, DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY)),
    -- Cable Tie alert: delivered to PM2
    ('nlog-0002', 'alrt-0002', @pm2_id, 'DELIVERED', 0, NULL,
     DATE_SUB(NOW(), INTERVAL 5 DAY), NULL, DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY)),
    -- Shrink Wrap alert: FAILED after 2 retries (simulate bad SMTP)
    ('nlog-0003', 'alrt-0003', @pm2_id, 'FAILED', 2, 'javax.mail.AuthenticationFailedException: 535 Bad credentials',
     DATE_SUB(NOW(), INTERVAL 1 HOUR), DATE_ADD(NOW(), INTERVAL 5 MINUTE), NULL, DATE_SUB(NOW(), INTERVAL 6 DAY)),
    -- High-Viz alert: pending (just created)
    ('nlog-0004', 'alrt-0004', @pm2_id, 'PENDING', 0, NULL,
     NULL, NOW(), NULL, DATE_SUB(NOW(), INTERVAL 1 DAY));

-- ============================================================
-- BULK OPERATION JOB (historical sample)
-- ============================================================

INSERT IGNORE INTO bulk_operation_jobs
    (id, submitted_by_id, status, total_rows, successful_rows, failed_rows,
     row_results, submitted_at, completed_at)
VALUES (
    'bulk-0001-0001-0001-000000000001',
    @admin_id,
    'COMPLETED',
    5, 4, 1,
    '[{"row":1,"status":"SUCCESS","message":"Created: Industrial Gloves (L)"},{"row":2,"status":"SUCCESS","message":"Created: Safety Helmet Type-B"},{"row":3,"status":"SUCCESS","message":"Created: Cable Tie 300mm Pack"},{"row":4,"status":"FAILED","message":"SKU already exists: SKU-TAPE-48"},{"row":5,"status":"SUCCESS","message":"Created: Barcode Scanner ZB-200"}]',
    DATE_SUB(NOW(), INTERVAL 15 DAY),
    DATE_SUB(NOW(), INTERVAL 15 DAY)
);

-- ============================================================
-- END OF SAMPLE DATA
-- ============================================================