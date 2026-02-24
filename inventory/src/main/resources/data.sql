INSERT IGNORE INTO users (email, password_hash, full_name, role, is_active, created_at, updated_at)
VALUES (
    'admin@warehouse.com',
    '$2a$12$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi.',
    'Admin User',
    'ADMIN',
    1,
    NOW(),
    NOW()
);

INSERT IGNORE INTO users (email, password_hash, full_name, role, is_active, created_at, updated_at)
VALUES (
    'staff@warehouse.com',
    '$2a$12$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi.',
    'Staff User',
    'STAFF',
    1,
    NOW(),
    NOW()
);