package vn.ninhkieupharma.pharmacy.features.common.service;

import org.junit.jupiter.api.Test;
import vn.ninhkieupharma.pharmacy.features.common.model.ModuleRow;
import vn.ninhkieupharma.pharmacy.features.common.model.ModuleDefinition;
import vn.ninhkieupharma.pharmacy.features.common.dao.FeatureModuleDAO;
import vn.ninhkieupharma.pharmacy.features.common.service.ModuleCatalog;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FeatureServicesTest {
    @Test
    void everySeedRowMatchesItsModuleColumns() {
        FeatureServices services = new FeatureServices();
        for (ModuleDefinition module : ModuleCatalog.MODULES) {
            FeatureModuleService service = services.forModule(module);
            assertTrue(service.supports(module), () -> module.id() + " has no dedicated service");
            assertTrue(ModuleCatalog.MODULES.stream()
                            .filter(other -> !other.id().equals(module.id()))
                            .noneMatch(service::supports),
                    () -> module.id() + " service unexpectedly supports another use case");
            assertTrue(service.findAll(module).stream()
                    .allMatch(row -> row.values().size() == module.columns().size()),
                    () -> module.id() + " has sample rows with a mismatched column count");
        }
    }

    @Test
    void supportsCreateUpdateDeleteAndResetsForANewSession() {
        FeatureServices services = new FeatureServices();
        ModuleDefinition productsModule = module("UC-08");
        FeatureModuleService products = services.forModule(productsModule);
        int originalSize = products.findAll(productsModule).size();

        ModuleRow created = products.create(productsModule,
                List.of("TEST01", "Test Product", "Test Category", "Box", "No", "Active", "Test Supplier"));
        assertEquals(originalSize + 1, products.findAll(productsModule).size());

        products.update(productsModule, created.id(),
                List.of("TEST01", "Updated Product", "Test Category", "Box", "No", "Active", "Test Supplier"));
        assertTrue(products.findAll(productsModule).stream()
                .anyMatch(row -> row.id().equals(created.id())
                        && row.values().get(1).equals("Updated Product")));

        products.delete(productsModule, created.id());
        assertEquals(originalSize, products.findAll(productsModule).size());

        FeatureServices freshSession = new FeatureServices();
        assertEquals(originalSize,
                freshSession.forModule(productsModule).findAll(productsModule).size());
    }

    @Test
    void categoryTrendReportReflectsProductChangesAndIsReadOnly() {
        FeatureServices services = new FeatureServices();
        ModuleDefinition productsModule = module("UC-08");
        ModuleDefinition categoryReport = module("UC-10");
        ModuleDefinition supplierReport = module("UC-11");
        FeatureModuleService products = services.forModule(productsModule);

        ModuleRow created = products.create(productsModule,
                List.of("TEST02", "Test Product", "Test Category", "Box", "No", "Active", "Test Supplier"));
        assertTrue(services.forModule(categoryReport).findAll(categoryReport).stream()
                .anyMatch(row -> row.values().get(0).equals("Test Category")));
        assertTrue(services.forModule(supplierReport).findAll(supplierReport).stream()
                .anyMatch(row -> row.values().get(0).equals("Test Supplier")));

        assertThrows(IllegalStateException.class, () ->
                services.forModule(categoryReport).create(categoryReport,
                        List.of("Category", "1", "100%", "Stable")));
        products.delete(productsModule, created.id());
        assertFalse(services.forModule(categoryReport).findAll(categoryReport).stream()
                .anyMatch(row -> row.values().get(0).equals("Test Category")));
    }

    @Test
    void inventoryMovementReportReflectsReceiptsAndStocktakes() {
        FeatureServices services = new FeatureServices();
        ModuleDefinition receiptModule = module("UC-12");
        ModuleDefinition stocktakeModule = module("UC-14");
        ModuleDefinition movementReport = module("UC-15");
        FeatureModuleService receipts = services.forModule(receiptModule);
        FeatureModuleService stocktakes = services.forModule(stocktakeModule);

        ModuleRow receipt = receipts.create(receiptModule,
                List.of("PNTEST", "Test Supplier", "10/11/2026", "9 items", "Received"));
        ModuleRow stocktake = stocktakes.create(stocktakeModule,
                List.of("KKTEST", "Test Shelf", "10/11/2026", "+3 items", "Draft"));
        assertTrue(services.forModule(movementReport).findAll(movementReport).stream()
                .anyMatch(row -> row.values().get(4).equals("Phiếu PNTEST")));
        assertTrue(services.forModule(movementReport).findAll(movementReport).stream()
                .anyMatch(row -> row.values().get(4).equals("Phiên KKTEST")));

        receipts.delete(receiptModule, receipt.id());
        stocktakes.delete(stocktakeModule, stocktake.id());
        assertFalse(services.forModule(movementReport).findAll(movementReport).stream()
                .anyMatch(row -> row.values().get(4).equals("Phiếu PNTEST")));
        assertFalse(services.forModule(movementReport).findAll(movementReport).stream()
                .anyMatch(row -> row.values().get(4).equals("Phiên KKTEST")));
    }

    @Test
    void inventoryMovementReportUsesNativeQueryWithoutBulkReplacement() {
        List<String> expectedValues = List.of("10/11/2026", "Product A", "Sale", "2", "Invoice INV-1");
        FeatureModuleDAO nativeReportDAO = new FeatureModuleDAO() {
            @Override
            public List<ModuleRow> findAll(String moduleId) {
                return List.of(new ModuleRow(UUID.randomUUID(), expectedValues));
            }

            @Override
            public boolean supportsNativeReports() {
                return true;
            }

            @Override
            public void replaceAll(String moduleId, List<List<String>> values) {
                throw new AssertionError("Native reports must not replace stored rows.");
            }

            @Override
            public ModuleRow create(String moduleId, List<String> values) {
                throw new UnsupportedOperationException();
            }

            @Override
            public ModuleRow update(String moduleId, UUID rowId, List<String> values) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void delete(String moduleId, UUID rowId) {
                throw new UnsupportedOperationException();
            }
        };
        ModuleDefinition movementReport = module("UC-15");

        assertEquals(expectedValues,
                new FeatureServices(nativeReportDAO).forModule(movementReport)
                        .findAll(movementReport).get(0).values());
    }

    @Test
    void workHourAndComplianceReportsFollowAttendanceRecords() {
        FeatureServices services = new FeatureServices();
        ModuleDefinition attendanceModule = module("UC-04");
        ModuleDefinition payrollReport = module("UC-05");
        ModuleDefinition complianceReport = module("UC-06");

        ModuleRow attendance = services.forModule(attendanceModule).create(attendanceModule,
                List.of("Test Employee", "Morning · 09:00–17:00",
                        "09:00", "17:00", "On Time"));

        assertTrue(services.forModule(payrollReport).findAll(payrollReport).stream()
                .anyMatch(row -> row.values().get(0).equals("Test Employee")
                        && row.values().get(3).startsWith("8,0")));
        assertTrue(services.forModule(complianceReport).findAll(complianceReport).stream()
                .anyMatch(row -> row.values().get(0).equals("Test Employee")
                        && row.values().get(1).equals("09:00–17:00")));

        services.forModule(attendanceModule).delete(attendanceModule, attendance.id());
        assertFalse(services.forModule(payrollReport).findAll(payrollReport).stream()
                .anyMatch(row -> row.values().get(0).equals("Test Employee")));
    }

    @Test
    void revenueReportIsProjectedFromSessionInvoices() {
        FeatureServices services = new FeatureServices();
        ModuleDefinition salesModule = module("UC-17");
        ModuleDefinition revenueReport = module("UC-19");

        services.forModule(salesModule).create(salesModule,
                List.of("HDTEST", "10/11/2026 10:00", "Khách lẻ", "Nhân viên demo",
                        "100.000 ₫", "Hoàn tất"));

        assertTrue(services.forModule(revenueReport).findAll(revenueReport).stream()
                .anyMatch(row -> row.values().equals(
                        List.of("10/11/2026", "1", "100.000 ₫", "0 ₫", "100.000 ₫"))));
    }

    @Test
    void salesServiceRejectsInvalidInvoiceDateAndTotal() {
        FeatureServices services = new FeatureServices();
        ModuleDefinition salesModule = module("UC-17");
        FeatureModuleService sales = services.forModule(salesModule);

        assertThrows(IllegalArgumentException.class, () -> sales.create(salesModule,
                List.of("HDTEST1", "not-a-date", "Khách lẻ", "Nhân viên demo",
                        "100.000 ₫", "Hoàn tất")));
        assertThrows(IllegalArgumentException.class, () -> sales.create(salesModule,
                List.of("HDTEST2", "10/11/2026 10:00", "Khách lẻ", "Nhân viên demo",
                        "invalid", "Hoàn tất")));
    }

    @Test
    void receivingAndStocktakeServicesValidateQuantities() {
        FeatureServices services = new FeatureServices();
        ModuleDefinition receivingModule = module("UC-12");
        ModuleDefinition stocktakeModule = module("UC-14");

        assertThrows(IllegalArgumentException.class, () ->
                services.forModule(receivingModule).create(receivingModule,
                        List.of("PNTEST", "Test Supplier", "10/11/2026", "many", "Received")));
        assertThrows(IllegalArgumentException.class, () ->
                services.forModule(stocktakeModule).create(stocktakeModule,
                        List.of("KKTEST", "Test Shelf", "10/11/2026", "unknown", "Draft")));
        assertEquals(5, services.forModule(stocktakeModule).create(stocktakeModule,
                List.of("KKTEST2", "Test Shelf", "10/11/2026", "Khớp tồn", "Draft"))
                .values().size());
    }

    private ModuleDefinition module(String id) {
        return ModuleCatalog.MODULES.stream()
                .filter(module -> module.id().equals(id))
                .findFirst()
                .orElseThrow();
    }
}
