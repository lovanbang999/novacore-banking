-- 1. Customers
CREATE TABLE customers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    full_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    phone_number VARCHAR(50) UNIQUE NOT NULL,
    kyc_status VARCHAR(50) NOT NULL DEFAULT 'PENDING_KYC', -- PENDING_KYC, VERIFIED, REJECTED
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 2. General Ledger Accounts (Chart of Accounts)
CREATE TABLE gl_accounts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(20) UNIQUE NOT NULL,                      -- e.g., 'FX_CLEARING', 'FEE_INCOME', 'VAULT'
    account_type VARCHAR(20) NOT NULL,                     -- ASSET, LIABILITY, INCOME, EXPENSE
    name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Baseline GL System Accounts
INSERT INTO gl_accounts (id, code, account_type, name) VALUES 
('00000000-0000-0000-0000-000000000001', 'FX_CLEARING', 'LIABILITY', 'Foreign Exchange Clearing Account'),
('00000000-0000-0000-0000-000000000002', 'FEE_INCOME', 'INCOME', 'Transaction Fee Income Account'),
('00000000-0000-0000-0000-000000000003', 'VAULT', 'ASSET', 'Central Cash Vault Account');

-- 3. Checking / Settlement Accounts
CREATE TABLE accounts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id UUID NOT NULL REFERENCES customers(id),
    account_number VARCHAR(30) UNIQUE NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'VND',
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING_KYC',     -- PENDING_KYC, ACTIVE, FROZEN, CLOSED
    current_balance NUMERIC(19, 4) NOT NULL DEFAULT 0.0000,
    version BIGINT NOT NULL DEFAULT 0,                     -- For Optimistic Locking
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_accounts_customer ON accounts(customer_id);

-- 4. Account Holds / Reservations (Available Balance = Current Balance - Active Holds)
CREATE TABLE account_holds (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id UUID NOT NULL REFERENCES accounts(id),
    amount NUMERIC(19, 4) NOT NULL CHECK (amount > 0),
    reason VARCHAR(255),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',          -- ACTIVE, RELEASED, CAPTURED
    expires_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_holds_account_status ON account_holds(account_id, status) WHERE status = 'ACTIVE';

-- 5. Foreign Exchange (FX) Rates
CREATE TABLE fx_rates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    base_currency VARCHAR(3) NOT NULL,                     -- e.g. 'USD'
    quote_currency VARCHAR(3) NOT NULL,                    -- e.g. 'VND'
    rate NUMERIC(18, 8) NOT NULL CHECK (rate > 0),         -- 1 base_currency = rate * quote_currency
    source VARCHAR(50) NOT NULL DEFAULT 'MANUAL',          -- MANUAL, EXTERNAL_PROVIDER
    effective_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (base_currency, quote_currency, effective_at)
);

CREATE INDEX idx_fx_rates_lookup ON fx_rates(base_currency, quote_currency, effective_at DESC);

-- Baseline FX Seed Rates
INSERT INTO fx_rates (base_currency, quote_currency, rate, source, effective_at) VALUES 
('USD', 'VND', 25450.00000000, 'MANUAL', CURRENT_TIMESTAMP),
('VND', 'USD', 0.00003929, 'MANUAL', CURRENT_TIMESTAMP),
('AUD', 'VND', 16800.00000000, 'MANUAL', CURRENT_TIMESTAMP),
('VND', 'AUD', 0.00005952, 'MANUAL', CURRENT_TIMESTAMP),
('USD', 'AUD', 1.51500000, 'MANUAL', CURRENT_TIMESTAMP),
('AUD', 'USD', 0.66000000, 'MANUAL', CURRENT_TIMESTAMP);

-- 6. Transactions
CREATE TABLE transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reference_number VARCHAR(64) UNIQUE NOT NULL,
    source_account_id UUID NOT NULL REFERENCES accounts(id),
    destination_account_id UUID NOT NULL REFERENCES accounts(id),
    source_amount NUMERIC(19, 4) NOT NULL CHECK (source_amount > 0),
    source_currency VARCHAR(3) NOT NULL,
    destination_amount NUMERIC(19, 4) NOT NULL CHECK (destination_amount > 0),
    destination_currency VARCHAR(3) NOT NULL,
    fx_rate_applied NUMERIC(18, 8),                        -- NULL if same currency, rate if cross-currency
    status VARCHAR(30) NOT NULL,                           -- INITIATED, SETTLED, FAILED
    failure_reason VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT chk_different_accounts CHECK (source_account_id != destination_account_id)
);

CREATE INDEX idx_transactions_status_created ON transactions(status, created_at);

-- 7. Double-Entry Ledger Entries (APPEND-ONLY: Polymorphic reference & Currency per row)
CREATE TABLE ledger_entries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_id UUID NOT NULL REFERENCES transactions(id),
    account_id UUID NOT NULL,                              -- References accounts(id) or gl_accounts(id)
    account_ref_type VARCHAR(10) NOT NULL DEFAULT 'CUSTOMER', -- 'CUSTOMER' or 'GL'
    entry_type VARCHAR(10) NOT NULL,                      -- 'DEBIT' or 'CREDIT'
    amount NUMERIC(19, 4) NOT NULL CHECK (amount > 0),
    currency VARCHAR(3) NOT NULL,                         -- Currency of this leg
    running_balance NUMERIC(19, 4) NOT NULL,               -- Balance snapshot immediately post-entry
    fx_rate_applied NUMERIC(18, 8),                       -- Populated on cross-currency clearing legs
    base_currency_equivalent NUMERIC(19, 4),               -- Normalized equivalent for audit
    description VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_ledger_account_created ON ledger_entries(account_ref_type, account_id, created_at DESC);
CREATE INDEX idx_ledger_currency ON ledger_entries(currency);

-- 8. Idempotency Records
CREATE TABLE idempotency_records (
    key VARCHAR(255) PRIMARY KEY,
    request_hash VARCHAR(64) NOT NULL,
    status VARCHAR(30) NOT NULL,                          -- PENDING, COMPLETED, FAILED
    response_body TEXT,
    http_status_code INT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_idempotency_pending ON idempotency_records(status, created_at) WHERE status = 'PENDING';

-- 9. Transactional Outbox Events
CREATE TABLE outbox_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_type VARCHAR(100) NOT NULL,                 -- 'TRANSFER', 'ACCOUNT'
    aggregate_id VARCHAR(100) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload JSONB NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',        -- PENDING, PUBLISHED, FAILED
    retry_count INT DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_outbox_pending ON outbox_events(status, created_at) WHERE status = 'PENDING';
