package config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Thread-Safe Singleton for Oracle JDBC Connection Management.
 * Manages database properties and provides connection pooling/handling.
 */
public class DatabaseConnection {

    // Default Oracle JDBC Connection settings
    private static String dbUrl = System.getProperty("db.url", "jdbc:oracle:thin:@localhost:1521:xe");
    private static String dbUser = System.getProperty("db.user", "SYSTEM");
    private static String dbPassword = System.getProperty("db.password", "oracle");
    
    // Singleton Instance
    private static volatile DatabaseConnection instance;
    private Connection connection;

    private DatabaseConnection() {
        registerDriver();
    }

    /**
     * Registers Oracle JDBC Driver.
     */
    private void registerDriver() {
        try {
            Class.forName("oracle.jdbc.OracleDriver");
        } catch (ClassNotFoundException e) {
            System.err.println("Warning: Oracle JDBC Driver class not found in classpath. Ensure ojdbc8.jar/ojdbc11.jar is added.");
        }
    }

    /**
     * Returns the thread-safe Singleton instance of DatabaseConnection.
     */
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
     * Obtains a fresh or active JDBC Connection to Oracle Database.
     * Re-establishes connection if closed or invalid.
     */
    public synchronized Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed() || !connection.isValid(2)) {
            Properties props = new Properties();
            props.setProperty("user", dbUser);
            props.setProperty("password", dbPassword);
            // Recommended Oracle Performance settings
            props.setProperty("oracle.net.CONNECT_TIMEOUT", "5000");
            props.setProperty("oracle.jdbc.ReadTimeout", "10000");
            
            connection = DriverManager.getConnection(dbUrl, props);
        }
        return connection;
    }

    /**
     * Creates a new connection instance (useful for isolated transaction management).
     */
    public static Connection createNewConnection() throws SQLException {
        try {
            Class.forName("oracle.jdbc.OracleDriver");
        } catch (ClassNotFoundException ignored) {}
        return DriverManager.getConnection(dbUrl, dbUser, dbPassword);
    }

    /**
     * Dynamically update database connection credentials at runtime.
     */
    public static void configure(String url, String user, String password) {
        dbUrl = url;
        dbUser = user;
        dbPassword = password;
        if (instance != null && instance.connection != null) {
            try {
                if (!instance.connection.isClosed()) {
                    instance.connection.close();
                }
            } catch (SQLException ignored) {}
            instance.connection = null;
        }
    }

    public static String getDbUrl() {
        return dbUrl;
    }

    public static String getDbUser() {
        return dbUser;
    }

    /**
     * Closes the active singleton connection cleanly.
     */
    public synchronized void closeConnection() {
        if (connection != null) {
            try {
                if (!connection.isClosed()) {
                    connection.close();
                }
            } catch (SQLException e) {
                System.err.println("Error closing database connection: " + e.getMessage());
            } finally {
                connection = null;
            }
        }
    }
}
