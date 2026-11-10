package vn.ninhkieupharma.pharmacy.auth.dao;

import vn.ninhkieupharma.pharmacy.auth.model.User;
import vn.ninhkieupharma.pharmacy.auth.model.UserRole;
import vn.ninhkieupharma.pharmacy.database.DatabaseConnection;
import vn.ninhkieupharma.pharmacy.features.common.dao.DataAccessException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import vn.ninhkieupharma.pharmacy.auth.service.PasswordHasher;

public final class UserDAOImpl implements UserDAO {
    private static final String SAMPLE_USERNAME = "admin_demo";
    private static final String INITIAL_ADMIN_PASSWORD_ENV = "PHARMACY_SAMPLE_ADMIN_PASSWORD";
    private static final String SCRIPT_HASH_PLACEHOLDER = "REPLACE_WITH_REAL_PASSWORD_HASH";

    public UserDAOImpl() {
        prepareSampleAccount();
    }

    private void prepareSampleAccount() {
        String updateSql = "UPDATE users SET password_hash = ? "
                + "WHERE username = ? AND password_hash = ?";
        try (Connection connection = DatabaseConnection.getConnection()) {
            String currentHash;
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT password_hash FROM users WHERE username = ?")) {
                statement.setString(1, SAMPLE_USERNAME);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (!resultSet.next()) {
                        throw new IllegalStateException("The `admin_demo` account is missing from `users`. "
                                + "Run 01_schema_quan_ly_cua_hang.sql before launching the application.");
                    }
                    currentHash = resultSet.getString("password_hash");
                }
            }
            if (SCRIPT_HASH_PLACEHOLDER.equals(currentHash)) {
                String initialPassword = System.getenv(INITIAL_ADMIN_PASSWORD_ENV);
                if (initialPassword == null || initialPassword.isBlank()) {
                    throw new IllegalStateException("Set " + INITIAL_ADMIN_PASSWORD_ENV
                            + " before the first launch to initialize the `admin_demo` account.");
                }
                try (PreparedStatement statement = connection.prepareStatement(updateSql)) {
                    statement.setString(1, samplePasswordHash(initialPassword));
                    statement.setString(2, SAMPLE_USERNAME);
                    statement.setString(3, SCRIPT_HASH_PLACEHOLDER);
                    statement.executeUpdate();
                }
            }
            if (findByUsername(SAMPLE_USERNAME).isEmpty()) {
                throw new IllegalStateException("The `admin_demo` account is missing from `users`. "
                        + "Run 01_schema_quan_ly_cua_hang.sql before launching the application.");
            }
        } catch (SQLException exception) {
            throw new DataAccessException("Could not prepare the sample account in the existing `users` table.",
                    exception);
        }
    }

    static String samplePasswordHash(String password) {
        return PasswordHasher.hash(password);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        String sql = """
                SELECT u.user_id, u.username, u.password_hash, u.is_active, r.role_code
                FROM users u
                JOIN roles r ON r.role_id = u.role_id
                WHERE u.username = ?
                """;
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(new User(
                        resultSet.getLong("user_id"),
                        resultSet.getString("username"),
                        resultSet.getString("password_hash"),
                        UserRole.valueOf(resultSet.getString("role_code").toUpperCase(java.util.Locale.ROOT)),
                        resultSet.getBoolean("is_active")));
            }
        } catch (SQLException | IllegalArgumentException exception) {
            throw new DataAccessException("Could not load the requested user.", exception);
        }
    }
}
