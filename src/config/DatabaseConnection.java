package config;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Statement;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Enhanced Database Connection Manager for Mini Banking System.
 * Supports:
 * 1. Embedded Local Database (Zero-configuration H2 in Oracle syntax mode)
 * 2. Oracle Database 19c/21c/23c (Remote or Local Oracle XE instance)
 *
 * Self-healing driver loader: dynamically discovers and registers H2 and Oracle
 * JDBC drivers even if launched without -cp or from IDEs with missing classpath settings.
 */
public class DatabaseConnection {

    public enum DatabaseMode {
        EMBEDDED,
        ORACLE
    }

    private static final String EMBEDDED_URL_TEMPLATE = "jdbc:h2:%s/bank_db;MODE=Oracle;AUTO_SERVER=TRUE;DB_CLOSE_DELAY=-1";
    private static final String DEFAULT_ORACLE_URL = "jdbc:oracle:thin:@localhost:1521:xe";
    private static final String DEFAULT_ORACLE_USER = "SYSTEM";
    private static final String DEFAULT_ORACLE_PASSWORD = "oracle";

    private static DatabaseMode currentMode = DatabaseMode.EMBEDDED;
    private static String dbUrl;
    private static String dbUser = "sa";
    private static String dbPassword = "";

    // Oracle configured settings
    private static String oracleUrl = System.getProperty("db.url", DEFAULT_ORACLE_URL);
    private static String oracleUser = System.getProperty("db.user", DEFAULT_ORACLE_USER);
    private static String oraclePassword = System.getProperty("db.password", DEFAULT_ORACLE_PASSWORD);

    private static volatile DatabaseConnection instance;
    private static boolean schemaInitialized = false;

    static {
        registerDrivers();
        initDefaultConfiguration();
    }

    private DatabaseConnection() {}

    /**
     * Resolves the canonical project root folder regardless of whether
     * the application was launched from src/, bin/, or the parent directory.
     */
    public static File getProjectRootDirectory() {
        try {
            File current = new File(".").getCanonicalFile();
            if (current.getName().equalsIgnoreCase("src") || current.getName().equalsIgnoreCase("bin")) {
                return current.getParentFile();
            }
            // Check if current directory has a child swing-jdbc-bank-app folder
            File sub = new File(current, "swing-jdbc-bank-app");
            if (sub.isDirectory() && new File(sub, "lib").isDirectory()) {
                return sub;
            }
            return current;
        } catch (Exception e) {
            return new File(".");
        }
    }

    /**
     * Attempts to find a jar file across common project directory structures.
     */
    private static File findJarFile(String jarName) {
        File projectRoot = getProjectRootDirectory();
        File[] candidates = new File[] {
            new File(projectRoot, "lib/" + jarName),
            new File(projectRoot, "../lib/" + jarName),
            new File("lib/" + jarName),
            new File("../lib/" + jarName),
            new File("../../lib/" + jarName),
            new File("swing-jdbc-bank-app/lib/" + jarName)
        };

        for (File candidate : candidates) {
            try {
                if (candidate.exists() && candidate.isFile()) {
                    return candidate.getCanonicalFile();
                }
            } catch (Exception ignored) {}
        }
        return null;
    }

    /**
     * Dynamically loads and registers a JDBC Driver from a JAR file if not on system classpath.
     */
    private static boolean loadAndRegisterDriver(String className, String jarName) {
        // 1. Try standard Class.forName first
        try {
            Class<?> clazz = Class.forName(className);
            System.out.println("[DatabaseConnection] Loaded " + className + " from system classpath.");
            return true;
        } catch (ClassNotFoundException e) {
            System.out.println("[DatabaseConnection] " + className + " not on system classpath, scanning for " + jarName + "...");
        } catch (Throwable t) {
            System.out.println("[DatabaseConnection] Note: " + className + " skipped on this JRE (" + t.getClass().getSimpleName() + ")");
            return false;
        }

        // 2. Locate jar and load dynamically via URLClassLoader + DriverShim
        File jarFile = findJarFile(jarName);
        if (jarFile != null && jarFile.exists()) {
            try {
                URL jarUrl = jarFile.toURI().toURL();
                URLClassLoader ucl = new URLClassLoader(new URL[]{jarUrl}, DatabaseConnection.class.getClassLoader());
                Class<?> driverClass = Class.forName(className, true, ucl);
                Driver driverInstance = (Driver) driverClass.getDeclaredConstructor().newInstance();
                DriverManager.registerDriver(new DriverShim(driverInstance));
                System.out.println("[DatabaseConnection] Successfully loaded & registered " + className + " from: " + jarFile.getAbsolutePath());
                return true;
            } catch (Exception ex) {
                System.err.println("[DatabaseConnection] Failed to dynamically register " + className + " from " + jarFile + ": " + ex.getMessage());
            }
        } else {
            System.err.println("[DatabaseConnection] Could not locate " + jarName + " on disk.");
        }
        return false;
    }

    private static void registerDrivers() {
        // Register H2 Driver (with dynamic jar fallback)
        loadAndRegisterDriver("org.h2.Driver", "h2.jar");

        // Register Oracle Driver (with dynamic jar fallback)
        loadAndRegisterDriver("oracle.jdbc.OracleDriver", "ojdbc11.jar");
    }

    private static void initDefaultConfiguration() {
        File projectRoot = getProjectRootDirectory();
        File dataDir = new File(projectRoot, "data");
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }
        String absoluteDataPath = dataDir.getAbsolutePath().replace('\\', '/');
        String embeddedUrl = String.format(EMBEDDED_URL_TEMPLATE, absoluteDataPath);

        // Try checking if Oracle is accessible
        boolean oracleAvailable = testOracleConnectionFast(oracleUrl, oracleUser, oraclePassword);
        if (oracleAvailable) {
            currentMode = DatabaseMode.ORACLE;
            dbUrl = oracleUrl;
            dbUser = oracleUser;
            dbPassword = oraclePassword;
            System.out.println("[DatabaseConnection] Connected to Oracle Database: " + dbUrl);
        } else {
            currentMode = DatabaseMode.EMBEDDED;
            dbUrl = embeddedUrl;
            dbUser = "sa";
            dbPassword = "";
            System.out.println("[DatabaseConnection] Running in Embedded Database Mode (Oracle-Compatible H2).");
            System.out.println("[DatabaseConnection] Storage directory: " + absoluteDataPath);
        }
    }

    private static boolean testOracleConnectionFast(String url, String user, String pass) {
        if (url == null || !url.toLowerCase().contains("oracle")) {
            return false;
        }
        try {
            Properties props = new Properties();
            props.setProperty("user", user);
            props.setProperty("password", pass);
            props.setProperty("oracle.net.CONNECT_TIMEOUT", "1500");
            props.setProperty("oracle.jdbc.ReadTimeout", "2000");

            try (Connection conn = DriverManager.getConnection(url, props)) {
                return conn != null && !conn.isClosed();
            }
        } catch (Exception e) {
            return false;
        }
    }

    public static DatabaseConnection getInstance() {
        if (instance == null) {
            synchronized (DatabaseConnection.class) {
                if (instance == null) {
                    instance = new DatabaseConnection();
                }
            }
        }
        return instance;
    }

    /**
     * Obtains a fresh JDBC Connection.
     */
    public Connection getConnection() throws SQLException {
        // Ensure drivers are registered before connecting
        registerDrivers();
        ensureSchemaInitialized();

        Properties props = new Properties();
        props.setProperty("user", dbUser);
        props.setProperty("password", dbPassword);

        if (currentMode == DatabaseMode.ORACLE) {
            props.setProperty("oracle.net.CONNECT_TIMEOUT", "5000");
            props.setProperty("oracle.jdbc.ReadTimeout", "10000");
        }

        return DriverManager.getConnection(dbUrl, props);
    }

    /**
     * Creates a new isolated connection instance.
     */
    public static Connection createNewConnection() throws SQLException {
        return getInstance().getConnection();
    }

    /**
     * Ensures schema and initial seed data exist when running in embedded mode.
     */
    public synchronized static void ensureSchemaInitialized() {
        if (schemaInitialized || currentMode != DatabaseMode.EMBEDDED) {
            return;
        }

        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
             Statement stmt = conn.createStatement()) {

            // Check if ACCOUNTS table already exists
            boolean tablesExist = false;
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'ACCOUNTS'")) {
                if (rs.next() && rs.getInt(1) > 0) {
                    tablesExist = true;
                }
            } catch (Exception ignored) {}

            if (!tablesExist) {
                createSchemaAndSeedData(stmt);
            }
            schemaInitialized = true;
        } catch (SQLException e) {
            System.err.println("[DatabaseConnection] Error initializing embedded schema: " + e.getMessage());
        }
    }

    private static void createSchemaAndSeedData(Statement stmt) throws SQLException {
        // Sequences
        stmt.execute("CREATE SEQUENCE IF NOT EXISTS SEQ_CUSTOMER_ID START WITH 1001 INCREMENT BY 1");
        stmt.execute("CREATE SEQUENCE IF NOT EXISTS SEQ_TRANSACTION_ID START WITH 50001 INCREMENT BY 1");

        // Customers Table
        stmt.execute("CREATE TABLE IF NOT EXISTS CUSTOMERS (" +
                "customer_id NUMBER(10) PRIMARY KEY, " +
                "full_name   VARCHAR2(100) NOT NULL, " +
                "email       VARCHAR2(100) NOT NULL UNIQUE, " +
                "pin_hash    VARCHAR2(64)  NOT NULL, " +
                "created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL)");

        // Accounts Table
        stmt.execute("CREATE TABLE IF NOT EXISTS ACCOUNTS (" +
                "account_no   VARCHAR2(20) PRIMARY KEY, " +
                "customer_id  NUMBER(10) NOT NULL, " +
                "account_type VARCHAR2(20) DEFAULT 'SAVINGS' NOT NULL, " +
                "balance      NUMBER(15, 2) DEFAULT 0.00 NOT NULL, " +
                "status       VARCHAR2(15) DEFAULT 'ACTIVE' NOT NULL, " +
                "created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL, " +
                "CONSTRAINT fk_accounts_customer FOREIGN KEY (customer_id) REFERENCES CUSTOMERS(customer_id) ON DELETE CASCADE, " +
                "CONSTRAINT chk_account_type CHECK (account_type IN ('SAVINGS', 'CHECKING', 'BUSINESS', 'CURRENT')), " +
                "CONSTRAINT chk_account_status CHECK (status IN ('ACTIVE', 'FROZEN', 'CLOSED')), " +
                "CONSTRAINT chk_account_balance CHECK (balance >= 0))");

        // Transactions Table
        stmt.execute("CREATE TABLE IF NOT EXISTS TRANSACTIONS (" +
                "transaction_id    NUMBER(12) PRIMARY KEY, " +
                "account_no        VARCHAR2(20) NOT NULL, " +
                "transaction_type  VARCHAR2(20) NOT NULL, " +
                "amount            NUMBER(15, 2) NOT NULL, " +
                "target_account_no VARCHAR2(20) NULL, " +
                "description       VARCHAR2(255), " +
                "transaction_date  TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL, " +
                "CONSTRAINT fk_trans_account FOREIGN KEY (account_no) REFERENCES ACCOUNTS(account_no), " +
                "CONSTRAINT fk_trans_target_account FOREIGN KEY (target_account_no) REFERENCES ACCOUNTS(account_no), " +
                "CONSTRAINT chk_trans_type CHECK (transaction_type IN ('DEPOSIT', 'WITHDRAWAL', 'TRANSFER', 'TRANSFER_IN', 'TRANSFER_OUT')), " +
                "CONSTRAINT chk_trans_amount CHECK (amount > 0))");

        // Seed Customers
        stmt.execute("INSERT INTO CUSTOMERS (customer_id, full_name, email, pin_hash) VALUES (1001, 'Alex Morgan', 'alex.morgan@example.com', '1234')");
        stmt.execute("INSERT INTO CUSTOMERS (customer_id, full_name, email, pin_hash) VALUES (1002, 'Samantha Reed', 'samantha.reed@example.com', '5678')");
        stmt.execute("INSERT INTO CUSTOMERS (customer_id, full_name, email, pin_hash) VALUES (1003, 'David Vance', 'david.vance@example.com', '9999')");

        // Seed Accounts
        stmt.execute("INSERT INTO ACCOUNTS (account_no, customer_id, account_type, balance) VALUES ('ACC1001', 1001, 'SAVINGS', 2500.00)");
        stmt.execute("INSERT INTO ACCOUNTS (account_no, customer_id, account_type, balance) VALUES ('ACC1002', 1002, 'CHECKING', 1200.00)");
        stmt.execute("INSERT INTO ACCOUNTS (account_no, customer_id, account_type, balance) VALUES ('ACC1003', 1003, 'SAVINGS', 5000.00)");

        // Initial Transactions
        stmt.execute("INSERT INTO TRANSACTIONS (transaction_id, account_no, transaction_type, amount, description) VALUES (SEQ_TRANSACTION_ID.NEXTVAL, 'ACC1001', 'DEPOSIT', 2500.00, 'Initial Account Opening Deposit')");
        stmt.execute("INSERT INTO TRANSACTIONS (transaction_id, account_no, transaction_type, amount, description) VALUES (SEQ_TRANSACTION_ID.NEXTVAL, 'ACC1002', 'DEPOSIT', 1200.00, 'Initial Account Opening Deposit')");
        stmt.execute("INSERT INTO TRANSACTIONS (transaction_id, account_no, transaction_type, amount, description) VALUES (SEQ_TRANSACTION_ID.NEXTVAL, 'ACC1003', 'DEPOSIT', 5000.00, 'Initial Account Opening Deposit')");

        System.out.println("[DatabaseConnection] Initialized database schema and sample accounts successfully.");
    }

    /**
     * Resets the embedded database back to initial seed data.
     */
    public synchronized static void resetSampleData() throws SQLException {
        if (currentMode != DatabaseMode.EMBEDDED) {
            throw new SQLException("Reset is only available in Embedded Database mode.");
        }
        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
             Statement stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS TRANSACTIONS CASCADE");
            stmt.execute("DROP TABLE IF EXISTS ACCOUNTS CASCADE");
            stmt.execute("DROP TABLE IF EXISTS CUSTOMERS CASCADE");
            stmt.execute("DROP SEQUENCE IF EXISTS SEQ_CUSTOMER_ID");
            stmt.execute("DROP SEQUENCE IF EXISTS SEQ_TRANSACTION_ID");
            createSchemaAndSeedData(stmt);
        }
    }

    /**
     * Dynamically update database configuration.
     */
    public static void configure(String url, String user, String password) {
        if (url != null && url.toLowerCase().contains("h2")) {
            currentMode = DatabaseMode.EMBEDDED;
            dbUrl = url;
            dbUser = user;
            dbPassword = password;
            schemaInitialized = false;
        } else {
            currentMode = DatabaseMode.ORACLE;
            oracleUrl = url;
            oracleUser = user;
            oraclePassword = password;
            dbUrl = url;
            dbUser = user;
            dbPassword = password;
        }
    }

    public static void switchToEmbeddedMode() {
        File projectRoot = getProjectRootDirectory();
        File dataDir = new File(projectRoot, "data");
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }
        String absoluteDataPath = dataDir.getAbsolutePath().replace('\\', '/');
        currentMode = DatabaseMode.EMBEDDED;
        dbUrl = String.format(EMBEDDED_URL_TEMPLATE, absoluteDataPath);
        dbUser = "sa";
        dbPassword = "";
        schemaInitialized = false;
        ensureSchemaInitialized();
    }

    public static DatabaseMode getCurrentMode() {
        return currentMode;
    }

    public static String getDbUrl() {
        return dbUrl;
    }

    public static String getDbUser() {
        return dbUser;
    }

    public static String getOracleUrl() {
        return oracleUrl;
    }

    public static String getOracleUser() {
        return oracleUser;
    }

    public static String getStatusSummary() {
        if (currentMode == DatabaseMode.EMBEDDED) {
            return "Embedded Local DB (Ready)";
        } else {
            return "Oracle DB (" + (oracleUrl.contains("@") ? oracleUrl.substring(oracleUrl.indexOf("@") + 1) : oracleUrl) + ")";
        }
    }

    public synchronized void closeConnection() {
        // Fresh connections are used per transaction, no shared state to close.
    }

    /**
     * DriverShim wraps a Driver loaded by a custom ClassLoader so DriverManager can accept it.
     */
    private static class DriverShim implements Driver {
        private final Driver driver;

        public DriverShim(Driver d) {
            this.driver = d;
        }

        @Override
        public boolean acceptsURL(String u) throws SQLException {
            return driver.acceptsURL(u);
        }

        @Override
        public Connection connect(String u, Properties p) throws SQLException {
            return driver.connect(u, p);
        }

        @Override
        public int getMajorVersion() {
            return driver.getMajorVersion();
        }

        @Override
        public int getMinorVersion() {
            return driver.getMinorVersion();
        }

        @Override
        public DriverPropertyInfo[] getPropertyInfo(String u, Properties p) throws SQLException {
            return driver.getPropertyInfo(u, p);
        }

        @Override
        public boolean jdbcCompliant() {
            return driver.jdbcCompliant();
        }

        @Override
        public Logger getParentLogger() throws SQLFeatureNotSupportedException {
            return driver.getParentLogger();
        }
    }
}
