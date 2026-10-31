CREATE TABLE clients (
    id UUID PRIMARY KEY,
    first_name text NOT NULL,
    last_name text NOT NULL,
    personal_code text NOT NULL UNIQUE,
    phone text,
    email text,
    status text NOT NULL DEFAULT 'REQUIRES_VERIFICATION',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_clients_name ON clients (LOWER(first_name), LOWER(last_name));
