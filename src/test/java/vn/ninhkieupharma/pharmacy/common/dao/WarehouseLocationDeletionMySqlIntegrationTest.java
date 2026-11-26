package vn.ninhkieupharma.pharmacy.common.dao;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import vn.ninhkieupharma.pharmacy.common.auth.User;
import vn.ninhkieupharma.pharmacy.common.auth.UserRole;
import vn.ninhkieupharma.pharmacy.config.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

@EnabledIfEnvironmentVariable(named = MySqlTestSchema.ENABLED_ENV, matches = "true")
class WarehouseLocationDeletionMySqlIntegrationTest {
    @Test
    void occupiedLocationsCannotBeDeletedButUnusedLocationsCan() throws Exception {
        try (MySqlTestSchema schema = MySqlTestSchema.withCurrentSchema()) {
            schema.runInChild(LocationDeletionScenario.class);
        }
    }

    public static final class LocationDeletionScenario {
        public static void main(String[] args) throws Exception {
            long adminId = firstId("""
                    SELECT e.employee_id
                    FROM employees e
                    JOIN roles r ON r.role_id = e.role_id
                    WHERE e.is_active = 1 AND r.role_code = 'ADMIN'
                    """);
            var dao = new RelationalFeatureModuleDAO(
                    new User(adminId, "integration-admin", "", UserRole.ADMIN, true));

            var occupiedLocation = dao.findInventoryLocations().stream()
                    .filter(location -> dao.findInventoryBatches(location.id()).stream()
                            .anyMatch(batch -> batch.quantityOnHand().signum() > 0))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException(
                            "The disposable schema has no location with stock."));
            var occupiedRow = dao.findAll("UC-13").stream()
                    .filter(row -> row.values().get(0).equals(occupiedLocation.code()))
                    .findFirst()
                    .orElseThrow();
            assertThrows(IllegalStateException.class,
                    () -> dao.delete("UC-13", occupiedRow.id()));

            String code = "DEL-" + UUID.randomUUID().toString().substring(0, 8);
            var unusedLocation = dao.create("UC-13",
                    List.of(code, "Integration Empty Location", "", "ACTIVE"));
            dao.delete("UC-13", unusedLocation.id());
            assertFalse(dao.findAll("UC-13").stream()
                    .anyMatch(row -> row.id().equals(unusedLocation.id())));
        }

        private static long firstId(String sql) throws Exception {
            try (Connection connection = DatabaseConnection.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql);
                 ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new IllegalStateException("The disposable schema has no active administrator.");
                }
                return resultSet.getLong(1);
            }
        }
    }
}
