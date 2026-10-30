package test;

import config.DatabaseConnection;
import dao.BankDAO;
import model.Account;
import model.Transaction;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

public class IntegrationTest {

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("  STARTING AUTOMATED BANKING SYSTEM TEST SUITE   ");
        System.out.println("==================================================");

        int passed = 0;
        int failed = 0;

        try {
            // Ensure Embedded Mode is clean and active
            DatabaseConnection.switchToEmbeddedMode();
            DatabaseConnection.resetSampleData();
            BankDAO dao = new BankDAO();

            // Test 1: Authenticate valid account
            Account acc1 = dao.authenticateCustomer("ACC1001", "1234");
            if (acc1 != null && "Alex Morgan".equals(acc1.getCustomerName()) && acc1.getBalance().compareTo(new BigDecimal("2500.00")) == 0) {
                System.out.println("PASS: Test 1 - Valid Authentication (ACC1001, PIN 1234)");
                passed++;
            } else {
                System.err.println("FAIL: Test 1 - Valid Authentication failed: " + acc1);
                failed++;
            }

            // Test 2: Authenticate invalid PIN
            Account accBadPin = dao.authenticateCustomer("ACC1001", "wrong");
            if (accBadPin == null) {
                System.out.println("PASS: Test 2 - Invalid PIN correctly rejected");
                passed++;
            } else {
                System.err.println("FAIL: Test 2 - Invalid PIN was accepted!");
                failed++;
            }

            // Test 3: Authenticate non-existent account
            Account accBadNo = dao.authenticateCustomer("ACC9999", "1234");
            if (accBadNo == null) {
                System.out.println("PASS: Test 3 - Non-existent Account correctly rejected");
                passed++;
            } else {
                System.err.println("FAIL: Test 3 - Non-existent Account was accepted!");
                failed++;
            }

            // Test 4: Deposit operation
            boolean depSuccess = dao.deposit("ACC1001", 500.00);
            Account accAfterDep = dao.getAccountDetails("ACC1001");
            if (depSuccess && accAfterDep.getBalance().compareTo(new BigDecimal("3000.00")) == 0) {
                System.out.println("PASS: Test 4 - Deposit $500.00 (Balance updated: $2500 -> $3000)");
                passed++;
            } else {
                System.err.println("FAIL: Test 4 - Deposit failed: Balance=" + accAfterDep.getBalance());
                failed++;
            }

            // Test 5: Negative Deposit rejected
            try {
                dao.deposit("ACC1001", -50.00);
                System.err.println("FAIL: Test 5 - Negative deposit was accepted!");
                failed++;
            } catch (IllegalArgumentException e) {
                System.out.println("PASS: Test 5 - Negative deposit correctly rejected");
                passed++;
            }

            // Test 6: Withdrawal operation
            boolean withSuccess = dao.withdraw("ACC1001", 200.00);
            Account accAfterWith = dao.getAccountDetails("ACC1001");
            if (withSuccess && accAfterWith.getBalance().compareTo(new BigDecimal("2800.00")) == 0) {
                System.out.println("PASS: Test 6 - Withdraw $200.00 (Balance updated: $3000 -> $2800)");
                passed++;
            } else {
                System.err.println("FAIL: Test 6 - Withdraw failed: Balance=" + accAfterWith.getBalance());
                failed++;
            }

            // Test 7: Overdraft rejected (insufficient funds)
            try {
                dao.withdraw("ACC1001", 99999.00);
                System.err.println("FAIL: Test 7 - Overdraft was accepted!");
                failed++;
            } catch (SQLException e) {
                System.out.println("PASS: Test 7 - Overdraft correctly prevented with Insufficient Funds error: " + e.getMessage());
                passed++;
            }

            // Test 8: Transfer operation (ACC1001 -> ACC1002, $300)
            Account acc2Before = dao.getAccountDetails("ACC1002");
            boolean transferSuccess = dao.transferMoney("ACC1001", "ACC1002", 300.00);
            Account acc1AfterTransfer = dao.getAccountDetails("ACC1001");
            Account acc2AfterTransfer = dao.getAccountDetails("ACC1002");

            boolean senderCorrect = acc1AfterTransfer.getBalance().compareTo(new BigDecimal("2500.00")) == 0;
            boolean receiverCorrect = acc2AfterTransfer.getBalance().compareTo(new BigDecimal("1500.00")) == 0;

            if (transferSuccess && senderCorrect && receiverCorrect) {
                System.out.println("PASS: Test 8 - Transfer $300.00 from ACC1001 to ACC1002 (Balances: 2800->2500, 1200->1500)");
                passed++;
            } else {
                System.err.println("FAIL: Test 8 - Transfer failed: Sender=" + acc1AfterTransfer.getBalance() + ", Receiver=" + acc2AfterTransfer.getBalance());
                failed++;
            }

            // Test 9: Self transfer rejected
            try {
                dao.transferMoney("ACC1001", "ACC1001", 50.00);
                System.err.println("FAIL: Test 9 - Self transfer was accepted!");
                failed++;
            } catch (IllegalArgumentException e) {
                System.out.println("PASS: Test 9 - Transfer to self correctly rejected");
                passed++;
            }

            // Test 10: Transfer to non-existent account
            try {
                dao.transferMoney("ACC1001", "ACC9999", 50.00);
                System.err.println("FAIL: Test 10 - Transfer to ghost account accepted!");
                failed++;
            } catch (SQLException e) {
                System.out.println("PASS: Test 10 - Transfer to non-existent account failed safely with rollback");
                passed++;
            }

            // Test 11: Transaction History log
            List<Transaction> txns = dao.getRecentTransactions("ACC1001", 10);
            if (txns != null && txns.size() >= 4) {
                System.out.println("PASS: Test 11 - Transaction audit history retrieved (" + txns.size() + " records found)");
                passed++;
            } else {
                System.err.println("FAIL: Test 11 - Transaction history count mismatch: " + (txns == null ? 0 : txns.size()));
                failed++;
            }

            // Test 12: Reset sample data
            DatabaseConnection.resetSampleData();
            Account accReset = dao.getAccountDetails("ACC1001");
            if (accReset != null && accReset.getBalance().compareTo(new BigDecimal("2500.00")) == 0) {
                System.out.println("PASS: Test 12 - Reset sample data restores exact opening state ($2,500.00)");
                passed++;
            } else {
                System.err.println("FAIL: Test 12 - Reset sample data failed");
                failed++;
            }

        } catch (Exception e) {
            e.printStackTrace();
            failed++;
        }

        System.out.println("==================================================");
        System.out.println("TEST RESULTS: " + passed + " PASSED, " + failed + " FAILED");
        System.out.println("==================================================");

        if (failed > 0) {
            System.exit(1);
        }
    }
}
