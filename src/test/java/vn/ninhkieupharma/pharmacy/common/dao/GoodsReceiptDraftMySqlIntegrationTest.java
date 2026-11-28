package vn.ninhkieupharma.pharmacy.common.dao;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import vn.ninhkieupharma.pharmacy.common.auth.User;
import vn.ninhkieupharma.pharmacy.common.auth.UserRole;
import vn.ninhkieupharma.pharmacy.config.DatabaseConnection;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledIfEnvironmentVariable(named = MySqlTestSchema.ENABLED_ENV, matches = "true")
class GoodsReceiptDraftMySqlIntegrationTest {
    @Test
    void draftsCanBeEditedOrCancelledAndConfirmationAppliesStockAndLocksTheReceipt() throws Exception {
        try (MySqlTestSchema schema = MySqlTestSchema.withCurrentSchema()) {
            schema.runInChild(ReceiptDraftScenario.class);
        }
    }

    public static final class ReceiptDraftScenario {
        public static void main(String[] args) throws Exception {
            long staffId = firstId("""
                    SELECT e.employee_id FROM employees e JOIN roles r ON r.role_id = e.role_id
                    WHERE e.is_active = 1 AND r.role_code = 'STAFF'
                    """);
            long adminId = firstId("""
                    SELECT e.employee_id FROM employees e JOIN roles r ON r.role_id = e.role_id
                    WHERE e.is_active = 1 AND r.role_code = 'ADMIN'
                    """);
            RelationalFeatureModuleDAO staffDao = new RelationalFeatureModuleDAO(
                    new User(staffId, "integration-staff", "", UserRole.STAFF, true));
            RelationalFeatureModuleDAO adminDao = new RelationalFeatureModuleDAO(
                    new User(adminId, "integration-admin", "", UserRole.ADMIN, true));
            long supplierId = firstId("SELECT supplier_id FROM suppliers WHERE is_active = 1");
            long productId = firstId("SELECT product_id FROM products WHERE is_active = 1");
            long locationId = firstId("SELECT location_id FROM warehouse_locations WHERE is_active = 1");
            String batchCode = "DRAFT-" + UUID.randomUUID().toString().substring(0, 8);
            InventoryWorkflowDAO.ReceiptRequest initial = request(
                    supplierId, productId, locationId, batchCode, "5");

            var draft = staffDao.createGoodsReceiptDraft(staffId, initial);
            assertEquals("Chưa xác nhận", draft.values().get(4));
            assertTrue(inventoryQuantity(productId, locationId, batchCode).signum() == 0);
            assertEquals(1, staffDao.findGoodsReceiptDraft(draft.id()).lines().size());

            staffDao.updateGoodsReceiptDraft(staffId, draft.id(),
                    request(supplierId, productId, locationId, batchCode, "2"));
            assertTrue(inventoryQuantity(productId, locationId, batchCode).signum() == 0);

            var confirmed = adminDao.confirmGoodsReceipt(adminId, draft.id());
            assertEquals("Đã xác nhận", confirmed.values().get(4));
            assertEquals(new BigDecimal("2.000"), inventoryQuantity(productId, locationId, batchCode));
            assertThrows(IllegalArgumentException.class, () -> staffDao.updateGoodsReceiptDraft(
                    staffId, draft.id(), request(supplierId, productId, locationId, batchCode, "3")));
            assertThrows(IllegalArgumentException.class,
                    () -> staffDao.cancelGoodsReceiptDraft(staffId, draft.id()));

            String cancelledBatchCode = batchCode + "-CANCEL";
            var cancellable = staffDao.createGoodsReceiptDraft(staffId,
                    request(supplierId, productId, locationId, cancelledBatchCode, "1"));
            staffDao.cancelGoodsReceiptDraft(staffId, cancellable.id());
            assertEquals("CANCELLED", receiptStatus(cancellable.id()));
            assertEquals("Đã hủy", staffDao.findAll("UC-12").stream()
                    .filter(row -> row.id().equals(cancellable.id()))
                    .findFirst()
                    .orElseThrow()
                    .values()
                    .get(4));
            assertEquals(1, staffDao.findGoodsReceiptDetails(cancellable.id()).size());
            assertTrue(inventoryQuantity(productId, locationId, cancelledBatchCode).signum() == 0);
            assertThrows(IllegalArgumentException.class,
                    () -> staffDao.findGoodsReceiptDraft(cancellable.id()));
        }

        private static InventoryWorkflowDAO.ReceiptRequest request(
                long supplierId, long productId, long locationId, String batchCode, String quantity) {
            return new InventoryWorkflowDAO.ReceiptRequest(
                    supplierId,
                    LocalDate.now(),
                    "",
                    List.of(new InventoryWorkflowDAO.ReceiptLine(
                            productId, locationId, batchCode, null,
                            new BigDecimal(quantity), BigDecimal.ZERO)));
        }

        private static long firstId(String sql) throws Exception {
            try (Connection connection = DatabaseConnection.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql);
                 ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new IllegalStateException("The disposable schema has no active records for: " + sql);
                }
                return resultSet.getLong(1);
            }
        }

        private static BigDecimal inventoryQuantity(long productId, long locationId, String batchCode)
                throws Exception {
            try (Connection connection = DatabaseConnection.getConnection();
                 PreparedStatement statement = connection.prepareStatement("""
                         SELECT COALESCE(MAX(quantity_on_hand), 0)
                         FROM inventory_batches
                         WHERE product_id = ? AND location_id = ? AND batch_code = ?
                         """)) {
                statement.setLong(1, productId);
                statement.setLong(2, locationId);
                statement.setString(3, batchCode);
                try (ResultSet resultSet = statement.executeQuery()) {
                    resultSet.next();
                    return resultSet.getBigDecimal(1);
                }
            }
        }

        private static String receiptStatus(UUID receiptId) throws Exception {
            try (Connection connection = DatabaseConnection.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "SELECT status FROM goods_receipts WHERE receipt_id = ?")) {
                statement.setLong(1, receiptId.getLeastSignificantBits());
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (!resultSet.next()) {
                        throw new IllegalStateException("The cancelled receipt disappeared.");
                    }
                    return resultSet.getString(1);
                }
            }
        }
    }
}
