package ui;

import dao.BankDAO;
import model.Account;
import model.Transaction;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;

/**
 * Main Banking Dashboard Panel displaying Account Details, Balance,
 * Recent Transactions JTable, and Navigation for Banking Actions.
 */
public class DashboardFrame extends JFrame {

    private Account currentAccount;
    private BankDAO bankDAO;

    private JLabel lblWelcomeCustomer;
    private JLabel lblAccountNo;
    private JLabel lblAccountType;
    private JLabel lblBalanceAmount;
    private JTable tblTransactions;
    private DefaultTableModel tableModel;

    public DashboardFrame(Account account) {
        this.currentAccount = account;
        this.bankDAO = new BankDAO();
        initUI();
        refreshAccountData();
    }

    private void initUI() {
        setTitle("Apex Trust Bank - Customer Dashboard (" + currentAccount.getAccountNo() + ")");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(920, 680);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(0, 15));
        mainPanel.setBackground(new Color(243, 245, 249));
        mainPanel.setBorder(new EmptyBorder(15, 20, 20, 20));

        // ---------------------------------------------------------------------
        // 1. TOP NAV / HEADER BAR
        // ---------------------------------------------------------------------
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(24, 43, 73)); // Dark Navy
        headerPanel.setBorder(new EmptyBorder(15, 20, 15, 20));

        JPanel userDetailPanel = new JPanel(new GridLayout(2, 1, 0, 3));
        userDetailPanel.setOpaque(false);

        lblWelcomeCustomer = new JLabel("Welcome, " + currentAccount.getCustomerName());
        lblWelcomeCustomer.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblWelcomeCustomer.setForeground(Color.WHITE);

        lblAccountNo = new JLabel("Account No: " + currentAccount.getAccountNo() + "  |  Email: " + currentAccount.getCustomerEmail());
        lblAccountNo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblAccountNo.setForeground(new Color(180, 198, 224));

        userDetailPanel.add(lblWelcomeCustomer);
        userDetailPanel.add(lblAccountNo);

        JPanel headerControls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        headerControls.setOpaque(false);

        lblAccountType = new JLabel(" " + currentAccount.getAccountType() + " ");
        lblAccountType.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblAccountType.setOpaque(true);
        lblAccountType.setBackground(new Color(0, 180, 136)); // Teal Badge
        lblAccountType.setForeground(Color.WHITE);
        lblAccountType.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));

        JButton btnRefresh = new JButton("↻ Refresh");
        btnRefresh.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btnRefresh.setBackground(new Color(45, 65, 95));
        btnRefresh.setForeground(Color.WHITE);
        btnRefresh.setFocusPainted(false);
        btnRefresh.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnRefresh.addActionListener(e -> refreshAccountData());

        JButton btnLogout = new JButton("Logout");
        btnLogout.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnLogout.setBackground(new Color(220, 53, 69));
        btnLogout.setForeground(Color.WHITE);
        btnLogout.setFocusPainted(false);
        btnLogout.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnLogout.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Are you sure you want to log out?", "Logout",
                    JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                new LoginFrame().setVisible(true);
                dispose();
            }
        });

        headerControls.add(lblAccountType);
        headerControls.add(btnRefresh);
        headerControls.add(btnLogout);

        headerPanel.add(userDetailPanel, BorderLayout.WEST);
        headerPanel.add(headerControls, BorderLayout.EAST);

        // ---------------------------------------------------------------------
        // 2. CENTER CONTENT: BALANCE CARD & QUICK ACTIONS
        // ---------------------------------------------------------------------
        JPanel contentPanel = new JPanel(new BorderLayout(0, 15));
        contentPanel.setOpaque(false);

        // Balance Card & Action Buttons Container
        JPanel topCardContainer = new JPanel(new GridLayout(1, 2, 15, 0));
        topCardContainer.setOpaque(false);

        // Balance Display Card
        JPanel balanceCard = new JPanel(new BorderLayout());
        balanceCard.setBackground(Color.WHITE);
        balanceCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 226, 235), 1),
                new EmptyBorder(18, 22, 18, 22)
        ));

        JLabel lblBalanceTitle = new JLabel("AVAILABLE BALANCE");
        lblBalanceTitle.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblBalanceTitle.setForeground(new Color(110, 120, 135));

        lblBalanceAmount = new JLabel(currentAccount.getFormattedBalance());
        lblBalanceAmount.setFont(new Font("Segoe UI", Font.BOLD, 32));
        lblBalanceAmount.setForeground(new Color(18, 120, 70)); // Dark Emerald

        JLabel lblBalanceFooter = new JLabel("Status: ACTIVE  •  Real-time Oracle DB Sync");
        lblBalanceFooter.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblBalanceFooter.setForeground(new Color(140, 150, 165));

        balanceCard.add(lblBalanceTitle, BorderLayout.NORTH);
        balanceCard.add(lblBalanceAmount, BorderLayout.CENTER);
        balanceCard.add(lblBalanceFooter, BorderLayout.SOUTH);

        // Quick Actions Card
        JPanel actionsCard = new JPanel(new GridLayout(1, 3, 10, 0));
        actionsCard.setBackground(Color.WHITE);
        actionsCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 226, 235), 1),
                new EmptyBorder(18, 18, 18, 18)
        ));

        JButton btnDeposit = createActionButton("↓ DEPOSIT", new Color(16, 142, 233));
        JButton btnWithdraw = createActionButton("↑ WITHDRAW", new Color(245, 124, 0));
        JButton btnTransfer = createActionButton("⇄ TRANSFER", new Color(114, 46, 209));

        btnDeposit.addActionListener(e -> openTransactionWindow(TransactionFrame.MODE_DEPOSIT));
        btnWithdraw.addActionListener(e -> openTransactionWindow(TransactionFrame.MODE_WITHDRAW));
        btnTransfer.addActionListener(e -> openTransactionWindow(TransactionFrame.MODE_TRANSFER));

        actionsCard.add(btnDeposit);
        actionsCard.add(btnWithdraw);
        actionsCard.add(btnTransfer);

        topCardContainer.add(balanceCard);
        topCardContainer.add(actionsCard);

        // ---------------------------------------------------------------------
        // 3. RECENT TRANSACTIONS TABLE
        // ---------------------------------------------------------------------
        JPanel tableContainer = new JPanel(new BorderLayout(0, 10));
        tableContainer.setBackground(Color.WHITE);
        tableContainer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 226, 235), 1),
                new EmptyBorder(15, 20, 15, 20)
        ));

        JLabel lblTableTitle = new JLabel("Recent Account Activity");
        lblTableTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblTableTitle.setForeground(new Color(40, 50, 65));

        String[] columns = {"ID", "Date & Time", "Type", "Amount", "Target Account", "Description"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Table is read-only
            }
        };

        tblTransactions = new JTable(tableModel);
        tblTransactions.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tblTransactions.setRowHeight(32);
        tblTransactions.setGridColor(new Color(235, 240, 245));
        tblTransactions.setSelectionBackground(new Color(230, 242, 255));
        tblTransactions.setSelectionForeground(Color.BLACK);

        // Custom Header Styling
        JTableHeader header = tblTransactions.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 13));
        header.setBackground(new Color(240, 243, 248));
        header.setForeground(new Color(60, 70, 85));
        header.setPreferredSize(new Dimension(header.getPreferredSize().width, 35));

        // Alignments & Renderers
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        tblTransactions.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
        tblTransactions.getColumnModel().getColumn(1).setCellRenderer(centerRenderer);
        tblTransactions.getColumnModel().getColumn(2).setCellRenderer(centerRenderer);
        tblTransactions.getColumnModel().getColumn(4).setCellRenderer(centerRenderer);

        // Color renderer for Amount
        tblTransactions.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setHorizontalAlignment(SwingConstants.RIGHT);
                String type = (String) table.getValueAt(row, 2);
                if (!isSelected) {
                    if ("DEPOSIT".equalsIgnoreCase(type)) {
                        c.setForeground(new Color(16, 124, 65)); // Green
                    } else if ("WITHDRAWAL".equalsIgnoreCase(type)) {
                        c.setForeground(new Color(217, 48, 37)); // Red
                    } else {
                        c.setForeground(new Color(24, 119, 242)); // Blue
                    }
                }
                return c;
            }
        });

        JScrollPane scrollPane = new JScrollPane(tblTransactions);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(230, 235, 242)));
        scrollPane.getViewport().setBackground(Color.WHITE);

        tableContainer.add(lblTableTitle, BorderLayout.NORTH);
        tableContainer.add(scrollPane, BorderLayout.CENTER);

        contentPanel.add(topCardContainer, BorderLayout.NORTH);
        contentPanel.add(tableContainer, BorderLayout.CENTER);

        mainPanel.add(headerPanel, BorderLayout.NORTH);
        mainPanel.add(contentPanel, BorderLayout.CENTER);

        add(mainPanel);
    }

    private JButton createActionButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(12, 10, 12, 10));
        return btn;
    }

    /**
     * Refreshes account balance and transaction table from Oracle DB.
     */
    public void refreshAccountData() {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            private Account updatedAccount;
            private List<Transaction> transactions;
            private String errorMsg;

            @Override
            protected Void doInBackground() {
                try {
                    updatedAccount = bankDAO.getAccountDetails(currentAccount.getAccountNo());
                    transactions = bankDAO.getRecentTransactions(currentAccount.getAccountNo(), 25);
                } catch (SQLException e) {
                    errorMsg = e.getMessage();
                }
                return null;
            }

            @Override
            protected void done() {
                if (updatedAccount != null) {
                    currentAccount = updatedAccount;
                    lblWelcomeCustomer.setText("Welcome, " + currentAccount.getCustomerName());
                    lblBalanceAmount.setText(currentAccount.getFormattedBalance());
                }

                if (transactions != null) {
                    tableModel.setRowCount(0);
                    for (Transaction t : transactions) {
                        tableModel.addRow(new Object[]{
                                t.getTransactionId(),
                                t.getFormattedTimestamp(),
                                t.getTransactionType(),
                                t.getFormattedAmount(),
                                t.getTargetAccountNo() != null ? t.getTargetAccountNo() : "-",
                                t.getDescription() != null ? t.getDescription() : ""
                        });
                    }
                }

                if (errorMsg != null) {
                    JOptionPane.showMessageDialog(DashboardFrame.this,
                            "Failed to refresh data from database:\n" + errorMsg,
                            "Database Sync Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void openTransactionWindow(String initialMode) {
        TransactionFrame frame = new TransactionFrame(this, currentAccount, bankDAO, initialMode);
        frame.setVisible(true);
    }

    public Account getCurrentAccount() {
        return currentAccount;
    }
}
