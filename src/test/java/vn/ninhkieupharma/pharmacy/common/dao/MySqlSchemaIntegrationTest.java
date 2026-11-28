package vn.ninhkieupharma.pharmacy.common.dao;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import vn.ninhkieupharma.pharmacy.common.auth.AuthenticationService;
import vn.ninhkieupharma.pharmacy.common.auth.PasswordHasher;
import vn.ninhkieupharma.pharmacy.common.auth.User;
import vn.ninhkieupharma.pharmacy.common.auth.UserDAOImpl;
import vn.ninhkieupharma.pharmacy.common.auth.UserRole;
import vn.ninhkieupharma.pharmacy.config.DatabaseConnection;
import vn.ninhkieupharma.pharmacy.common.service.FeatureServices;
import vn.ninhkieupharma.pharmacy.common.service.ModuleCatalog;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class MySqlSchemaIntegrationTest {
    @Test
    @EnabledIfEnvironmentVariable(named = "PHARMACY_DB_INTEGRATION_TEST", matches = "true")
    void readsEveryModuleFromTheProvidedSchemaWithoutCreatingBusinessRows() throws SQLException {
        DatabaseConnection.verifySchema();
        User administrator;
        try (var connection = DatabaseConnection.getConnection();
             var statement = connection.prepareStatement("""
                     SELECT e.employee_id, e.username FROM employees e
                     JOIN roles r ON r.role_id = e.role_id
                     WHERE r.role_code = 'ADMIN' AND e.is_active = 1
                     ORDER BY e.employee_id LIMIT 1
                     """);
             var accounts = statement.executeQuery()) {
            if (!accounts.next()) {
                throw new IllegalStateException("The configured database must have an active administrator.");
            }
            administrator = new User(accounts.getLong("employee_id"), accounts.getString("username"),
                    "", UserRole.ADMIN, true);
        }
        FeatureServices services = new FeatureServices(new RelationalFeatureModuleDAO(administrator));

        for (var module : ModuleCatalog.MODULES) {
            services.forModule(module).findAll(module).forEach(row ->
                    assertEquals(module.columns().size(), row.values().size(),
                            "Rows should match displayed columns for " + module.id()));
        }
    }

    @Test
    @EnabledIfEnvironmentVariable(named = MySqlTestSchema.ENABLED_ENV, matches = "true")
    void authenticatesStandaloneAndEmployeeAccountsFromTheSameTable() throws Exception {
        try (MySqlTestSchema schema = MySqlTestSchema.withCurrentSchema()) {
            schema.runInChild(EmployeeAccountScenario.class);
        }
    }

    public static final class EmployeeAccountScenario {
        public static void main(String[] args) throws SQLException {
            String database = System.getenv("PHARMACY_DB_NAME");
            if (database == null || !database.matches("pharmacy_test_[a-f0-9]{32}")) {
                throw new IllegalStateException("Employee account tests require a disposable database.");
            }
            String password = UUID.randomUUID().toString();
            try (var connection = DatabaseConnection.getConnection();
                 var statement = connection.prepareStatement(
                         "UPDATE employees SET password_hash = ? WHERE username = 'admin_demo'")) {
                statement.setString(1, PasswordHasher.hash(password));
                assertEquals(1, statement.executeUpdate());
            }

            var authentication = new AuthenticationService(new UserDAOImpl());
            User admin = authentication.authenticate("admin_demo", password).orElseThrow();
            assertEquals(UserRole.ADMIN, admin.role());
            var repository = new RelationalFeatureModuleDAO(admin);
            assertFalse(repository.findAll("UC-01").stream()
                    .anyMatch(row -> row.id().getLeastSignificantBits() == admin.id()));

            var employeeModule = ModuleCatalog.MODULES.stream()
                    .filter(module -> module.id().equals("UC-01")).findFirst().orElseThrow();
            var employees = new FeatureServices(repository).forModule(employeeModule);
            var employee = employees.create(employeeModule,
                    List.of("NV-NEW", "New employee", "", "", "new_staff", "STAFF", password));
            User staff = authentication.authenticate("new_staff", password).orElseThrow();
            assertEquals(UserRole.STAFF, staff.role());
            assertEquals(employee.id().getLeastSignificantBits(), staff.id());
        }
    }
}
