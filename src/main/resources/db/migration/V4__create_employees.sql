CREATE TABLE employees (
    id UUID PRIMARY KEY,
    username TEXT NOT NULL,
    password_hash TEXT NOT NULL,
    role TEXT NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_employees_role CHECK (role IN ('ADMIN', 'MANAGER', 'OPERATOR'))
);

CREATE UNIQUE INDEX ux_employees_username_ci ON employees (LOWER(username));
