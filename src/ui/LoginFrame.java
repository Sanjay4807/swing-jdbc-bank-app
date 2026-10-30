package ui;

import config.DatabaseConnection;
import dao.BankDAO;
import model.Account;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Modern Swing Login Window for Mini Banking System.
 */
public class LoginFrame extends JFrame {

    private JTextField txtAccountNo;
    private JPasswordField txtPin;
    private JButton btnLogin;
    private JButton btnDbConfig;
    private JLabel lblStatus;
    private BankDAO bankDAO;

    public LoginFrame() {
        bankDAO = new BankDAO();
        initUI();
    }

    private void initUI() {
        setTitle("Apex Trust Bank - Secure Portal");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(500, 620);
        setLocationRelativeTo(null);
        setResizable(false);

        // Main Background Panel
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());
        mainPanel.setBackground(new Color(245, 247, 250));

        // Header Panel with Gradient-like Dark Theme
        JPanel headerPanel = new JPanel();
        headerPanel.setBackground(new Color(24, 43, 73)); // Deep Navy
        headerPanel.setPreferredSize(new Dimension(500, 130));
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setBorder(new EmptyBorder(25, 20, 20, 20));

        JLabel lblBankTitle = new JLabel("APEX TRUST BANK");
        lblBankTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblBankTitle.setForeground(Color.WHITE);
        lblBankTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblSubTitle = new JLabel("Digital Banking System • Java Swing & JDBC");
        lblSubTitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblSubTitle.setForeground(new Color(180, 198, 224));
        lblSubTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        headerPanel.add(lblBankTitle);
        headerPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        headerPanel.add(lblSubTitle);

        // Card Panel for Form Inputs
        JPanel cardPanel = new JPanel();
        cardPanel.setLayout(null);
        cardPanel.setBackground(Color.WHITE);
        cardPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(225, 230, 238), 1),
                new EmptyBorder(25, 30, 25, 30)
        ));

        // Form Title
        JLabel lblLoginHeader = new JLabel("Sign In to Your Account");
        lblLoginHeader.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblLoginHeader.setForeground(new Color(40, 50, 65));
        lblLoginHeader.setBounds(40, 15, 300, 25);
        cardPanel.add(lblLoginHeader);

        // Account Number Label & Field
        JLabel lblAccountNo = new JLabel("Account Number");
        lblAccountNo.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblAccountNo.setForeground(new Color(90, 100, 115));
        lblAccountNo.setBounds(40, 55, 300, 20);
        cardPanel.add(lblAccountNo);

        txtAccountNo = new JTextField();
        txtAccountNo.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtAccountNo.setBounds(40, 78, 360, 38);
        txtAccountNo.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 208, 220)),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        txtAccountNo.setText("ACC1001"); // Sample default for easy testing
        cardPanel.add(txtAccountNo);

        // PIN Label & Field
        JLabel lblPin = new JLabel("PIN / Password");
        lblPin.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblPin.setForeground(new Color(90, 100, 115));
        lblPin.setBounds(40, 130, 300, 20);
        cardPanel.add(lblPin);

        txtPin = new JPasswordField();
        txtPin.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtPin.setBounds(40, 153, 360, 38);
        txtPin.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 208, 220)),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        txtPin.setText("1234"); // Sample default for easy testing
        cardPanel.add(txtPin);

        // Status / Error Label
        lblStatus = new JLabel("");
        lblStatus.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblStatus.setForeground(new Color(220, 53, 69));
        lblStatus.setBounds(40, 198, 360, 20);
        cardPanel.add(lblStatus);

        // Login Button
        btnLogin = new JButton("LOGIN SECURELY");
        btnLogin.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnLogin.setBackground(new Color(24, 119, 242)); // Modern Blue
        btnLogin.setForeground(Color.WHITE);
        btnLogin.setFocusPainted(false);
        btnLogin.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnLogin.setBounds(40, 228, 360, 42);
        btnLogin.addActionListener(e -> performLogin());
        cardPanel.add(btnLogin);

        // Database Configuration Link Button
        btnDbConfig = new JButton();
        updateDbConfigButtonText();
        btnDbConfig.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnDbConfig.setForeground(new Color(75, 85, 105));
        btnDbConfig.setContentAreaFilled(false);
        btnDbConfig.setBorderPainted(false);
        btnDbConfig.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnDbConfig.setBounds(40, 280, 360, 30);
        btnDbConfig.addActionListener(e -> showDbConfigDialog());
        cardPanel.add(btnDbConfig);

        // Footer info label
        JLabel lblFooter = new JLabel("Sample Accounts: ACC1001 (PIN: 1234) | ACC1002 (PIN: 5678) | ACC1003 (PIN: 9999)");
        lblFooter.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblFooter.setForeground(new Color(110, 120, 135));
        lblFooter.setHorizontalAlignment(SwingConstants.CENTER);
        lblFooter.setBorder(new EmptyBorder(10, 10, 15, 10));

        // Assemble Panels
        mainPanel.add(headerPanel, BorderLayout.NORTH);

        JPanel wrapperPanel = new JPanel(new GridBagLayout());
        wrapperPanel.setBackground(new Color(245, 247, 250));
        wrapperPanel.add(cardPanel);
        cardPanel.setPreferredSize(new Dimension(440, 340));

        mainPanel.add(wrapperPanel, BorderLayout.CENTER);
        mainPanel.add(lblFooter, BorderLayout.SOUTH);

        add(mainPanel);

        // Enter key action for text fields
        ActionListener loginAction = e -> performLogin();
        txtAccountNo.addActionListener(loginAction);
        txtPin.addActionListener(loginAction);
    }

    private void updateDbConfigButtonText() {
        btnDbConfig.setText("⚙ Database: " + DatabaseConnection.getStatusSummary());
    }

    private void performLogin() {
        String accountNo = txtAccountNo.getText().trim();
        String pin = new String(txtPin.getPassword()).trim();

        if (accountNo.isEmpty() || pin.isEmpty()) {
            lblStatus.setText("Please enter both Account Number and PIN.");
            return;
        }

        lblStatus.setForeground(new Color(24, 119, 242));
        lblStatus.setText("Authenticating with Database...");
        btnLogin.setEnabled(false);

        // Execute DB check on background thread to keep UI responsive
        SwingWorker<Account, Void> worker = new SwingWorker<Account, Void>() {
            private String errorMessage = null;

            @Override
            protected Account doInBackground() {
                try {
                    return bankDAO.authenticateCustomer(accountNo, pin);
                } catch (SQLException ex) {
                    errorMessage = "Database Error: " + ex.getMessage();
                    return null;
                }
            }

            @Override
            protected void done() {
                btnLogin.setEnabled(true);
                try {
                    Account account = get();
                    if (account != null) {
                        lblStatus.setText("");
                        // Open Dashboard
                        DashboardFrame dashboard = new DashboardFrame(account);
                        dashboard.setVisible(true);
                        dispose(); // Close login window
                    } else {
                        lblStatus.setForeground(new Color(220, 53, 69));
                        if (errorMessage != null) {
                            lblStatus.setText(errorMessage);
                            JOptionPane.showMessageDialog(LoginFrame.this,
                                    errorMessage + "\n\nTip: Click 'Database Settings' to verify your connection.",
                                    "Database Connection Error", JOptionPane.ERROR_MESSAGE);
                        } else {
                            lblStatus.setText("Invalid Account Number or PIN.");
                        }
                    }
                } catch (Exception ex) {
                    lblStatus.setForeground(new Color(220, 53, 69));
                    lblStatus.setText("Error during login.");
                }
            }
        };
        worker.execute();
    }

    private void showDbConfigDialog() {
        JDialog dialog = new JDialog(this, "Database Configuration & Viva Settings", true);
        dialog.setSize(520, 420);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout(10, 10));

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(new EmptyBorder(15, 20, 15, 20));

        JLabel lblHeading = new JLabel("Database Engine Selection");
        lblHeading.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblHeading.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(lblHeading);

        content.add(Box.createRigidArea(new Dimension(0, 10)));

        JRadioButton rbEmbedded = new JRadioButton("Embedded Local Database (H2 Oracle Mode - Zero Setup / Viva Ready)");
        JRadioButton rbOracle = new JRadioButton("Oracle Database (XE / 19c / 21c Remote or Local Listener)");
        ButtonGroup group = new ButtonGroup();
        group.add(rbEmbedded);
        group.add(rbOracle);

        if (DatabaseConnection.getCurrentMode() == DatabaseConnection.DatabaseMode.EMBEDDED) {
            rbEmbedded.setSelected(true);
        } else {
            rbOracle.setSelected(true);
        }

        rbEmbedded.setAlignmentX(Component.LEFT_ALIGNMENT);
        rbOracle.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(rbEmbedded);
        content.add(rbOracle);

        content.add(Box.createRigidArea(new Dimension(0, 15)));

        JPanel oraclePanel = new JPanel(new GridLayout(0, 1, 4, 4));
        oraclePanel.setBorder(BorderFactory.createTitledBorder("Oracle Connection Properties"));
        oraclePanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JTextField txtUrl = new JTextField(DatabaseConnection.getOracleUrl(), 25);
        JTextField txtUser = new JTextField(DatabaseConnection.getOracleUser(), 15);
        JPasswordField txtPass = new JPasswordField("oracle", 15);

        oraclePanel.add(new JLabel("Oracle JDBC URL:"));
        oraclePanel.add(txtUrl);
        oraclePanel.add(new JLabel("Username:"));
        oraclePanel.add(txtUser);
        oraclePanel.add(new JLabel("Password:"));
        oraclePanel.add(txtPass);

        content.add(oraclePanel);

        ActionListener toggleListener = e -> {
            boolean isOracle = rbOracle.isSelected();
            txtUrl.setEnabled(isOracle);
            txtUser.setEnabled(isOracle);
            txtPass.setEnabled(isOracle);
        };
        rbEmbedded.addActionListener(toggleListener);
        rbOracle.addActionListener(toggleListener);
        toggleListener.actionPerformed(null);

        content.add(Box.createRigidArea(new Dimension(0, 10)));

        // Reset Sample Data Button
        JButton btnResetData = new JButton("↺ Reset Sample Data (ACC1001, ACC1002, ACC1003)");
        btnResetData.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnResetData.addActionListener(e -> {
            try {
                DatabaseConnection.resetSampleData();
                JOptionPane.showMessageDialog(dialog, "Sample accounts and transactions reset successfully!",
                        "Reset Complete", JOptionPane.INFORMATION_MESSAGE);
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(dialog, "Failed to reset sample data: " + ex.getMessage(),
                        "Reset Failed", JOptionPane.ERROR_MESSAGE);
            }
        });
        content.add(btnResetData);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnTest = new JButton("Test Connection");
        JButton btnSave = new JButton("Save & Apply");
        JButton btnCancel = new JButton("Cancel");

        btnTest.addActionListener(e -> {
            if (rbEmbedded.isSelected()) {
                DatabaseConnection.switchToEmbeddedMode();
                JOptionPane.showMessageDialog(dialog, "Embedded Database connected and ready!",
                        "Test Success", JOptionPane.INFORMATION_MESSAGE);
            } else {
                String url = txtUrl.getText().trim();
                String user = txtUser.getText().trim();
                String pass = new String(txtPass.getPassword()).trim();
                try {
                    DatabaseConnection.configure(url, user, pass);
                    try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
                        if (conn != null && !conn.isClosed()) {
                            JOptionPane.showMessageDialog(dialog, "Connection to Oracle Database successful!",
                                    "Test Success", JOptionPane.INFORMATION_MESSAGE);
                        }
                    }
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(dialog, "Failed to connect to Oracle DB:\n" + ex.getMessage(),
                            "Connection Failed", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        btnSave.addActionListener(e -> {
            if (rbEmbedded.isSelected()) {
                DatabaseConnection.switchToEmbeddedMode();
            } else {
                String url = txtUrl.getText().trim();
                String user = txtUser.getText().trim();
                String pass = new String(txtPass.getPassword()).trim();
                DatabaseConnection.configure(url, user, pass);
            }
            updateDbConfigButtonText();
            dialog.dispose();
        });

        btnCancel.addActionListener(e -> dialog.dispose());

        buttonPanel.add(btnTest);
        buttonPanel.add(btnSave);
        buttonPanel.add(btnCancel);

        dialog.add(content, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }
}
