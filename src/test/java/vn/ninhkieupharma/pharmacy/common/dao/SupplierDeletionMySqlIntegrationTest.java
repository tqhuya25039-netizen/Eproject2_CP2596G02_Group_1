package vn.ninhkieupharma.pharmacy.common.dao;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import vn.ninhkieupharma.pharmacy.common.auth.User;
import vn.ninhkieupharma.pharmacy.common.auth.UserRole;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@EnabledIfEnvironmentVariable(named = MySqlTestSchema.ENABLED_ENV, matches = "true")
class SupplierDeletionMySqlIntegrationTest {
    @Test
    void preventsDeletingSuppliersThatSupplyProductsAndAllowsUnusedSuppliersToBeDeleted() throws Exception {
        try (MySqlTestSchema schema = MySqlTestSchema.withCurrentSchema()) {
            schema.execute("""
                    INSERT INTO suppliers (supplier_id, supplier_code, supplier_name)
                    VALUES (999, 'NCC999', 'Unused Supplier')
                    """);
            schema.runInChild(SupplierDeletionScenario.class);
        }
    }

    public static final class SupplierDeletionScenario {
        public static void main(String[] args) throws SQLException {
            String schema = System.getenv("PHARMACY_DB_NAME");
            if (schema == null || !schema.matches("pharmacy_test_[a-f0-9]{32}")) {
                throw new IllegalStateException("The supplier deletion scenario requires a disposable test database.");
            }

            var dao = new RelationalFeatureModuleDAO(
                    new User(1, "admin_demo", "", UserRole.ADMIN, true));
            var linkedSupplier = dao.findAll("UC-09").stream()
                    .filter(row -> row.id().getLeastSignificantBits() == 1)
                    .findFirst()
                    .orElseThrow();
            IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                    () -> dao.delete("UC-09", linkedSupplier.id()));
            assertEquals("Không thể xóa nhà cung cấp vì nhà cung cấp này đang cung cấp sản phẩm.",
                    error.getMessage());

            try (var connection = vn.ninhkieupharma.pharmacy.config.DatabaseConnection.getConnection();
                 var statement = connection.createStatement();
                 var resultSet = statement.executeQuery(
                         "SELECT COUNT(*) FROM supplier_products WHERE supplier_id = 1")) {
                if (!resultSet.next() || resultSet.getInt(1) == 0) {
                    throw new AssertionError("The supplier-product link should remain after the rejected deletion.");
                }
            }

            var unusedSupplier = dao.findAll("UC-09").stream()
                    .filter(row -> row.id().getLeastSignificantBits() == 999)
                    .findFirst()
                    .orElseThrow();
            dao.delete("UC-09", unusedSupplier.id());
            if (dao.findAll("UC-09").stream().anyMatch(row -> row.id().getLeastSignificantBits() == 999)) {
                throw new AssertionError("An unused supplier should be deleted.");
            }
        }
    }
}
