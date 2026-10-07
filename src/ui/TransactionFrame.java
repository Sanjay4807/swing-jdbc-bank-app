package ui;

import dao.BankDAO;
import model.Account;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Modal Dialog for handling Banking Transactions (Deposit, Withdrawal, Transfer).
 */
public class TransactionFrame extends JDialog {

    public static final String MODE_DEPOSIT = "DEPOSIT";
    public static final String MODE_WITHDRAW = "WITHDRAWAL";
    public static final String MODE_TRANSFER = "TRANSFER";

    private DashboardFrame parentDashboard;
    private Account account;
    private BankDAO bankDAO;

    private JComboBox<String> cbTransactionType;
    private JTextField txtAmount;
    private JTextField txtTargetAccount;
    private JLabel lblTargetAccount;
    private JLabel lblAvailableBalance;
    private JButton btnSubmit;
    private JButton btnCancel;

    public TransactionFrame(DashboardFrame parent, Account account, BankDAO bankDAO, String initialMode) {
        super(parent, "Banking Transaction", true);
        this.parentDashboard = parent;
        this.account = account;
        this.bankDAO = bankDAO;

        initUI(initialMode);
    }

    private void initUI(String initialMode) {
        setSize(440, 420);
        setLocationRelativeTo(parentDashboard);
        setResizable(false);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(new Color(245, 247, 250));

        // Header Panel
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(24, 43, 73));
        headerPanel.setBorder(new EmptyBorder(15, 20, 15, 20));

        JLabel lblTitle = new JLabel("EXECUTE TRANSACTION");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTitle.setForeground(Color.WHITE);

        JLabel lblSub = new JLabel("Account No: " + account.getAccountNo());
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSub.setForeground(new Color(180, 198, 224));

        headerPanel.add(lblTitle, BorderLayout.NORTH);
        headerPanel.add(lblSub, BorderLayout.SOUTH);

        // Form Panel
        JPanel formPanel = new JPanel(null);
        formPanel.setBackground(Color.WHITE);
        formPanel.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));

        // Transaction Type
        JLabel lblType = new JLabel("Transaction Type:");
        lblType.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblType.setBounds(25, 15, 300, 20);
        formPanel.add(lblType);

        cbTransactionType = new JComboBox<>(new String[]{MODE_DEPOSIT, MODE_WITHDRAW, MODE_TRANSFER});
        cbTransactionType.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cbTransactionType.setBounds(25, 38, 370, 35);
        cbTransactionType.setSelectedItem(initialMode);
        cbTransactionType.addActionListener(e -> updateFormState());
        formPanel.add(cbTransactionType);

        // Available Balance Info
        lblAvailableBalance = new JLabel("Available Balance: " + account.getFormattedBalance());
        lblAvailableBalance.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblAvailableBalance.setForeground(new Color(18, 120, 70));
        lblAvailableBalance.setBounds(25, 80, 370, 20);
        formPanel.add(lblAvailableBalance);

        // Amount Field
        JLabel lblAmount = new JLabel("Amount ($):");
        lblAmount.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblAmount.setBounds(25, 108, 300, 20);
        formPanel.add(lblAmount);

        txtAmount = new JTextField();
        txtAmount.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtAmount.setBounds(25, 130, 370, 35);
        txtAmount.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 208, 220)),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        formPanel.add(txtAmount);

        // Target Account Field (For Transfers)
        lblTargetAccount = new JLabel("Recipient Account Number:");
        lblTargetAccount.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblTargetAccount.setBounds(25, 175, 300, 20);
        formPanel.add(lblTargetAccount);

        txtTargetAccount = new JTextField();
        txtTargetAccount.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtTargetAccount.setBounds(25, 198, 370, 35);
        txtTargetAccount.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 208, 220)),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        formPanel.add(txtTargetAccount);

        // Buttons Panel
        btnSubmit = new JButton("Confirm Transaction");
        btnSubmit.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnSubmit.setBackground(new Color(24, 119, 242));
        btnSubmit.setForeground(Color.WHITE);
        btnSubmit.setFocusPainted(false);
        btnSubmit.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSubmit.setBounds(25, 255, 230, 40);
        btnSubmit.addActionListener(e -> processTransaction());

        btnCancel = new JButton("Cancel");
        btnCancel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btnCancel.setBackground(new Color(220, 225, 230));
        btnCancel.setForeground(new Color(60, 70, 85));
        btnCancel.setFocusPainted(false);
        btnCancel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnCancel.setBounds(265, 255, 130, 40);
        btnCancel.addActionListener(e -> dispose());

        formPanel.add(btnSubmit);
        formPanel.add(btnCancel);

        mainPanel.add(headerPanel, BorderLayout.NORTH);
        mainPanel.add(formPanel, BorderLayout.CENTER);

        add(mainPanel);

        updateFormState();
    }

    private void updateFormState() {
        String mode = (String) cbTransactionType.getSelectedItem();
        boolean isTransfer = MODE_TRANSFER.equalsIgnoreCase(mode);

        lblTargetAccount.setVisible(isTransfer);
        txtTargetAccount.setVisible(isTransfer);

        if (MODE_DEPOSIT.equalsIgnoreCase(mode)) {
            btnSubmit.setText("Confirm Deposit");
            btnSubmit.setBackground(new Color(16, 142, 233));
        } else if (MODE_WITHDRAW.equalsIgnoreCase(mode)) {
            btnSubmit.setText("Confirm Withdrawal");
            btnSubmit.setBackground(new Color(245, 124, 0));
        } else {
            btnSubmit.setText("Confirm Transfer");
            btnSubmit.setBackground(new Color(114, 46, 209));
        }

        revalidate();
        repaint();
    }

    private void processTransaction() {
        String mode = (String) cbTransactionType.getSelectedItem();
        String amountText = txtAmount.getText().trim();

        if (amountText.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter an amount.",
                    "Input Error", JOptionPane.WARNING_MESSAGE);
            txtAmount.requestFocus();
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountText);
            if (amount <= 0) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Please enter a valid positive numerical amount (e.g. 100.00).",
                    "Invalid Amount", JOptionPane.ERROR_MESSAGE);
            txtAmount.requestFocus();
            return;
        }

        String targetAcc = txtTargetAccount.getText().trim();
        if (MODE_TRANSFER.equalsIgnoreCase(mode)) {
            if (targetAcc.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter the Recipient Account Number.",
                        "Input Error", JOptionPane.WARNING_MESSAGE);
                txtTargetAccount.requestFocus();
                return;
            }
            if (targetAcc.equalsIgnoreCase(account.getAccountNo())) {
                JOptionPane.showMessageDialog(this, "Target account cannot be your own account.",
                        "Validation Error", JOptionPane.WARNING_MESSAGE);
                txtTargetAccount.requestFocus();
                return;
            }
        }

        btnSubmit.setEnabled(false);

        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            private String failureMessage = null;

            @Override
            protected Boolean doInBackground() {
                try {
                    if (MODE_DEPOSIT.equalsIgnoreCase(mode)) {
                        return bankDAO.deposit(account.getAccountNo(), amount);
                    } else if (MODE_WITHDRAW.equalsIgnoreCase(mode)) {
                        return bankDAO.withdraw(account.getAccountNo(), amount);
                    } else if (MODE_TRANSFER.equalsIgnoreCase(mode)) {
                        return bankDAO.transferMoney(account.getAccountNo(), targetAcc, amount);
                    }
                } catch (Exception ex) {
                    failureMessage = ex.getMessage();
                }
                return false;
            }

            @Override
            protected void done() {
                btnSubmit.setEnabled(true);
                try {
                    boolean success = get();
                    if (success) {
                        String successMsg = String.format("Transaction Successful!\nType: %s\nAmount: $%,.2f", mode, amount);
                        if (MODE_TRANSFER.equalsIgnoreCase(mode)) {
                            successMsg += "\nRecipient: " + targetAcc;
                        }
                        JOptionPane.showMessageDialog(TransactionFrame.this, successMsg,
                                "Transaction Complete", JOptionPane.INFORMATION_MESSAGE);

                        // Refresh parent dashboard
                        parentDashboard.refreshAccountData();
                        dispose();
                    } else {
                        String err = failureMessage != null ? failureMessage : "Transaction failed. Please check details and try again.";
                        JOptionPane.showMessageDialog(TransactionFrame.this, err,
                                "Transaction Failed", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(TransactionFrame.this,
                            "An unexpected error occurred: " + ex.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };

        worker.execute();
    }
}
