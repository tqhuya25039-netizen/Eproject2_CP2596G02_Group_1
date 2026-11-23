package vn.ninhkieupharma.pharmacy.common.dao;

import org.junit.jupiter.api.Test;
import vn.ninhkieupharma.pharmacy.common.service.ModuleCatalog;
import vn.ninhkieupharma.pharmacy.common.dao.InventoryWorkflowDAO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

class RelationalFeatureModuleDAOTest {
    private final RelationalFeatureModuleDAO dao = new RelationalFeatureModuleDAO();

    @Test
    void categoryFormMatchesTheNormalizedSchemaFields() {
        var category = ModuleCatalog.MODULES.stream()
                .filter(module -> module.id().equals("UC-07"))
                .findFirst()
                .orElseThrow();

        assertEquals(3, category.columns().size());
        assertThrows(IllegalArgumentException.class,
                () -> dao.create("UC-07", List.of("category code", "category name", "description", "ACTIVE")));
    }

    @Test
    void complexWorkflowsRemainReadOnlyInsteadOfWritingPartialRecords() {
        for (String moduleId : List.of("UC-03", "UC-04", "UC-12", "UC-14", "UC-17")) {
            UnsupportedOperationException error = assertThrows(UnsupportedOperationException.class,
                    () -> dao.create(moduleId, List.of()));
            assertTrue(error.getMessage().contains(moduleId));
        }
    }

    @Test
    void storageLocationRequiresCodeNameAndValidFieldLengths() {
        assertThrows(IllegalArgumentException.class,
                () -> dao.create("UC-13", List.of("", "Kệ A", "", "ACTIVE")));
        assertThrows(IllegalArgumentException.class,
                () -> dao.create("UC-13", List.of("K-A", "", "", "ACTIVE")));
        assertThrows(IllegalArgumentException.class,
                () -> dao.update("UC-13", UUID.randomUUID(),
                        List.of("", "Kệ A", "", "ACTIVE")));
    }

    @Test
    void inventoryWorkflowsRejectIncompleteAndInvalidRequestsBeforeConnectingToMysql() {
        assertThrows(IllegalArgumentException.class, () -> dao.create("UC-13",
                List.of("", "Kệ A · Tầng 1", "", "ACTIVE")));
        assertThrows(IllegalArgumentException.class, () -> dao.receiveGoods(1,
                new InventoryWorkflowDAO.ReceiptRequest(1, LocalDate.now(), "", List.of())));
        assertThrows(IllegalArgumentException.class, () -> dao.receiveGoods(1,
                new InventoryWorkflowDAO.ReceiptRequest(1, LocalDate.now(), "",
                        List.of(new InventoryWorkflowDAO.ReceiptLine(
                                1, 1, "LOT-1", LocalDate.now().plusDays(30),
                                new BigDecimal("-1"), BigDecimal.ZERO)))));
        assertThrows(IllegalArgumentException.class, () -> dao.receiveGoods(1,
                new InventoryWorkflowDAO.ReceiptRequest(1, LocalDate.now().plusDays(1), "",
                        List.of(new InventoryWorkflowDAO.ReceiptLine(
                                1, 1, "LOT-1", LocalDate.now().plusDays(30),
                                new BigDecimal("1"), BigDecimal.ZERO)))));
        assertThrows(IllegalArgumentException.class, () -> dao.receiveGoods(1,
                new InventoryWorkflowDAO.ReceiptRequest(1, LocalDate.now(), "",
                        List.of(new InventoryWorkflowDAO.ReceiptLine(
                                1, 1, "LOT-1", LocalDate.now().plusDays(30),
                                new BigDecimal("1"), BigDecimal.ZERO),
                                new InventoryWorkflowDAO.ReceiptLine(
                                        1, 1, "LOT-1", LocalDate.now().plusDays(30),
                                        new BigDecimal("2"), new BigDecimal("1000"))))));
        assertThrows(IllegalArgumentException.class, () -> dao.receiveGoods(1,
                new InventoryWorkflowDAO.ReceiptRequest(1, LocalDate.now(), "",
                        List.of(new InventoryWorkflowDAO.ReceiptLine(
                                1, 1, "LOT-1", LocalDate.now().plusDays(30),
                                new BigDecimal("1"), new BigDecimal("1.001"))))));
        assertThrows(IllegalArgumentException.class, () -> dao.receiveGoods(1,
                new InventoryWorkflowDAO.ReceiptRequest(1, LocalDate.now(), "",
                        List.of(new InventoryWorkflowDAO.ReceiptLine(
                                1, 1, "LOT-1", LocalDate.now().minusDays(1),
                                new BigDecimal("1"), BigDecimal.ZERO)))));
        assertThrows(IllegalArgumentException.class, () -> dao.completeStocktake(1,
                new InventoryWorkflowDAO.StocktakeRequest(1, LocalDate.now(), "", List.of())));
        assertThrows(IllegalArgumentException.class, () -> dao.recordInventoryLoss(
                1, 1, InventoryWorkflowDAO.LossType.DAMAGED, BigDecimal.ZERO, ""));
    }

    @Test
    void employeeAccountCreationDoesNotAllowElevatedRoles() {
        assertThrows(IllegalArgumentException.class, () -> dao.create("UC-01",
                List.of("NV-TEST", "Test Employee", "", "", "test_admin", "ADMIN", "Test@12345")));
    }

    @Test
    void employeeAccountCreationValidatesPasswordBeforeConnectingToMysql() {
        assertThrows(IllegalArgumentException.class, () -> dao.create("UC-01",
                List.of("NV-TEST", "Test Employee", "", "", "test_staff", "STAFF", "x".repeat(73))));
    }

    @Test
    void customerCreationRequiresNameAndBlockReasonBeforeConnectingToMysql() {
        IllegalArgumentException missingName = assertThrows(IllegalArgumentException.class,
                () -> dao.create("UC-16", List.of("", "", "0901234567", "", "", "0", "ACTIVE", "", "")));
        assertTrue(missingName.getMessage().toLowerCase().contains("tên"));

        IllegalArgumentException missingReason = assertThrows(IllegalArgumentException.class,
                () -> dao.create("UC-16",
                        List.of("", "Test Customer", "0901234567", "", "", "0", "BLOCKED", "", "")));
        assertTrue(missingReason.getMessage().toLowerCase().contains("lý do"));
    }

    @Test
    void customerCreationRejectsMalformedPhoneAndDuplicateAllergies() {
        IllegalArgumentException invalidPhone = assertThrows(IllegalArgumentException.class,
                () -> dao.create("UC-16",
                        List.of("", "Test Customer", "phone", "", "", "0", "ACTIVE", "", "")));
        assertTrue(invalidPhone.getMessage().contains("điện thoại"));

        IllegalArgumentException duplicateAllergy = assertThrows(IllegalArgumentException.class,
                () -> dao.create("UC-16",
                        List.of("", "Test Customer", "0901234567", "", "Penicillin\npenicillin",
                                "0", "ACTIVE", "", "")));
        assertTrue(duplicateAllergy.getMessage().contains("trùng"));
    }

    @Test
    void salesCustomerCreationValidatesStaffEditableFieldsBeforeConnectingToMysql() {
        IllegalArgumentException missingName = assertThrows(IllegalArgumentException.class,
                () -> dao.createCustomerFromSale(List.of("", "0901234567", "", "", "")));
        assertTrue(missingName.getMessage().contains("Tên"));

        IllegalArgumentException invalidPhone = assertThrows(IllegalArgumentException.class,
                () -> dao.createCustomerFromSale(List.of("Test Customer", "phone", "", "", "")));
        assertTrue(invalidPhone.getMessage().contains("điện thoại"));

        IllegalArgumentException duplicateAllergy = assertThrows(IllegalArgumentException.class,
                () -> dao.createCustomerFromSale(
                        List.of("Test Customer", "0901234567", "", "Penicillin\npenicillin", "")));
        assertTrue(duplicateAllergy.getMessage().contains("trùng"));
    }

    @Test
    void salesCustomerEditingAcceptsOnlyProfileFieldsNotPointsOrRestrictions() {
        assertThrows(IllegalArgumentException.class,
                () -> dao.updateCustomerFromSale(UUID.randomUUID(),
                        List.of("", "", "", "", "9999", "BLOCKED", "reason")));
    }

    @Test
    void customerHistoryRequiresARelationalCustomerId() {
        assertThrows(IllegalArgumentException.class,
                () -> dao.findCustomerPurchaseHistory(UUID.randomUUID()));
    }

    @Test
    void catalogModulesAllowStaffReadAccess() {
        for (String moduleId : List.of("UC-07", "UC-08", "UC-09")) {
            var module = ModuleCatalog.MODULES.stream()
                    .filter(candidate -> candidate.id().equals(moduleId))
                    .findFirst()
                    .orElseThrow();
            assertTrue(module.staffAccessible(), moduleId + " should allow staff read access");
        }
    }

    @Test
    void reportModulesRemainUnavailableToStaff() {
        for (String moduleId : List.of("UC-10", "UC-11")) {
            var module = ModuleCatalog.MODULES.stream()
                    .filter(candidate -> candidate.id().equals(moduleId))
                    .findFirst()
                    .orElseThrow();
            assertFalse(module.staffAccessible(), moduleId + " should remain unavailable to staff");
        }
    }

    @Test
    void customerManagementIsNotExposedAsAStaffNavigationModule() {
        var customerModule = ModuleCatalog.MODULES.stream()
                .filter(module -> module.id().equals("UC-16"))
                .findFirst()
                .orElseThrow();

        assertFalse(customerModule.staffAccessible());
        assertEquals(9, customerModule.columns().size());

        var salesModule = ModuleCatalog.MODULES.stream()
                .filter(module -> module.id().equals("UC-17"))
                .findFirst()
                .orElseThrow();
        assertTrue(salesModule.staffAccessible());
    }

    @Test
    void bulkReplacementIsNeverAllowedForExistingBusinessData() {
        assertThrows(UnsupportedOperationException.class,
                () -> dao.replaceAll("UC-07", List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> dao.delete("UC-07", UUID.randomUUID()));
    }
}
