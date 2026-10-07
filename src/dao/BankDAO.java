package dao;

import config.DatabaseConnection;
import model.Account;
import model.Transaction;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object (DAO) providing core banking operations and JDBC transaction handling.
 */
public class BankDAO {

    /**
     * Authenticate customer credentials by matching Account Number and PIN.
     * @param accountNo Account Number
     * @param pin Plain text PIN
     * @return Account object if authentication succeeds, null otherwise.
     * @throws SQLException if database error occurs
     */
    public Account authenticateCustomer(String accountNo, String pin) throws SQLException {
        String sql = "SELECT a.account_no, a.customer_id, c.full_name, c.email, a.account_type, " +
                     "a.balance, a.status, a.created_at " +
                     "FROM ACCOUNTS a " +
                     "JOIN CUSTOMERS c ON a.customer_id = c.customer_id " +
                     "WHERE UPPER(a.account_no) = UPPER(?) AND c.pin_hash = ? AND a.status = 'ACTIVE'";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, accountNo.trim());
            stmt.setString(2, pin.trim());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return extractAccountFromResultSet(rs);
                }
            }
        }
        return null;
    }

    /**
     * Retrieve full Account details by Account Number.
     */
    public Account getAccountDetails(String accountNo) throws SQLException {
        String sql = "SELECT a.account_no, a.customer_id, c.full_name, c.email, a.account_type, " +
                     "a.balance, a.status, a.created_at " +
                     "FROM ACCOUNTS a " +
                     "JOIN CUSTOMERS c ON a.customer_id = c.customer_id " +
                     "WHERE UPPER(a.account_no) = UPPER(?)";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, accountNo.trim());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return extractAccountFromResultSet(rs);
                }
            }
        }
        return null;
    }

    /**
     * Fetch recent transaction history for an account up to a specified limit.
     */
    public List<Transaction> getRecentTransactions(String accountNo, int limit) throws SQLException {
        List<Transaction> transactions = new ArrayList<>();
        // Query recent transactions involving this account (either primary or target)
        String sql = "SELECT * FROM (" +
                     "   SELECT transaction_id, account_no, transaction_type, amount, target_account_no, description, transaction_date " +
                     "   FROM TRANSACTIONS " +
                     "   WHERE UPPER(account_no) = UPPER(?) OR UPPER(target_account_no) = UPPER(?) " +
                     "   ORDER BY transaction_date DESC" +
                     ") WHERE ROWNUM <= ?";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, accountNo.trim());
            stmt.setString(2, accountNo.trim());
            stmt.setInt(3, limit > 0 ? limit : 20);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Transaction t = new Transaction();
                    t.setTransactionId(rs.getLong("transaction_id"));
                    t.setAccountNo(rs.getString("account_no"));
                    t.setTransactionType(rs.getString("transaction_type"));
                    t.setAmount(rs.getBigDecimal("amount"));
                    t.setTargetAccountNo(rs.getString("target_account_no"));
                    t.setDescription(rs.getString("description"));
                    t.setTimestamp(rs.getTimestamp("transaction_date"));
                    transactions.add(t);
                }
            }
        }
        return transactions;
    }

    /**
     * Deposit funds into an account.
     * @param accountNo Account Number
     * @param amount Deposit amount
     * @return true if successful
     * @throws SQLException on database failure or invalid inputs
     */
    public boolean deposit(String accountNo, double amount) throws SQLException {
        if (amount <= 0) {
            throw new IllegalArgumentException("Deposit amount must be greater than zero.");
        }

        Connection conn = null;
        boolean autoCommitState = true;

        try {
            conn = DatabaseConnection.getInstance().getConnection();
            autoCommitState = conn.getAutoCommit();
            conn.setAutoCommit(false);

            // 1. Lock and verify account exists
            Account acc = getAccountForUpdate(conn, accountNo);
            if (acc == null) {
                throw new SQLException("Account number '" + accountNo + "' not found.");
            }

            // 2. Update Balance
            String updateSql = "UPDATE ACCOUNTS SET balance = balance + ? WHERE UPPER(account_no) = UPPER(?)";
            try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                updateStmt.setBigDecimal(1, BigDecimal.valueOf(amount));
                updateStmt.setString(2, accountNo.trim());
                updateStmt.executeUpdate();
            }

            // 3. Log Transaction
            String logSql = "INSERT INTO TRANSACTIONS (transaction_id, account_no, transaction_type, amount, description) " +
                           "VALUES (SEQ_TRANSACTION_ID.NEXTVAL, UPPER(?), 'DEPOSIT', ?, ?)";
            try (PreparedStatement logStmt = conn.prepareStatement(logSql)) {
                logStmt.setString(1, accountNo.trim());
                logStmt.setBigDecimal(2, BigDecimal.valueOf(amount));
                logStmt.setString(3, "Cash/Direct Deposit");
                logStmt.executeUpdate();
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { e.addSuppressed(ex); }
            }
            throw e;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(autoCommitState); } catch (SQLException ignored) {}
            }
        }
    }

    /**
     * Withdraw funds from an account.
     * @param accountNo Account Number
     * @param amount Withdrawal amount
     * @return true if successful
     * @throws SQLException if balance insufficient or database fails
     */
    public boolean withdraw(String accountNo, double amount) throws SQLException {
        if (amount <= 0) {
            throw new IllegalArgumentException("Withdrawal amount must be greater than zero.");
        }

        Connection conn = null;
        boolean autoCommitState = true;

        try {
            conn = DatabaseConnection.getInstance().getConnection();
            autoCommitState = conn.getAutoCommit();
            conn.setAutoCommit(false);

            // 1. Lock and check balance
            Account acc = getAccountForUpdate(conn, accountNo);
            if (acc == null) {
                throw new SQLException("Account number '" + accountNo + "' not found.");
            }

            BigDecimal withdrawAmt = BigDecimal.valueOf(amount);
            if (acc.getBalance().compareTo(withdrawAmt) < 0) {
                throw new SQLException("Insufficient funds! Current Balance: $" + acc.getBalance());
            }

            // 2. Update Balance
            String updateSql = "UPDATE ACCOUNTS SET balance = balance - ? WHERE UPPER(account_no) = UPPER(?)";
            try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                updateStmt.setBigDecimal(1, withdrawAmt);
                updateStmt.setString(2, accountNo.trim());
                updateStmt.executeUpdate();
            }

            // 3. Log Transaction
            String logSql = "INSERT INTO TRANSACTIONS (transaction_id, account_no, transaction_type, amount, description) " +
                           "VALUES (SEQ_TRANSACTION_ID.NEXTVAL, UPPER(?), 'WITHDRAWAL', ?, ?)";
            try (PreparedStatement logStmt = conn.prepareStatement(logSql)) {
                logStmt.setString(1, accountNo.trim());
                logStmt.setBigDecimal(2, withdrawAmt);
                logStmt.setString(3, "ATM/Cash Withdrawal");
                logStmt.executeUpdate();
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { e.addSuppressed(ex); }
            }
            throw e;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(autoCommitState); } catch (SQLException ignored) {}
            }
        }
    }

    /**
     * Transfer money between two accounts with STRICT ACID compliance using JDBC transactions.
     * Enforces: setAutoCommit(false), Balance checks, Update both accounts, Double Transaction logs, Commit/Rollback.
     */
    public boolean transferMoney(String senderAccountNo, String receiverAccountNo, double amount) throws SQLException {
        if (senderAccountNo == null || receiverAccountNo == null) {
            throw new IllegalArgumentException("Sender and Receiver accounts must not be null.");
        }
        if (senderAccountNo.trim().equalsIgnoreCase(receiverAccountNo.trim())) {
            throw new IllegalArgumentException("Cannot transfer money to the same account.");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("Transfer amount must be greater than zero.");
        }

        Connection conn = null;
        boolean autoCommitState = true;

        try {
            conn = DatabaseConnection.getInstance().getConnection();
            autoCommitState = conn.getAutoCommit();

            // STRICT ACID STEP 1: Disable auto-commit
            conn.setAutoCommit(false);

            // Order locks by account string comparison to prevent database deadlocks
            String firstLock = senderAccountNo.compareToIgnoreCase(receiverAccountNo) < 0 ? senderAccountNo : receiverAccountNo;
            String secondLock = senderAccountNo.compareToIgnoreCase(receiverAccountNo) < 0 ? receiverAccountNo : senderAccountNo;

            getAccountForUpdate(conn, firstLock);
            getAccountForUpdate(conn, secondLock);

            // Verify Sender
            Account sender = getAccountForUpdate(conn, senderAccountNo);
            if (sender == null) {
                throw new SQLException("Sender account '" + senderAccountNo + "' does not exist.");
            }

            // Verify Receiver
            Account receiver = getAccountForUpdate(conn, receiverAccountNo);
            if (receiver == null) {
                throw new SQLException("Receiver account '" + receiverAccountNo + "' does not exist.");
            }

            if (!"ACTIVE".equalsIgnoreCase(receiver.getStatus())) {
                throw new SQLException("Receiver account is inactive or frozen.");
            }

            BigDecimal transferAmt = BigDecimal.valueOf(amount);

            // Check sender funds
            if (sender.getBalance().compareTo(transferAmt) < 0) {
                throw new SQLException(String.format("Insufficient funds for transfer. Available: $%,.2f, Required: $%,.2f",
                        sender.getBalance().doubleValue(), amount));
            }

            // STRICT ACID STEP 2: Deduct from sender
            String deductSql = "UPDATE ACCOUNTS SET balance = balance - ? WHERE UPPER(account_no) = UPPER(?)";
            try (PreparedStatement deductStmt = conn.prepareStatement(deductSql)) {
                deductStmt.setBigDecimal(1, transferAmt);
                deductStmt.setString(2, senderAccountNo.trim());
                int rows = deductStmt.executeUpdate();
                if (rows != 1) {
                    throw new SQLException("Failed to update sender balance.");
                }
            }

            // STRICT ACID STEP 3: Add to receiver
            String creditSql = "UPDATE ACCOUNTS SET balance = balance + ? WHERE UPPER(account_no) = UPPER(?)";
            try (PreparedStatement creditStmt = conn.prepareStatement(creditSql)) {
                creditStmt.setBigDecimal(1, transferAmt);
                creditStmt.setString(2, receiverAccountNo.trim());
                int rows = creditStmt.executeUpdate();
                if (rows != 1) {
                    throw new SQLException("Failed to update receiver balance.");
                }
            }

            // STRICT ACID STEP 4: Insert transaction log for Sender
            String senderLogSql = "INSERT INTO TRANSACTIONS (transaction_id, account_no, transaction_type, amount, target_account_no, description) " +
                                 "VALUES (SEQ_TRANSACTION_ID.NEXTVAL, UPPER(?), 'TRANSFER', ?, UPPER(?), ?)";
            try (PreparedStatement logStmt = conn.prepareStatement(senderLogSql)) {
                logStmt.setString(1, senderAccountNo.trim());
                logStmt.setBigDecimal(2, transferAmt);
                logStmt.setString(3, receiverAccountNo.trim());
                logStmt.setString(4, "Transfer OUT to " + receiverAccountNo.trim());
                logStmt.executeUpdate();
            }

            // STRICT ACID STEP 5: Insert transaction log for Receiver
            String receiverLogSql = "INSERT INTO TRANSACTIONS (transaction_id, account_no, transaction_type, amount, target_account_no, description) " +
                                   "VALUES (SEQ_TRANSACTION_ID.NEXTVAL, UPPER(?), 'TRANSFER', ?, UPPER(?), ?)";
            try (PreparedStatement logStmt = conn.prepareStatement(receiverLogSql)) {
                logStmt.setString(1, receiverAccountNo.trim());
                logStmt.setBigDecimal(2, transferAmt);
                logStmt.setString(3, senderAccountNo.trim());
                logStmt.setString(4, "Transfer IN from " + senderAccountNo.trim());
                logStmt.executeUpdate();
            }

            // STRICT ACID STEP 6: Commit all changes
            conn.commit();
            return true;

        } catch (SQLException e) {
            // STRICT ACID STEP 7: Rollback on any failure
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    e.addSuppressed(rollbackEx);
                }
            }
            throw e;
        } finally {
            // Restore auto-commit state
            if (conn != null) {
                try {
                    conn.setAutoCommit(autoCommitState);
                } catch (SQLException ignored) {}
            }
        }
    }

    /**
     * Helper to lock and extract row FOR UPDATE
     */
    private Account getAccountForUpdate(Connection conn, String accountNo) throws SQLException {
        String sql = "SELECT a.account_no, a.customer_id, c.full_name, c.email, a.account_type, " +
                     "a.balance, a.status, a.created_at " +
                     "FROM ACCOUNTS a " +
                     "JOIN CUSTOMERS c ON a.customer_id = c.customer_id " +
                     "WHERE UPPER(a.account_no) = UPPER(?) FOR UPDATE";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, accountNo.trim());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return extractAccountFromResultSet(rs);
                }
            }
        }
        return null;
    }

    private Account extractAccountFromResultSet(ResultSet rs) throws SQLException {
        Account acc = new Account();
        acc.setAccountNo(rs.getString("account_no"));
        acc.setCustomerId(rs.getInt("customer_id"));
        acc.setCustomerName(rs.getString("full_name"));
        acc.setCustomerEmail(rs.getString("email"));
        acc.setAccountType(rs.getString("account_type"));
        acc.setBalance(rs.getBigDecimal("balance"));
        acc.setStatus(rs.getString("status"));
        acc.setCreatedAt(rs.getTimestamp("created_at"));
        return acc;
    }
}
