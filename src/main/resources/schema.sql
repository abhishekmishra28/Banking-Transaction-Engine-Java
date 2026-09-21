-- SQLite Schema for Banking Transaction Engine

CREATE TABLE IF NOT EXISTS customers (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    created_at TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS accounts (
    id TEXT PRIMARY KEY,
    customer_id TEXT NOT NULL,
    type TEXT NOT NULL,
    balance TEXT NOT NULL, -- Storing as exact string (BigDecimal) to avoid float issues
    is_active INTEGER NOT NULL DEFAULT 1,
    created_at TEXT NOT NULL,
    overdraft_limit TEXT, -- Only for CURRENT accounts
    FOREIGN KEY(customer_id) REFERENCES customers(id)
);

CREATE TABLE IF NOT EXISTS transactions (
    id TEXT PRIMARY KEY,
    type TEXT NOT NULL,
    source_account_id TEXT,
    destination_account_id TEXT,
    amount TEXT NOT NULL,
    status TEXT NOT NULL,
    failure_reason TEXT,
    timestamp TEXT NOT NULL,
    FOREIGN KEY(source_account_id) REFERENCES accounts(id),
    FOREIGN KEY(destination_account_id) REFERENCES accounts(id)
);

CREATE TABLE IF NOT EXISTS audit_logs (
    id TEXT PRIMARY KEY,
    event_type TEXT NOT NULL,
    description TEXT NOT NULL,
    entity_id TEXT,
    timestamp TEXT NOT NULL
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_accounts_customer_id ON accounts(customer_id);
CREATE INDEX IF NOT EXISTS idx_transactions_source ON transactions(source_account_id);
CREATE INDEX IF NOT EXISTS idx_transactions_destination ON transactions(destination_account_id);
CREATE INDEX IF NOT EXISTS idx_transactions_timestamp ON transactions(timestamp);
