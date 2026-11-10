package vn.ninhkieupharma.pharmacy.features.sales.dao;

import vn.ninhkieupharma.pharmacy.database.DatabaseConnection;
import vn.ninhkieupharma.pharmacy.features.common.dao.DataAccessException;
import vn.ninhkieupharma.pharmacy.features.common.dao.EntityCodec;
import vn.ninhkieupharma.pharmacy.features.common.dao.EntityCodecs;
import vn.ninhkieupharma.pharmacy.features.sales.model.SalesInvoice;
import vn.ninhkieupharma.pharmacy.features.common.service.DemoData;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

public final class SalesInvoiceDAOImpl implements SalesInvoiceDAO {
    private static final String TABLE = "SalesInvoice";
    private static final String MODULE_ID = "UC-17";
    private static final String SEED_KEY = "typed-seed-uc-17";
    private static final List<String> COLUMNS = List.of("invoiceCode", "issuedAt", "customer", "employee", "total", "status");
    private static final EntityCodec<SalesInvoice> CODEC = EntityCodecs.of(SalesInvoice::id,
                        value -> List.of(value.invoiceCode(), value.issuedAt(), value.customer(), value.employee(),
                                value.total(), value.status()),
                        (id, values) -> new SalesInvoice(id, values.get(0), values.get(1), values.get(2),
                                values.get(3), values.get(4), values.get(5)));

    public SalesInvoiceDAOImpl() {
    }

    public static EntityCodec<SalesInvoice> codec() {
        return CODEC;
    }

    private void initialize() {
        try (Connection connection = DatabaseConnection.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS `AppSetting` ("
                    + "setting_key VARCHAR(100) NOT NULL PRIMARY KEY, "
                    + "setting_value VARCHAR(500) NOT NULL) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
            statement.execute("CREATE TABLE IF NOT EXISTS `" + TABLE + "` ("
                    + "`entity_id` CHAR(36) NOT NULL PRIMARY KEY, "
                    + "`entity_order` BIGINT NOT NULL AUTO_INCREMENT UNIQUE, `invoiceCode` VARCHAR(1000) NOT NULL, `issuedAt` VARCHAR(1000) NOT NULL, `customer` VARCHAR(1000) NOT NULL, `employee` VARCHAR(1000) NOT NULL, `total` VARCHAR(1000) NOT NULL, `status` VARCHAR(1000) NOT NULL) "
                    + "CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
            seedIfNeeded(connection);
        } catch (SQLException exception) {
            throw new DataAccessException("Could not initialize SQL table " + TABLE + ".", exception);
        }
    }

    private void seedIfNeeded(Connection connection) throws SQLException {
        connection.setAutoCommit(false);
        try {
            if (isSeeded(connection)) {
                connection.commit();
                return;
            }
            if (!tableHasRows(connection)) {
                for (List<String> values : DemoData.rowsFor(MODULE_ID)) {
                    if (values.size() == COLUMNS.size()) {
                        insert(connection, CODEC.fromValues(UUID.randomUUID(), values));
                    }
                }
            }
            markSeedComplete(connection);
            connection.commit();
        } catch (SQLException | RuntimeException exception) {
            try { connection.rollback(); } catch (SQLException rollbackException) {
                exception.addSuppressed(rollbackException);
            }
            throw exception;
        } finally {
            connection.setAutoCommit(true);
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

    private boolean tableHasRows(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT 1 FROM `" + TABLE + "` LIMIT 1")) {
            return resultSet.next();
        }
    }

    private void markSeedComplete(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO `AppSetting` (setting_key, setting_value) VALUES (?, ?)")) {
            statement.setString(1, SEED_KEY);
            statement.setString(2, "completed");
            statement.executeUpdate();
        }
    }

    @Override
    public List<SalesInvoice> findAll() {
        String sql = "SELECT entity_id, " + columnList() + " FROM `" + TABLE + "` ORDER BY entity_order";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            List<SalesInvoice> entities = new ArrayList<>();
            while (resultSet.next()) entities.add(readEntity(resultSet));
            return List.copyOf(entities);
        } catch (SQLException exception) {
            throw new DataAccessException("Could not load records from " + TABLE + ".", exception);
        }
    }

    @Override
    public Optional<SalesInvoice> findById(UUID id) {
        String sql = "SELECT entity_id, " + columnList() + " FROM `" + TABLE + "` WHERE entity_id = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(readEntity(resultSet)) : Optional.empty();
            }
        } catch (SQLException exception) {
            throw new DataAccessException("Could not find a record in " + TABLE + ".", exception);
        }
    }

    @Override
    public SalesInvoice create(SalesInvoice entity) {
        try (Connection connection = DatabaseConnection.getConnection()) {
            insert(connection, entity);
            return entity;
        } catch (SQLException exception) {
            throw new DataAccessException("Could not create a record in " + TABLE + ".", exception);
        }
    }

    @Override
    public SalesInvoice update(SalesInvoice entity) {
        List<String> values = CODEC.values(entity);
        String assignments = COLUMNS.stream().map(column -> "`" + column + "` = ?")
                .reduce((left, right) -> left + ", " + right).orElseThrow();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE `" + TABLE + "` SET " + assignments + " WHERE entity_id = ?")) {
            bindValues(statement, values, 1);
            statement.setString(values.size() + 1, CODEC.id(entity).toString());
            if (statement.executeUpdate() == 0) throw new NoSuchElementException(
                    "Could not find record " + CODEC.id(entity) + ".");
            return entity;
        } catch (SQLException exception) {
            throw new DataAccessException("Could not update a record in " + TABLE + ".", exception);
        }
    }

    @Override
    public void delete(UUID id) {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "DELETE FROM `" + TABLE + "` WHERE entity_id = ?")) {
            statement.setString(1, id.toString());
            if (statement.executeUpdate() == 0) throw new NoSuchElementException(
                    "Could not find record " + id + ".");
        } catch (SQLException exception) {
            throw new DataAccessException("Could not delete a record from " + TABLE + ".", exception);
        }
    }

    private SalesInvoice readEntity(ResultSet resultSet) throws SQLException {
        List<String> values = new ArrayList<>(COLUMNS.size());
        for (String column : COLUMNS) values.add(resultSet.getString(column));
        return CODEC.fromValues(UUID.fromString(resultSet.getString("entity_id")), values);
    }

    private void insert(Connection connection, SalesInvoice entity) throws SQLException {
        List<String> values = CODEC.values(entity);
        if (values.size() != COLUMNS.size()) throw new IllegalArgumentException(
                "Expected " + COLUMNS.size() + " fields for " + TABLE + ".");
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO `" + TABLE + "` (entity_id, " + columnList() + ") VALUES (?, ?, ?, ?, ?, ?, ?)")) {
            statement.setString(1, CODEC.id(entity).toString());
            bindValues(statement, values, 2);
            statement.executeUpdate();
        }
    }

    private void bindValues(PreparedStatement statement, List<String> values, int startIndex)
            throws SQLException {
        for (int index = 0; index < values.size(); index++) {
            statement.setString(startIndex + index, values.get(index));
        }
    }

    private String columnList() {
        return COLUMNS.stream().map(column -> "`" + column + "`")
                .reduce((left, right) -> left + ", " + right).orElseThrow();
    }
}
