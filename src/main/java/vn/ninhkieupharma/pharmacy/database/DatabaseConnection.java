package vn.ninhkieupharma.pharmacy.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public final class DatabaseConnection {
    private static final String DEFAULT_HOST = "127.0.0.1";
    private static final int DEFAULT_PORT = 3306;
    private static final String DEFAULT_DATABASE = "quan_ly_cua_hang";
    private static final java.util.Set<String> REQUIRED_TABLES = java.util.Set.of(
            "roles", "users", "employees", "shifts", "employee_availabilities",
            "work_schedules", "schedule_assignments", "attendance_records",
            "product_categories", "suppliers", "products", "supplier_products",
            "warehouse_locations", "inventory_batches", "goods_receipts",
            "goods_receipt_items", "stocktakes", "stocktake_items", "customers",
            "customer_allergies", "loyalty_programs", "promotions", "sales",
            "sale_items", "inventory_movements");

    private DatabaseConnection() {
    }

    public static void verifySchema() throws SQLException {
        String database = databaseName();
        try (Connection connection = getConnection()) {
            java.util.Set<String> found = new java.util.HashSet<>();
            try (var statement = connection.createStatement();
                 var resultSet = statement.executeQuery(
                         "SELECT TABLE_NAME FROM information_schema.TABLES "
                                 + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_TYPE = 'BASE TABLE'")) {
                while (resultSet.next()) {
                    found.add(resultSet.getString(1));
                }
            }
            java.util.Set<String> missing = new java.util.TreeSet<>(REQUIRED_TABLES);
            missing.removeAll(found);
            if (!missing.isEmpty()) {
                throw new SQLException("Database `" + database + "` is missing required tables: "
                        + String.join(", ", missing) + ". Run 01_schema_quan_ly_cua_hang.sql first.");
            }
        } catch (SQLException exception) {
            if (exception.getMessage() != null
                    && exception.getMessage().contains("Run 01_schema_quan_ly_cua_hang.sql first")) {
                throw exception;
            }
            throw new SQLException("Could not connect to database `" + database
                    + "`. Start MySQL and run 01_schema_quan_ly_cua_hang.sql before launching the app.",
                    exception);
        }
    }

    public static Connection getConnection() throws SQLException {
        String database = databaseName();
        return DriverManager.getConnection(serverUrl() + database + connectionParameters(), credentials());
    }

    private static String databaseName() throws SQLException {
        String database = setting("PHARMACY_DB_NAME", DEFAULT_DATABASE);
        if (!database.matches("[A-Za-z0-9_]+")) {
            throw new SQLException("PHARMACY_DB_NAME may contain only letters, numbers, and underscores.");
        }
        return database;
    }

    private static String serverUrl() {
        String host = setting("PHARMACY_DB_HOST", DEFAULT_HOST);
        int port;
        try {
            port = Integer.parseInt(setting("PHARMACY_DB_PORT", Integer.toString(DEFAULT_PORT)));
        } catch (NumberFormatException exception) {
            throw new IllegalStateException("PHARMACY_DB_PORT must be a valid TCP port.", exception);
        }
        if (!host.matches("[A-Za-z0-9.-]+") || port < 1 || port > 65535) {
            throw new IllegalStateException("Configure a valid PHARMACY_DB_HOST and PHARMACY_DB_PORT.");
        }
        return "jdbc:mysql://" + host + ":" + port + "/";
    }

    private static String connectionParameters() {
        return "?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Ho_Chi_Minh";
    }

    private static Properties credentials() {
        Properties properties = new Properties();
        properties.setProperty("user", setting("PHARMACY_DB_USER", "root"));
        properties.setProperty("password", setting("PHARMACY_DB_PASSWORD", ""));
        properties.setProperty("allowPublicKeyRetrieval", "true");
        properties.setProperty("useSSL", "false");
        return properties;
    }

    private static String setting(String name, String defaultValue) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }
}
