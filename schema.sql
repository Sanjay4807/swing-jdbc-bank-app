-- =============================================================================
-- Mini Banking System Database Schema (Oracle SQL - 3NF Normalized)
-- Compatible with Oracle 12c, 19c, 21c, 23c
-- =============================================================================

-- Clean up existing objects if re-running script
BEGIN
   EXECUTE IMMEDIATE 'DROP TABLE TRANSACTIONS CASCADE CONSTRAINTS';
EXCEPTION WHEN OTHERS THEN NULL;
END;
/

BEGIN
   EXECUTE IMMEDIATE 'DROP TABLE ACCOUNTS CASCADE CONSTRAINTS';
EXCEPTION WHEN OTHERS THEN NULL;
END;
/

BEGIN
   EXECUTE IMMEDIATE 'DROP TABLE CUSTOMERS CASCADE CONSTRAINTS';
EXCEPTION WHEN OTHERS THEN NULL;
END;
/

BEGIN
   EXECUTE IMMEDIATE 'DROP SEQUENCE SEQ_CUSTOMER_ID';
EXCEPTION WHEN OTHERS THEN NULL;
END;
/

BEGIN
   EXECUTE IMMEDIATE 'DROP SEQUENCE SEQ_TRANSACTION_ID';
EXCEPTION WHEN OTHERS THEN NULL;
END;
/

-- -----------------------------------------------------------------------------
-- 1. CUSTOMERS Table (1st Normal Form to 3rd Normal Form compliant)
-- Contains customer profile information with unique identity.
-- -----------------------------------------------------------------------------
CREATE SEQUENCE SEQ_CUSTOMER_ID START WITH 1001 INCREMENT BY 1 NOCACHE NOCYCLE;

CREATE TABLE CUSTOMERS (
    customer_id NUMBER(10) PRIMARY KEY,
    full_name   VARCHAR2(100) NOT NULL,
    email       VARCHAR2(100) NOT NULL UNIQUE,
    pin_hash    VARCHAR2(64)  NOT NULL, -- Stored as PIN or hashed credentials
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- -----------------------------------------------------------------------------
-- 2. ACCOUNTS Table (3NF Compliant)
-- Maps customers to bank accounts. Account numbers are unique identifiers.
-- Foreign key customer_id references CUSTOMERS.
-- -----------------------------------------------------------------------------
CREATE TABLE ACCOUNTS (
    account_no   VARCHAR2(20) PRIMARY KEY,
    customer_id  NUMBER(10) NOT NULL,
    account_type VARCHAR2(20) DEFAULT 'SAVINGS' NOT NULL,
    balance      NUMBER(15, 2) DEFAULT 0.00 NOT NULL,
    status       VARCHAR2(15) DEFAULT 'ACTIVE' NOT NULL,
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT fk_accounts_customer FOREIGN KEY (customer_id) 
        REFERENCES CUSTOMERS(customer_id) ON DELETE CASCADE,
    CONSTRAINT chk_account_type CHECK (account_type IN ('SAVINGS', 'CHECKING', 'BUSINESS', 'CURRENT')),
    CONSTRAINT chk_account_status CHECK (status IN ('ACTIVE', 'FROZEN', 'CLOSED')),
    CONSTRAINT chk_account_balance CHECK (balance >= 0)
);

-- -----------------------------------------------------------------------------
-- 3. TRANSACTIONS Table (3NF Compliant)
-- Audit log of all financial movements (DEPOSIT, WITHDRAWAL, TRANSFER).
-- -----------------------------------------------------------------------------
CREATE SEQUENCE SEQ_TRANSACTION_ID START WITH 50001 INCREMENT BY 1 NOCACHE NOCYCLE;

CREATE TABLE TRANSACTIONS (
    transaction_id    NUMBER(12) PRIMARY KEY,
    account_no        VARCHAR2(20) NOT NULL,
    transaction_type  VARCHAR2(20) NOT NULL,
    amount            NUMBER(15, 2) NOT NULL,
    target_account_no VARCHAR2(20) NULL,
    description       VARCHAR2(255),
    transaction_date  TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT fk_trans_account FOREIGN KEY (account_no) 
        REFERENCES ACCOUNTS(account_no),
    CONSTRAINT fk_trans_target_account FOREIGN KEY (target_account_no) 
        REFERENCES ACCOUNTS(account_no),
    CONSTRAINT chk_trans_type CHECK (transaction_type IN ('DEPOSIT', 'WITHDRAWAL', 'TRANSFER')),
    CONSTRAINT chk_trans_amount CHECK (amount > 0)
);

-- -----------------------------------------------------------------------------
-- Indexes for performance optimization on foreign keys and lookup columns
-- -----------------------------------------------------------------------------
CREATE INDEX idx_accounts_customer_id ON ACCOUNTS(customer_id);
CREATE INDEX idx_trans_account_no ON TRANSACTIONS(account_no);
CREATE INDEX idx_trans_target_account ON TRANSACTIONS(target_account_no);
CREATE INDEX idx_trans_date ON TRANSACTIONS(transaction_date DESC);

-- -----------------------------------------------------------------------------
-- Sample Seed Data for Instant Testing
-- Customer 1: Account ACC1001, PIN: 1234, Initial Balance: $2500.00
-- Customer 2: Account ACC1002, PIN: 5678, Initial Balance: $1200.00
-- Customer 3: Account ACC1003, PIN: 9999, Initial Balance: $5000.00
-- -----------------------------------------------------------------------------
INSERT INTO CUSTOMERS (customer_id, full_name, email, pin_hash) 
VALUES (1001, 'Alex Morgan', 'alex.morgan@example.com', '1234');

INSERT INTO CUSTOMERS (customer_id, full_name, email, pin_hash) 
VALUES (1002, 'Samantha Reed', 'samantha.reed@example.com', '5678');

INSERT INTO CUSTOMERS (customer_id, full_name, email, pin_hash) 
VALUES (1003, 'David Vance', 'david.vance@example.com', '9999');

INSERT INTO ACCOUNTS (account_no, customer_id, account_type, balance) 
VALUES ('ACC1001', 1001, 'SAVINGS', 2500.00);

INSERT INTO ACCOUNTS (account_no, customer_id, account_type, balance) 
VALUES ('ACC1002', 1002, 'CHECKING', 1200.00);

INSERT INTO ACCOUNTS (account_no, customer_id, account_type, balance) 
VALUES ('ACC1003', 1003, 'SAVINGS', 5000.00);

-- Sample initial transaction history
INSERT INTO TRANSACTIONS (transaction_id, account_no, transaction_type, amount, description) 
VALUES (SEQ_TRANSACTION_ID.NEXTVAL, 'ACC1001', 'DEPOSIT', 2500.00, 'Initial Account Opening Deposit');

INSERT INTO TRANSACTIONS (transaction_id, account_no, transaction_type, amount, description) 
VALUES (SEQ_TRANSACTION_ID.NEXTVAL, 'ACC1002', 'DEPOSIT', 1200.00, 'Initial Account Opening Deposit');

INSERT INTO TRANSACTIONS (transaction_id, account_no, transaction_type, amount, description) 
VALUES (SEQ_TRANSACTION_ID.NEXTVAL, 'ACC1003', 'DEPOSIT', 5000.00, 'Initial Account Opening Deposit');

COMMIT;
