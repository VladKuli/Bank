ALTER TABLE accounts DROP CONSTRAINT chk_accounts_status;

ALTER TABLE accounts
    ADD CONSTRAINT chk_accounts_status CHECK (status IN ('OPEN', 'FROZEN', 'BLOCKED', 'CLOSED'));
