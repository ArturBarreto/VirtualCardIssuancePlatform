-- Table: card
CREATE TABLE card (
    id UUID PRIMARY KEY,
    cardholder_name VARCHAR(100) NOT NULL,
    balance DECIMAL(18,2) NOT NULL CHECK (balance >= 0),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- 'ACTIVE', 'BLOCKED'
    version INT NOT NULL DEFAULT 0,               -- for optimistic locking
    created_at TIMESTAMP NOT NULL
);

-- Table: transaction
CREATE TABLE transaction (
     id UUID PRIMARY KEY,
     card_id UUID NOT NULL,
     type VARCHAR(20) NOT NULL, -- 'SPEND', 'TOPUP'
     amount DECIMAL(18,2) NOT NULL CHECK (amount > 0),
     created_at TIMESTAMP NOT NULL,
     CONSTRAINT fk_card FOREIGN KEY(card_id) REFERENCES card(id)
);

-- Optional: add indexes for performance
CREATE INDEX idx_transaction_card_id ON transaction(card_id);
CREATE INDEX idx_transaction_created_at ON transaction(created_at);