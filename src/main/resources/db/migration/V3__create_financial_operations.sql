CREATE TABLE financial_operations (
    id UUID PRIMARY KEY,
    type TEXT NOT NULL,
    source_account_id UUID REFERENCES accounts(id),
    target_account_id UUID REFERENCES accounts(id),
    amount NUMERIC(19, 2) NOT NULL,
    currency TEXT NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_financial_operation_type CHECK (type IN ('DEPOSIT', 'WITHDRAWAL', 'TRANSFER')),
    CONSTRAINT chk_financial_operation_amount CHECK (amount > 0),
    CONSTRAINT chk_financial_operation_accounts CHECK (
        (type = 'DEPOSIT' AND source_account_id IS NULL AND target_account_id IS NOT NULL) OR
        (type = 'WITHDRAWAL' AND source_account_id IS NOT NULL AND target_account_id IS NULL) OR
        (type = 'TRANSFER' AND source_account_id IS NOT NULL AND target_account_id IS NOT NULL
            AND source_account_id <> target_account_id)
    )
);

CREATE INDEX idx_financial_operations_source ON financial_operations(source_account_id, occurred_at DESC);
CREATE INDEX idx_financial_operations_target ON financial_operations(target_account_id, occurred_at DESC);
