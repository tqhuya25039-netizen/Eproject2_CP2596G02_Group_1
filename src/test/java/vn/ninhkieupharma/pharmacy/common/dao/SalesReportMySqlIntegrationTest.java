package vn.ninhkieupharma.pharmacy.common.dao;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import vn.ninhkieupharma.pharmacy.common.auth.User;
import vn.ninhkieupharma.pharmacy.common.auth.UserRole;
import vn.ninhkieupharma.pharmacy.domain.sales.SalesReport;
import vn.ninhkieupharma.pharmacy.domain.sales.SalesService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledIfEnvironmentVariable(named = MySqlTestSchema.ENABLED_ENV, matches = "true")
class SalesReportMySqlIntegrationTest {
    private static final LocalDate FROM = LocalDate.of(2031, 6, 2);
    private static final LocalDate TO = LocalDate.of(2031, 6, 4);

    @Test
    void filtersCompletedSalesAndAllocatesMixedInvoicesWithoutDoubleCounting() throws Exception {
        try (MySqlTestSchema schema = MySqlTestSchema.withCurrentSchema()) {
            schema.executeScript("""
                    INSERT INTO employees (employee_id, employee_code, full_name, hire_date)
                    VALUES (9001, 'REPORT-A', 'First seller', '2030-01-01'),
                           (9002, 'REPORT-B', 'Second seller', '2030-01-01');
                    INSERT INTO product_categories (category_id, category_name)
                    VALUES (9001, 'Report category A'), (9002, 'Report category B');
                    INSERT INTO products (product_id, category_id, product_code, product_name, unit_name, is_active)
                    VALUES (9001, 9001, 'REPORT-A', 'First medicine', 'box', 0),
                           (9002, 9002, 'REPORT-B', 'Second medicine', 'box', 1),
                           (9003, 9001, 'REPORT-C', 'Third medicine', 'box', 1);
                    INSERT INTO sales (sale_id, sale_code, employee_id, sold_at, subtotal, discount_amount,
                                       total_amount, status)
                    VALUES (9001, 'REPORT-1', 9001, '2031-06-02 00:00:00', 30, 0.01, 29.99, 'COMPLETED'),
                           (9002, 'REPORT-2', 9002, '2031-06-04 23:59:59', 15, 3, 12, 'COMPLETED'),
                           (9003, 'REPORT-3', 9001, '2031-06-03 00:00:00', 10, 0, 10, 'PENDING'),
                           (9004, 'REPORT-4', 9001, '2031-06-03 00:00:00', 10, 0, 10, 'VOIDED'),
                           (9005, 'REPORT-5', 9001, '2031-06-01 23:59:59', 10, 0, 10, 'COMPLETED'),
                           (9006, 'REPORT-6', 9001, '2031-06-05 00:00:00', 10, 0, 10, 'COMPLETED');
                    INSERT INTO sale_items (sale_item_id, sale_id, product_id, quantity, unit_price,
                                            line_discount, line_total)
                    VALUES (10001, 9001, 9001, 1, 10, 0, 10), (10002, 9001, 9001, 1, 10, 0, 10),
                           (10003, 9001, 9003, 1, 10, 0, 10), (10004, 9002, 9001, 1, 10, 2, 8),
                           (10005, 9002, 9002, 1, 5, 0, 5), (10006, 9003, 9001, 1, 10, 0, 10),
                           (10007, 9004, 9001, 1, 10, 0, 10), (10008, 9005, 9001, 1, 10, 0, 10),
                           (10009, 9006, 9001, 1, 10, 0, 10);
                    """);
            schema.runInChild(ReportChecks.class);
        }
    }

    public static final class ReportChecks {
        public static void main(String[] args) {
            User admin = new User(1, "report_fixture", "", UserRole.ADMIN, true);
            SalesService service = new SalesService(new RelationalFeatureModuleDAO(admin));

            SalesReport.FilterOptions options = service.salesReportOptions();
            assertTrue(options.employees().contains(new SalesReport.Option(9001, "First seller (REPORT-A)")));
            assertTrue(options.products().contains(new SalesReport.Option(9001, "First medicine (REPORT-A)")));
            assertTrue(options.categories().contains(new SalesReport.Option(9001, "Report category A")));

            SalesReport all = service.salesReport(new SalesReport.Filter(FROM, TO, null, null, null));
            assertEquals(SalesReport.Grouping.DAILY, all.grouping());
            assertEquals(List.of(FROM, TO), all.rows().stream().map(SalesReport.Row::start).toList());
            assertTotals(2, "41.99", "3.01", all.totals());
            assertTotals(1, "29.99", "0.01", breakdown(all.employees(), 9001));
            assertTotals(2, "27.37", "2.63", breakdown(all.products(), 9001));
            assertTotals(2, "37.37", "2.63", breakdown(all.categories(), 9001));

            SalesReport category = service.salesReport(new SalesReport.Filter(FROM, TO, null, null, 9001L));
            assertTotals(2, "37.37", "2.63", category.totals());
            assertEquals(2, category.products().size());

            SalesReport filtered = service.salesReport(new SalesReport.Filter(FROM, TO, 9002L, 9001L, 9001L),
                    SalesReport.Period.QUARTER);
            assertEquals(SalesReport.Grouping.MONTHLY, filtered.grouping());
            assertEquals(List.of(FROM.withDayOfMonth(1)), filtered.rows().stream().map(SalesReport.Row::start).toList());
            assertTotals(1, "7.38", "2.62", filtered.totals());
            assertTotals(1, "7.38", "2.62", breakdown(filtered.employees(), 9002));

            SalesReport noMatch = service.salesReport(new SalesReport.Filter(FROM, TO, null, 9001L, 9002L));
            assertTrue(noMatch.rows().isEmpty());
            assertTrue(noMatch.products().isEmpty());
        }
    }

    private static SalesReport.Totals breakdown(List<SalesReport.Breakdown> rows, long id) {
        return rows.stream().filter(row -> row.option().id() == id).findFirst().orElseThrow().totals();
    }

    private static void assertTotals(long orders, String revenue, String discount, SalesReport.Totals actual) {
        assertEquals(orders, actual.orderCount());
        assertEquals(0, new BigDecimal(revenue).compareTo(actual.revenue()));
        assertEquals(0, new BigDecimal(discount).compareTo(actual.discount()));
    }
}
