package vn.ninhkieupharma.pharmacy.ui;

import org.junit.jupiter.api.Test;
import vn.ninhkieupharma.pharmacy.common.auth.UserRole;
import org.junit.jupiter.api.io.TempDir;
import vn.ninhkieupharma.pharmacy.common.service.Localization;
import vn.ninhkieupharma.pharmacy.common.service.ModuleCatalog;
import vn.ninhkieupharma.pharmacy.common.model.ModuleRow;
import vn.ninhkieupharma.pharmacy.common.model.ModuleDefinition;
import vn.ninhkieupharma.pharmacy.domain.sales.SalesReport;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PageRendererTest {
    @Test
    void parsesPositiveCheckoutQuantitiesAtSchemaPrecision() {
        assertEquals(new BigDecimal("1.250"), PageRenderer.parseCheckoutQuantity("1,250"));
        for (String invalid : List.of("0", "-1", "1.2345", "1e2", "1000000000")) {
            assertThrows(IllegalArgumentException.class, () -> PageRenderer.parseCheckoutQuantity(invalid));
        }
    }

    @Test
    void customerSuggestionsNarrowAsPhoneDigitsAreTyped() {
        ModuleRow customer = new ModuleRow(
                UUID.randomUUID(),
                List.of("Nguyễn Thị Mai", "0912345678", "", "", "125"));

        assertTrue(PageRenderer.matchesSalesCustomerQuery(customer, "0"));
        assertTrue(PageRenderer.matchesSalesCustomerQuery(customer, "09"));
        assertFalse(PageRenderer.matchesSalesCustomerQuery(customer, "08"));
    }

    @Test
    void customerSuggestionsMatchNamesWithoutCaseSensitivity() {
        ModuleRow customer = new ModuleRow(
                UUID.randomUUID(),
                List.of("Nguyễn Thị Mai", "0912345678", "", "", "125"));

        assertTrue(PageRenderer.matchesSalesCustomerQuery(customer, "nguyễn"));
    }

    @Test
    void catalogMutationsAreAdminOnly() {
        for (String moduleId : List.of("UC-07", "UC-08", "UC-09", "UC-13")) {
            assertTrue(PageRenderer.canWriteModule(moduleId, UserRole.ADMIN));
            assertFalse(PageRenderer.canWriteModule(moduleId, UserRole.STAFF));
        }
    }

    @Test
    void productTableAndFormIncludeMinimumAge() {
        ModuleDefinition products = ModuleCatalog.MODULES.stream()
                .filter(module -> module.id().equals("UC-08"))
                .findFirst()
                .orElseThrow();

        assertEquals(10, products.columns().size());
        assertEquals("Tuổi tối thiểu", products.columns().get(9));
        assertEquals(products.columns(), products.formFields());
    }

    @Test
    void productFiltersMatchFieldsAndInclusivePriceRange() {
        ModuleRow product = new ModuleRow(UUID.randomUUID(),
                List.of("SP0001", "Paracetamol 500mg", "Thuốc giảm đau", "Viên nén", "Vỉ",
                        "15000.00", "No", "ACTIVE", "Dược phẩm Hậu Giang", "0"));
        List<String> columnQueries = List.of("", "para", "", "", "", "", "", "", "HẬU", "");

        assertTrue(PageRenderer.matchesProductFilters(
                product, "", columnQueries, new BigDecimal("15000"), new BigDecimal("15000")));
        assertFalse(PageRenderer.matchesProductFilters(
                product, "", columnQueries, new BigDecimal("15000.01"), null));
        assertFalse(PageRenderer.matchesProductFilters(
                product, "", List.of("", "para", "", "", "", "", "", "INACTIVE", "", ""),
                null, null));
        assertTrue(PageRenderer.matchesProductFilters(
                product, "", List.of("", "", "", "", "", "", "No", "", "", ""), null, null));
        assertFalse(PageRenderer.matchesProductFilters(
                product, "", List.of("", "", "", "", "", "", "Yes", "", "", ""), null, null));
        assertEquals(new BigDecimal("15000"), PageRenderer.parseProductPriceBound("15.000"));
        assertEquals(new BigDecimal("20000"), PageRenderer.parseProductPriceBound("20,000"));
    }

    @Test
    void reportFiltersMatchEachColumnIndependently() {
        ModuleRow reportRow = new ModuleRow(UUID.randomUUID(), List.of(
                "2026-Q2", "Thực phẩm bổ sung", "3 SP (2 dạng / 1 NCC)", "50.0%",
                "Có thể thay thế cơ bản"));

        assertTrue(PageRenderer.matchesReportFilters(
                reportRow, "q2", List.of("", "", "3 SP", "", "")));
        assertTrue(PageRenderer.matchesReportFilters(
                reportRow, "", List.of("", "", "", "50.0", "")));
        assertFalse(PageRenderer.matchesReportFilters(
                reportRow, "", List.of("", "", "50.0", "", "")));
    }

    @Test
    void trendChartDataGroupsCountsAndFillsMissingPeriodsWithZero() {
        List<ModuleRow> rows = List.of(
                reportRow("2026-Q2", "Ngành A", "2 SP"),
                reportRow("2026-Q1", "Ngành A", "1 SP"),
                reportRow("2026-Q1", "Ngành B", "3 SP"));

        PageRenderer.TrendChartData data = PageRenderer.createTrendChartData(rows, 0, 1);

        assertEquals(List.of("2026-Q1", "2026-Q2"), data.periods());
        assertEquals(List.of("Ngành A", "Ngành B"), data.dimensions());
        assertEquals(List.of(1, 2), data.series().get("Ngành A"));
        assertEquals(List.of(3, 0), data.series().get("Ngành B"));
        assertEquals(List.of(2, 0), data.latestPeriodTotals());
    }

    @Test
    void onlyAdminsCanConfirmGoodsReceiptsAndStocktakes() {
        for (String moduleId : List.of("UC-12", "UC-14")) {
            assertTrue(PageRenderer.canConfirmInventoryWorkflow(moduleId, UserRole.ADMIN));
            assertFalse(PageRenderer.canConfirmInventoryWorkflow(moduleId, UserRole.STAFF));
        }
        assertFalse(PageRenderer.canConfirmInventoryWorkflow("UC-15", UserRole.ADMIN));
    }

    @Test
    void staffCanManageCustomersButOnlyAdminsCanArchiveOrConfigureSales() {
        assertTrue(PageRenderer.canWriteModule("UC-16", UserRole.STAFF));
        assertTrue(PageRenderer.canDeleteModule("UC-16", UserRole.ADMIN));
        assertFalse(PageRenderer.canDeleteModule("UC-16", UserRole.STAFF));
        assertFalse(PageRenderer.canWriteModule("UC-17", UserRole.ADMIN));
        assertFalse(PageRenderer.canWriteModule("UC-17", UserRole.STAFF));
        assertTrue(PageRenderer.canWriteModule("UC-18", UserRole.ADMIN));
        assertFalse(PageRenderer.canWriteModule("UC-18", UserRole.STAFF));
        assertFalse(PageRenderer.canWriteModule("UC-19", UserRole.STAFF));
    }

    private ModuleRow reportRow(String period, String dimension, String count) {
        return new ModuleRow(UUID.randomUUID(),
                List.of(period, dimension, count, "50.0%", "Đánh giá"));
    }

    @Test
    void inventoryCsvReportEscapesHeadersAndValues(@TempDir Path tempDir)
            throws Exception {
        ModuleDefinition report = new ModuleDefinition(
                "UC-15", "Inventory", "Inventory", "", "Tạo báo cáo",
                List.of("Thời điểm", "Sản phẩm / lô", "Loại biến động"),
                List.of(), true, true);
        ModuleRow row = new ModuleRow(UUID.randomUUID(),
                List.of("2026-11-23", "Paracetamol, lô \"A\"", "NHAP"));
        Path file = tempDir.resolve("inventory.csv");

        PageRenderer.writeCsvReport(file, report, List.of(row));

        String csv = Files.readString(file);
        assertTrue(csv.contains("Thời điểm,Sản phẩm / lô,Loại biến động"));
        assertTrue(csv.contains("\"Paracetamol, lô \"\"A\"\"\""));
    }

    @Test
    void salesReportCsvKeepsTheSameRowsAndTotalsUsedForPrinting(@TempDir Path tempDir)
            throws Exception {
        LocalDate january = LocalDate.of(2026, 1, 1);
        var report = new SalesReport(SalesReport.Period.QUARTER, january, List.of(
                new SalesReport.Row(january, new SalesReport.Totals(1,
                        new BigDecimal("100.50"), new BigDecimal("5.25"))),
                new SalesReport.Row(january.plusMonths(1), new SalesReport.Totals(2,
                        new BigDecimal("303.25"), new BigDecimal("8.00")))));
        ModuleDefinition module = ModuleCatalog.MODULES.stream()
                .filter(candidate -> candidate.id().equals("UC-19"))
                .findFirst().orElseThrow();
        var printRows = PageRenderer.salesReportOutput(report, report.totals());
        Path file = tempDir.resolve("sales.csv");

        PageRenderer.writeCsvReport(file, module, printRows);

        List<String> csv = Files.readAllLines(file);
        assertEquals(List.of(
                "2026-01,1,100.50,5.25,100.50",
                "2026-02,2,303.25,8.00,151.63",
                Localization.text("Tổng cộng") + ",3,403.75,13.25,134.58"),
                csv.subList(1, csv.size()));
    }

    @Test
    void inventoryMovementTypesFollowTheSelectedLanguage() {
        if (Localization.isEnglish()) {
            Localization.toggleLanguage();
        }
        assertTrue(Localization.movementTypeText("RECEIPT").equals("Nhập hàng"));
        assertTrue(Localization.movementTypeText("ADJUSTMENT").equals("Điều chỉnh"));

        Localization.toggleLanguage();
        assertTrue(Localization.movementTypeText("RECEIPT").equals("Goods Receipt"));
        assertTrue(Localization.movementTypeText("ADJUSTMENT").equals("Adjustment"));
        Localization.toggleLanguage();
    }

    @Test
    void inventoryFiltersMatchVietnameseAndEnglishMovementTypes() {
        List<String> row = List.of(
                "2026-11-23 00:00",
                "Paracetamol / LOT-1",
                "RECEIPT",
                "200",
                "Nhập hàng PN-1",
                "2026-12-01",
                "KE-A1 · Ke A tầng 1");

        assertTrue(PageRenderer.matchesInventoryRow(
                row, "", null, null, "ALL", "ALL", "ALL", LocalDate.of(2026, 11, 23)));
        assertTrue(PageRenderer.matchesInventoryRow(
                row, "", LocalDate.of(2026, 11, 23), LocalDate.of(2026, 11, 23),
                "RECEIPT", "EXPIRING", "KE-A1 · Ke A tầng 1",
                LocalDate.of(2026, 11, 23)));
        assertFalse(PageRenderer.matchesInventoryRow(
                row, "", null, null, "SALE", "ALL", "ALL",
                LocalDate.of(2026, 11, 23)));
    }

    @Test
    void inventorySearchMatchesTheVisibleMovementTypeLabel() {
        List<String> row = List.of(
                "2026-11-23 00:00",
                "Paracetamol / LOT-1",
                "RECEIPT",
                "2",
                "Nhập hàng PN-1",
                "2027-01-01",
                "KE-A1 · Ke A tầng 1");

        assertTrue(PageRenderer.matchesInventoryRow(
                row, Localization.movementTypeText("RECEIPT"), null, null,
                "ALL", "ALL", "ALL", LocalDate.of(2026, 11, 23)));
    }

}
