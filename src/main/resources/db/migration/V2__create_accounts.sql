CREATE TABLE accounts (
    id UUID PRIMARY KEY,
    account_number TEXT NOT NULL UNIQUE,
    client_id UUID NOT NULL REFERENCES clients(id),
    account_type TEXT NOT NULL,
    currency TEXT NOT NULL,
    balance NUMERIC(19, 2) NOT NULL DEFAULT 0,
    status TEXT NOT NULL DEFAULT 'OPEN',
    opened_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_accounts_balance_nonnegative CHECK (balance >= 0),
    CONSTRAINT chk_accounts_type CHECK (account_type IN ('CURRENT', 'SAVINGS')),
    CONSTRAINT chk_accounts_status CHECK (status IN ('OPEN', 'BLOCKED', 'CLOSED'))
);

CREATE INDEX idx_accounts_client_id ON accounts(client_id);
