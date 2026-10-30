ALTER TABLE clients ADD COLUMN password_hash TEXT;
ALTER TABLE clients ADD COLUMN membership_tier TEXT NOT NULL DEFAULT 'STANDARD';
CREATE UNIQUE INDEX ux_clients_registered_email_ci ON clients (LOWER(email)) WHERE password_hash IS NOT NULL;
CREATE INDEX idx_clients_registered_name ON clients (LOWER(last_name), LOWER(first_name)) WHERE password_hash IS NOT NULL;

ALTER TABLE financial_operations ADD COLUMN transaction_reference TEXT;
ALTER TABLE financial_operations ADD COLUMN description TEXT;
ALTER TABLE financial_operations ADD COLUMN status TEXT NOT NULL DEFAULT 'COMPLETED';
ALTER TABLE financial_operations ADD COLUMN idempotency_key UUID;
UPDATE financial_operations
SET transaction_reference = 'NB-' || UPPER(REPLACE(id::text, '-', ''))
WHERE transaction_reference IS NULL;
ALTER TABLE financial_operations ALTER COLUMN transaction_reference SET NOT NULL;
CREATE UNIQUE INDEX ux_financial_operations_reference ON financial_operations (transaction_reference);
CREATE UNIQUE INDEX ux_financial_operations_idempotency_key ON financial_operations (idempotency_key) WHERE idempotency_key IS NOT NULL;

CREATE TABLE payment_cards (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL REFERENCES clients(id),
    account_id UUID NOT NULL REFERENCES accounts(id),
    tier TEXT NOT NULL,
    last_four VARCHAR(4) NOT NULL,
    expires_on DATE NOT NULL,
    status TEXT NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_payment_cards_tier CHECK (tier IN ('STANDARD', 'PREMIUM', 'VIP')),
    CONSTRAINT chk_payment_cards_status CHECK (status IN ('ACTIVE', 'FROZEN', 'BLOCKED'))
);
CREATE INDEX idx_payment_cards_owner ON payment_cards(owner_id, created_at DESC);
CREATE UNIQUE INDEX ux_payment_cards_account_tier ON payment_cards(owner_id, account_id, tier)
    WHERE status <> 'BLOCKED';

CREATE TABLE card_requests (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL REFERENCES clients(id),
    account_id UUID NOT NULL REFERENCES accounts(id),
    tier TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'PENDING',
    decision_reason TEXT,
    requested_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    decided_at TIMESTAMPTZ,
    decided_by TEXT,
    CONSTRAINT chk_card_requests_tier CHECK (tier IN ('STANDARD', 'PREMIUM', 'VIP')),
    CONSTRAINT chk_card_requests_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED'))
);
CREATE INDEX idx_card_requests_status ON card_requests(status, requested_at DESC);

CREATE TABLE membership_requests (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL REFERENCES clients(id),
    requested_tier TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'PENDING',
    decision_reason TEXT,
    requested_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    decided_at TIMESTAMPTZ,
    decided_by TEXT,
    CONSTRAINT chk_membership_requests_tier CHECK (requested_tier IN ('PREMIUM', 'VIP')),
    CONSTRAINT chk_membership_requests_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED'))
);
CREATE INDEX idx_membership_requests_status ON membership_requests(status, requested_at DESC);

CREATE TABLE loan_applications (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL REFERENCES clients(id),
    amount NUMERIC(19, 2) NOT NULL,
    term_months INTEGER NOT NULL,
    purpose TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'PENDING',
    decision_reason TEXT,
    requested_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    decided_at TIMESTAMPTZ,
    decided_by TEXT,
    CONSTRAINT chk_loan_applications_amount CHECK (amount > 0 AND amount <= 100000),
    CONSTRAINT chk_loan_applications_term CHECK (term_months BETWEEN 6 AND 84),
    CONSTRAINT chk_loan_applications_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED'))
);
CREATE INDEX idx_loan_applications_owner ON loan_applications(owner_id, requested_at DESC);
CREATE INDEX idx_loan_applications_status ON loan_applications(status, requested_at DESC);

CREATE TABLE support_requests (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL REFERENCES clients(id),
    category TEXT NOT NULL,
    subject TEXT NOT NULL,
    message TEXT NOT NULL,
    employee_response TEXT,
    status TEXT NOT NULL DEFAULT 'OPEN',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_support_requests_status CHECK (status IN ('OPEN', 'IN_PROGRESS', 'WAITING_FOR_CUSTOMER', 'RESOLVED'))
);
CREATE INDEX idx_support_requests_owner ON support_requests(owner_id, created_at DESC);
CREATE INDEX idx_support_requests_status ON support_requests(status, updated_at DESC);
