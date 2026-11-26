package vn.ninhkieupharma.pharmacy.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public final class DatabaseConnection {
    private static final String SCHEMA_MISMATCH_PREFIX = "Database schema is missing required";
    private static final String SCHEMA_SETUP_HINT = "Initialize a new database with "
            + "environment/01_schema_nkp_db.sql and set PHARMACY_DB_NAME to its name.";
    private static final String DEFAULT_HOST = "127.0.0.1";
    private static final int DEFAULT_PORT = 3306;
    private static final String DEFAULT_DATABASE = "nkp_db";
    private static final java.util.Set<String> REQUIRED_TABLES = java.util.Set.of(
            "roles", "employees", "shifts", "employee_availabilities",
            "work_schedules", "schedule_assignments",
            "product_categories", "suppliers", "products", "supplier_products",
            "warehouse_locations", "inventory_batches", "goods_receipts",
            "goods_receipt_items", "goods_receipt_draft_items", "stocktakes", "stocktake_items",
            "customers",
            "customer_allergies", "loyalty_programs", "promotions", "sales",
            "promotion_point_tiers", "sale_items", "inventory_movements", "product_ingredients");
    private static final java.util.Map<String, java.util.Set<String>> REQUIRED_COLUMNS = java.util.Map.of(
            "employees", java.util.Set.of("role_id", "username", "password_hash", "is_active", "account_created_at"),
            "schedule_assignments", java.util.Set.of("has_attendance", "check_in_at", "check_out_at", "attendance_note"),
            "customers", java.util.Set.of("is_blocked", "block_reason", "date_of_birth",
                    "date_of_birth_year", "date_of_birth_month"),
            "products", java.util.Set.of("selling_price", "minimum_age_years"),
            "goods_receipts", java.util.Set.of("is_confirmed"),
            "sales", java.util.Set.of("promotion_discount_amount", "points_discount_amount",
                    "redeem_points", "requested_points", "revision", "prescription_code", "prescription_date", "prescribing_doctor",
                    "clinic", "diagnosis", "note"),
            "sale_items", java.util.Set.of("dosage_instructions", "treatment_days",
                    "line_kind", "dose_amount", "dose_unit", "dose_timing", "dose_sessions"));

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
                throw new MissingSchemaException(SCHEMA_MISMATCH_PREFIX + " tables: "
                        + String.join(", ", missing) + ". " + SCHEMA_SETUP_HINT);
            }
            for (var entry : REQUIRED_COLUMNS.entrySet()) {
                java.util.Set<String> foundColumns = new java.util.HashSet<>();
                try (var statement = connection.prepareStatement(
                        "SELECT COLUMN_NAME FROM information_schema.COLUMNS "
                                + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?")) {
                    statement.setString(1, entry.getKey());
                    try (var resultSet = statement.executeQuery()) {
                        while (resultSet.next()) {
                            foundColumns.add(resultSet.getString(1));
                        }
                    }
                }
                java.util.Set<String> missingColumns = new java.util.TreeSet<>(entry.getValue());
                missingColumns.removeAll(foundColumns);
                if (!missingColumns.isEmpty()) {
                    throw new MissingSchemaException(SCHEMA_MISMATCH_PREFIX + " "
                            + entry.getKey() + " columns: " + String.join(", ", missingColumns)
                            + ". " + SCHEMA_SETUP_HINT);
                }
            }
        } catch (SQLException exception) {
            if (exception.getMessage() != null && exception.getMessage().startsWith(SCHEMA_MISMATCH_PREFIX)) {
                throw exception;
            }
            throw new SQLException("Could not connect to database `" + database
                    + "`. Check that MySQL is running and the connection settings are correct. "
                    + SCHEMA_SETUP_HINT,
                    exception);
        }
    }

    public static final class MissingSchemaException extends SQLException {
        public MissingSchemaException(String message) {
            super(message);
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
