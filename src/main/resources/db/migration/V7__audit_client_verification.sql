ALTER TABLE clients ADD COLUMN verification_approved_at TIMESTAMPTZ;
ALTER TABLE clients ADD COLUMN verification_approved_by VARCHAR(100);
