package vn.ninhkieupharma.pharmacy.common.service;

import org.junit.jupiter.api.Test;
import vn.ninhkieupharma.pharmacy.common.model.ModuleRow;
import vn.ninhkieupharma.pharmacy.common.model.ModuleDefinition;
import vn.ninhkieupharma.pharmacy.common.dao.FeatureModuleDAO;
import vn.ninhkieupharma.pharmacy.common.repository.InMemoryModuleRepository;
import vn.ninhkieupharma.pharmacy.common.service.ModuleCatalog;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FeatureServicesTest {
    private static final Map<String, String> REPORT_OWNERS = Map.of(
            "UC-05", "UC-04",
            "UC-06", "UC-03",
            "UC-10", "UC-07",
            "UC-11", "UC-09",
            "UC-15", "UC-14",
            "UC-19", "UC-17");

    @Test
    void everySeedRowMatchesItsModuleColumns() {
        FeatureServices services = new FeatureServices();
        for (ModuleDefinition module : ModuleCatalog.MODULES) {
            FeatureModuleService service = services.forModule(module);
            assertTrue(service.supports(module), () -> module.id() + " has no dedicated service");
            String owner = REPORT_OWNERS.getOrDefault(module.id(), module.id());
            assertTrue(ModuleCatalog.MODULES.stream()
                            .filter(other -> !other.id().equals(module.id()))
                            .noneMatch(other -> service.supports(other)
                                    && !owner.equals(REPORT_OWNERS.getOrDefault(other.id(), other.id()))),
                    () -> module.id() + " service unexpectedly supports another use case");
            assertTrue(service.findAll(module).stream()
                    .allMatch(row -> row.values().size() == module.columns().size()),
                    () -> module.id() + " has sample rows with a mismatched column count");
        }
    }

    @Test
    void supplierRowsIncludeProductCountsWithoutAddingAFormField() {
        FeatureServices services = new FeatureServices();
        ModuleDefinition suppliersModule = module("UC-09");

        assertEquals(7, suppliersModule.columns().size());
        assertEquals(6, suppliersModule.formFields().size());
        Map<String, String> productCounts = services.forModule(suppliersModule).findAll(suppliersModule).stream()
                .collect(java.util.stream.Collectors.toMap(
                        row -> row.values().get(1), row -> row.values().get(6)));

        assertEquals("3", productCounts.get("Dược phẩm Hậu Giang"));
        assertEquals("2", productCounts.get("Thiết bị y tế Mekong"));
        assertEquals("1", productCounts.get("Phân phối An Khang"));
        assertEquals("0", productCounts.get("Dược liệu Tây Đô"));

        services.forModule(suppliersModule).create(suppliersModule,
                List.of("NCC999", "Nhà cung cấp mới", "", "", "", "ACTIVE"));
        var newSupplier = services.forModule(suppliersModule).findAll(suppliersModule).stream()
                .filter(row -> row.values().get(0).equals("NCC999"))
                .findFirst()
                .orElseThrow();
        assertEquals(7, newSupplier.values().size());
        assertEquals("0", newSupplier.values().get(6));
    }

    @Test
    void readOnlyReportsShareTheirOwningDomainService() {
        FeatureServices services = new FeatureServices();
        REPORT_OWNERS.forEach((reportId, ownerId) ->
                assertEquals(services.forModule(module(ownerId)),
                        services.forModule(module(reportId)), reportId + " should use " + ownerId + " service"));
    }

    @Test
    void allReportsRemainReadOnly() {
        FeatureServices services = new FeatureServices();
        REPORT_OWNERS.keySet().forEach(reportId ->
                assertThrows(IllegalStateException.class, () ->
                        services.forModule(module(reportId)).create(module(reportId), List.of())));
    }

    @Test
    void supportsCreateUpdateDeleteAndResetsForANewSession() {
        FeatureServices services = new FeatureServices();
        ModuleDefinition productsModule = module("UC-08");
        FeatureModuleService products = services.forModule(productsModule);
        int originalSize = products.findAll(productsModule).size();

        ModuleRow created = products.create(productsModule,
                List.of("TEST01", "Test Product", "Test Category", "Tablet", "Box",
                        "12000", "No", "ACTIVE", "Test Supplier", "0"));
        assertEquals(originalSize + 1, products.findAll(productsModule).size());

        products.update(productsModule, created.id(),
                List.of("TEST01", "Updated Product", "Test Category", "Tablet", "Box",
                        "12000", "No", "ACTIVE", "Test Supplier", "0"));
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
                List.of("TEST02", "Test Product", "Test Category", "Tablet", "Box",
                        "12000", "No", "ACTIVE", "Test Supplier A, Test Supplier B", "0"));
        List<ModuleRow> categoryRows = services.forModule(categoryReport).findAll(categoryReport);
        assertTrue(categoryRows.stream()
                .anyMatch(row -> row.values().get(1).equals("Test Category")
                        && row.values().get(2).contains("2 NCC")));
        List<ModuleRow> supplierRows = services.forModule(supplierReport).findAll(supplierReport);
        assertTrue(supplierRows.stream().anyMatch(row -> row.values().get(1).equals("Test Supplier A")));
        assertTrue(supplierRows.stream().anyMatch(row -> row.values().get(1).equals("Test Supplier B")));

        assertThrows(IllegalStateException.class, () ->
                services.forModule(categoryReport).create(categoryReport,
                        List.of("Category", "1", "100%", "Stable")));
        products.delete(productsModule, created.id());
        assertFalse(services.forModule(categoryReport).findAll(categoryReport).stream()
                .anyMatch(row -> row.values().get(1).equals("Test Category")));
    }

    @Test
    void inventoryMovementReportReflectsReceiptsAndStocktakes() {
        InMemoryModuleRepository repository = new InMemoryModuleRepository();
        FeatureServices services = new FeatureServices(repository);
        ModuleDefinition receiptModule = module("UC-12");
        ModuleDefinition movementReport = module("UC-15");

        ModuleRow receipt = repository.create("UC-12",
                List.of("PNTEST", "Test Supplier", "10/11/2026", "9 items", "Received"));
        ModuleRow stocktake = repository.create("UC-14",
                List.of("KKTEST", "Test Shelf", "10/11/2026", "+3 items", "Draft"));
        assertTrue(services.forModule(movementReport).findAll(movementReport).stream()
                .anyMatch(row -> row.values().get(4).equals("Phiếu PNTEST")));
        assertTrue(services.forModule(movementReport).findAll(movementReport).stream()
                .anyMatch(row -> row.values().get(4).equals("Phiên KKTEST")));

        repository.delete("UC-12", receipt.id());
        repository.delete("UC-14", stocktake.id());
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
    void workHourReportAcceptsMysqlAttendanceTimestamps() {
        FeatureServices services = new FeatureServices();
        ModuleDefinition attendanceModule = module("UC-04");
        ModuleDefinition payrollReport = module("UC-05");

        services.forModule(attendanceModule).create(attendanceModule,
                List.of("Timestamp Employee", "Morning shift", "2026-11-25 09:00",
                        "2026-11-25 17:00", ""));

        assertTrue(services.forModule(payrollReport).findAll(payrollReport).stream()
                .anyMatch(row -> row.values().get(0).equals("Timestamp Employee")
                        && row.values().get(3).startsWith("8,0")));
    }

    @Test
    void workHourReportAcceptsVietnameseAttendanceTimestamps() {
        FeatureServices services = new FeatureServices();
        ModuleDefinition attendanceModule = module("UC-04");
        ModuleDefinition payrollReport = module("UC-05");

        ModuleRow attendance = services.forModule(attendanceModule).create(attendanceModule,
                List.of("Vietnamese Date Employee", "Morning shift",
                        "25/11/2026 09:00", "25/11/2026 17:00", ""));

        assertEquals("2026-11-25 09:00", attendance.values().get(2));
        assertEquals("2026-11-25 17:00", attendance.values().get(3));
        assertTrue(services.forModule(payrollReport).findAll(payrollReport).stream()
                .anyMatch(row -> row.values().get(0).equals("Vietnamese Date Employee")
                        && row.values().get(3).startsWith("8,0")));
    }

    @Test
    void revenueReportIsProjectedFromSessionInvoices() {
        InMemoryModuleRepository repository = new InMemoryModuleRepository();
        FeatureServices services = new FeatureServices(repository);
        ModuleDefinition revenueReport = module("UC-19");

        repository.create("UC-17",
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
    void receivingAndStocktakeServicesAcceptTheirCatalogFormFields() {
        FeatureServices services = new FeatureServices();
        ModuleDefinition receivingModule = module("UC-12");
        ModuleDefinition stocktakeModule = module("UC-14");

        ModuleRow receipt = services.forModule(receivingModule).create(receivingModule,
                List.of("Test Supplier", "10/11/2026", "INV-1", "Shelf A", "many"));
        ModuleRow stocktake = services.forModule(stocktakeModule).create(stocktakeModule,
                List.of("Shelf A", "10/11/2026", "Admin", "Recount required"));

        assertEquals(receivingModule.formFields().size(), receipt.values().size());
        assertEquals(stocktakeModule.formFields().size(), stocktake.values().size());
    }

    private ModuleDefinition module(String id) {
        return ModuleCatalog.MODULES.stream()
                .filter(module -> module.id().equals(id))
                .findFirst()
                .orElseThrow();
    }
}
