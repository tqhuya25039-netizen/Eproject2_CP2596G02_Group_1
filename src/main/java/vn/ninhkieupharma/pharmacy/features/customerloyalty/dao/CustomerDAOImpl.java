package vn.ninhkieupharma.pharmacy.features.customerloyalty.dao;

import vn.ninhkieupharma.pharmacy.database.DatabaseConnection;
import vn.ninhkieupharma.pharmacy.features.common.dao.DataAccessException;
import vn.ninhkieupharma.pharmacy.features.customerloyalty.model.Customer;
import vn.ninhkieupharma.pharmacy.features.common.service.DemoData;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

public final class CustomerDAOImpl implements CustomerDAO {
    private static final List<String> SCHEMA_SQL = List.of(
            """
                    CREATE TABLE IF NOT EXISTS `AppSetting` (
                        setting_key VARCHAR(100) NOT NULL PRIMARY KEY,
                        setting_value VARCHAR(500) NOT NULL
                    ) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci
                    """,
            """
                    CREATE TABLE IF NOT EXISTS `Customer` (
                        customer_id CHAR(36) NOT NULL PRIMARY KEY,
                        customer_code VARCHAR(30) NOT NULL UNIQUE,
                        full_name VARCHAR(200) NOT NULL,
                        phone VARCHAR(50) NOT NULL,
                        allergies VARCHAR(500) NOT NULL,
                        loyalty_points INT NOT NULL CHECK (loyalty_points >= 0),
                        status VARCHAR(50) NOT NULL
                    ) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci
                    """);
    private static final String SEED_KEY = "demo-customer-seed-v1";

    public CustomerDAOImpl() {
    }

    private void initialize() {
        try (Connection connection = DatabaseConnection.getConnection();
             Statement statement = connection.createStatement()) {
            for (String schemaSql : SCHEMA_SQL) {
                statement.execute(schemaSql);
            }
            seedDemoCustomersIfNeeded(connection);
        } catch (SQLException | RuntimeException exception) {
            throw new DataAccessException(
                    "Could not initialize the Customer table in MySQL. Check the database settings "
                            + "and ensure the SQL setup script has been run. Existing customer data must have "
                            + "valid loyalty-point values.",
                    exception);
        }
    }

    private void seedDemoCustomersIfNeeded(Connection connection) throws SQLException {
        connection.setAutoCommit(false);
        try {
            if (isSeeded(connection)) {
                connection.commit();
                return;
            }
            if (hasCustomers(connection)) {
                markSeedComplete(connection);
                connection.commit();
                return;
            }
            List<Customer> legacyCustomers = readLegacyCustomers(connection);
            if (legacyCustomers.isEmpty()) {
                legacyCustomers = DemoData.rowsFor("UC-16").stream()
                        .map(this::fromDemoValues)
                        .toList();
            }
            for (Customer customer : legacyCustomers) {
                insert(connection, customer);
            }
            markSeedComplete(connection);
            connection.commit();
        } catch (SQLException | RuntimeException exception) {
            rollback(connection, exception);
            throw exception;
        } finally {
            connection.setAutoCommit(true);
        }
    }

    private boolean hasCustomers(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM `Customer` LIMIT 1");
             ResultSet resultSet = statement.executeQuery()) {
            return resultSet.next();
        }
    }

    private List<Customer> readLegacyCustomers(Connection connection) throws SQLException {
        try (ResultSet tables = connection.getMetaData().getTables(
                connection.getCatalog(), null, "FeatureRecord", new String[]{"TABLE"})) {
            if (!tables.next()) {
                return List.of();
            }
        }
        String sql = """
                SELECT r.record_id, v.column_index, v.value_text
                FROM `FeatureRecord` r
                JOIN `FeatureRecordValue` v ON v.record_id = r.record_id
                WHERE r.module_id = 'UC-16'
                ORDER BY r.row_order, v.column_index
                """;
        Map<UUID, List<String>> valuesById = new LinkedHashMap<>();
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                UUID id = UUID.fromString(resultSet.getString("record_id"));
                List<String> values = valuesById.computeIfAbsent(id, ignored -> new ArrayList<>());
                int columnIndex = resultSet.getInt("column_index");
                while (values.size() < columnIndex - 1) {
                    values.add("");
                }
                values.add(resultSet.getString("value_text"));
            }
        }
        return valuesById.entrySet().stream()
                .filter(entry -> entry.getValue().size() == 6)
                .map(entry -> fromValues(entry.getKey(), entry.getValue()))
                .toList();
    }

    private Customer fromDemoValues(List<String> values) {
        return fromValues(UUID.randomUUID(), values);
    }

    private Customer fromValues(UUID id, List<String> values) {
        return new Customer(
                id,
                values.get(0),
                values.get(1),
                values.get(2),
                values.get(3),
                Integer.parseInt(values.get(4).replace(".", "").replace(",", "")),
                values.get(5));
    }

    private void markSeedComplete(Connection connection) throws SQLException {
        try (PreparedStatement setting = connection.prepareStatement(
                "INSERT INTO `AppSetting` (setting_key, setting_value) VALUES (?, ?)")) {
            setting.setString(1, SEED_KEY);
            setting.setString(2, "completed");
            setting.executeUpdate();
        }
    }

    private boolean isSeeded(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM `AppSetting` WHERE setting_key = ?")) {
            statement.setString(1, SEED_KEY);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    @Override
    public List<Customer> findAll() {
        String sql = """
                SELECT customer_id, customer_code, full_name, phone, allergies, loyalty_points, status
                FROM `Customer`
                ORDER BY customer_code
                """;
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            List<Customer> customers = new ArrayList<>();
            while (resultSet.next()) {
                customers.add(readCustomer(resultSet));
            }
            return List.copyOf(customers);
        } catch (SQLException exception) {
            throw new DataAccessException("Could not load customers from MySQL.", exception);
        }
    }

    @Override
    public Optional<Customer> findById(UUID id) {
        String sql = """
                SELECT customer_id, customer_code, full_name, phone, allergies, loyalty_points, status
                FROM `Customer`
                WHERE customer_id = ?
                """;
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(readCustomer(resultSet)) : Optional.empty();
            }
        } catch (SQLException exception) {
            throw new DataAccessException("Could not find a customer in MySQL.", exception);
        }
    }

    @Override
    public Customer create(Customer customer) {
        String sql = """
                INSERT INTO `Customer`
                    (customer_id, customer_code, full_name, phone, allergies, loyalty_points, status)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bindCustomer(statement, customer);
            statement.executeUpdate();
            return customer;
        } catch (SQLException exception) {
            throw new DataAccessException("Could not create the customer in MySQL.", exception);
        }
    }

    @Override
    public Customer update(Customer customer) {
        String sql = """
                UPDATE `Customer`
                SET customer_code = ?, full_name = ?, phone = ?, allergies = ?, loyalty_points = ?, status = ?
                WHERE customer_id = ?
                """;
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, customer.customerCode());
            statement.setString(2, customer.fullName());
            statement.setString(3, customer.phone());
            statement.setString(4, customer.allergies());
            statement.setInt(5, customer.loyaltyPoints());
            statement.setString(6, customer.status());
            statement.setString(7, customer.id().toString());
            if (statement.executeUpdate() == 0) {
                throw new NoSuchElementException("Could not find customer " + customer.id() + ".");
            }
            return customer;
        } catch (SQLException exception) {
            throw new DataAccessException("Could not update the customer in MySQL.", exception);
        }
    }

    @Override
    public void delete(UUID id) {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "DELETE FROM `Customer` WHERE customer_id = ?")) {
            statement.setString(1, id.toString());
            if (statement.executeUpdate() == 0) {
                throw new NoSuchElementException("Could not find customer " + id + ".");
            }
        } catch (SQLException exception) {
            throw new DataAccessException("Could not delete the customer from MySQL.", exception);
        }
    }

    private Customer readCustomer(ResultSet resultSet) throws SQLException {
        return new Customer(
                UUID.fromString(resultSet.getString("customer_id")),
                resultSet.getString("customer_code"),
                resultSet.getString("full_name"),
                resultSet.getString("phone"),
                resultSet.getString("allergies"),
                resultSet.getInt("loyalty_points"),
                resultSet.getString("status"));
    }

    private void insert(Connection connection, Customer customer) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO `Customer`
                    (customer_id, customer_code, full_name, phone, allergies, loyalty_points, status)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """)) {
            bindCustomer(statement, customer);
            statement.executeUpdate();
        }
    }

    private void bindCustomer(PreparedStatement statement, Customer customer) throws SQLException {
        statement.setString(1, customer.id().toString());
        statement.setString(2, customer.customerCode());
        statement.setString(3, customer.fullName());
        statement.setString(4, customer.phone());
        statement.setString(5, customer.allergies());
        statement.setInt(6, customer.loyaltyPoints());
        statement.setString(7, customer.status());
    }

    private void rollback(Connection connection, Exception originalException) {
        try {
            connection.rollback();
        } catch (SQLException rollbackException) {
            originalException.addSuppressed(rollbackException);
        }
    }
}
