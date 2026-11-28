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
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledIfEnvironmentVariable(named = MySqlTestSchema.ENABLED_ENV, matches = "true")
class StocktakeDraftMySqlIntegrationTest {
    @Test
    void draftsCanBeEditedOrCancelledAndConfirmationAppliesCountsAndLocksTheSession() throws Exception {
        try (MySqlTestSchema schema = MySqlTestSchema.withCurrentSchema()) {
            schema.runInChild(StocktakeScenario.class);
        }
    }

    public static final class StocktakeScenario {
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
            InventoryWorkflowDAO.LocationOption location = staffDao.findInventoryLocations().stream()
                    .filter(option -> !staffDao.findInventoryBatches(option.id()).isEmpty())
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("The disposable schema has no stock to count."));
            List<InventoryWorkflowDAO.BatchOption> initialBatches = staffDao.findInventoryBatches(location.id());
            InventoryWorkflowDAO.BatchOption target = initialBatches.get(0);
            List<InventoryWorkflowDAO.StocktakeCount> initialCounts = counts(initialBatches, target.id(), 1);
            BigDecimal initialQuantity = target.quantityOnHand();
            String stocktakeNote = "Morning shift count";

            var draft = staffDao.createStocktakeDraft(staffId,
                    request(location.id(), stocktakeNote, initialCounts));
            assertEquals("Chưa xác nhận", draft.values().get(4));
            assertEquals(initialQuantity, inventoryQuantity(target.id()));
            var loadedDraft = staffDao.findStocktakeDraft(draft.id());
            assertEquals(initialCounts.size(), loadedDraft.counts().size());
            assertEquals(stocktakeNote, loadedDraft.note());
            assertEquals(5, staffDao.findAll("UC-14").stream()
                    .filter(row -> row.id().equals(draft.id()))
                    .findFirst()
                    .orElseThrow()
                    .values()
                    .size());
            var draftDetails = staffDao.findStocktakeDetails(draft.id());
            assertEquals(stocktakeNote, draftDetails.note());
            assertEquals(initialCounts.size(), draftDetails.items().size());

            List<InventoryWorkflowDAO.StocktakeCount> editedCounts =
                    counts(initialBatches, target.id(), 2);
            String updatedNote = "Second count after review";
            staffDao.updateStocktakeDraft(staffId, draft.id(),
                    request(location.id(), updatedNote, editedCounts));
            assertEquals(initialQuantity, inventoryQuantity(target.id()));
            assertEquals(updatedNote, staffDao.findStocktakeDraft(draft.id()).note());
            assertEquals(updatedNote, staffDao.findStocktakeDetails(draft.id()).note());

            var confirmed = adminDao.confirmStocktake(adminId, draft.id());
            assertEquals("Đã xác nhận", confirmed.values().get(4));
            assertEquals(initialQuantity.add(new BigDecimal("2")), inventoryQuantity(target.id()));
            assertEquals(updatedNote, staffDao.findStocktakeDetails(draft.id()).note());
            assertThrows(IllegalArgumentException.class, () -> staffDao.updateStocktakeDraft(
                    staffId, draft.id(), request(location.id(), editedCounts)));
            assertThrows(IllegalArgumentException.class,
                    () -> staffDao.cancelStocktakeDraft(staffId, draft.id()));

            List<InventoryWorkflowDAO.BatchOption> currentBatches = staffDao.findInventoryBatches(location.id());
            InventoryWorkflowDAO.BatchOption cancellationTarget = currentBatches.get(0);
            BigDecimal quantityBeforeCancellation = cancellationTarget.quantityOnHand();
            String cancellationNote = "Count was cancelled";
            var cancellable = staffDao.createStocktakeDraft(staffId,
                    request(location.id(), cancellationNote, counts(currentBatches, cancellationTarget.id(), 1)));
            staffDao.cancelStocktakeDraft(staffId, cancellable.id());
            assertEquals("CANCELLED", stocktakeStatus(cancellable.id()));
            assertTrue(stocktakeItemCount(cancellable.id()) > 0);
            assertEquals(quantityBeforeCancellation, inventoryQuantity(cancellationTarget.id()));
            assertEquals(cancellationNote, staffDao.findStocktakeDetails(cancellable.id()).note());
            assertEquals("Đã hủy", staffDao.findAll("UC-14").stream()
                    .filter(row -> row.id().equals(cancellable.id()))
                    .findFirst()
                    .orElseThrow()
                    .values()
                    .get(4));
            assertThrows(IllegalArgumentException.class, () -> staffDao.findStocktakeDraft(cancellable.id()));
        }

        private static InventoryWorkflowDAO.StocktakeRequest request(
                long locationId, List<InventoryWorkflowDAO.StocktakeCount> counts) {
            return request(locationId, "", counts);
        }

        private static InventoryWorkflowDAO.StocktakeRequest request(
                long locationId, String note, List<InventoryWorkflowDAO.StocktakeCount> counts) {
            return new InventoryWorkflowDAO.StocktakeRequest(locationId, LocalDate.now(), note, counts);
        }

        private static List<InventoryWorkflowDAO.StocktakeCount> counts(
                List<InventoryWorkflowDAO.BatchOption> batches, long targetBatchId, int increment) {
            List<InventoryWorkflowDAO.StocktakeCount> counts = new ArrayList<>();
            for (InventoryWorkflowDAO.BatchOption batch : batches) {
                BigDecimal counted = batch.quantityOnHand();
                if (batch.id() == targetBatchId) {
                    counted = counted.add(BigDecimal.valueOf(increment));
                }
                counts.add(new InventoryWorkflowDAO.StocktakeCount(batch.id(), counted));
            }
            return List.copyOf(counts);
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

        private static BigDecimal inventoryQuantity(long batchId) throws Exception {
            try (Connection connection = DatabaseConnection.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "SELECT quantity_on_hand FROM inventory_batches WHERE batch_id = ?")) {
                statement.setLong(1, batchId);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (!resultSet.next()) {
                        throw new IllegalStateException("The inventory batch disappeared during the test.");
                    }
                    return resultSet.getBigDecimal(1);
                }
            }
        }

        private static String stocktakeStatus(java.util.UUID stocktakeId) throws Exception {
            try (Connection connection = DatabaseConnection.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "SELECT status FROM stocktakes WHERE stocktake_id = ?")) {
                statement.setLong(1, stocktakeId.getLeastSignificantBits());
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (!resultSet.next()) {
                        throw new IllegalStateException("The cancelled stocktake disappeared.");
                    }
                    return resultSet.getString(1);
                }
            }
        }

        private static int stocktakeItemCount(java.util.UUID stocktakeId) throws Exception {
            try (Connection connection = DatabaseConnection.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "SELECT COUNT(*) FROM stocktake_items WHERE stocktake_id = ?")) {
                statement.setLong(1, stocktakeId.getLeastSignificantBits());
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (!resultSet.next()) {
                        throw new IllegalStateException("Could not count the retained stocktake items.");
                    }
                    return resultSet.getInt(1);
                }
            }
        }
    }
}
