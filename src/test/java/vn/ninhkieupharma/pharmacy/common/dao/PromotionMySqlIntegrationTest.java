package vn.ninhkieupharma.pharmacy.common.dao;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import vn.ninhkieupharma.pharmacy.common.auth.User;
import vn.ninhkieupharma.pharmacy.common.auth.UserRole;
import vn.ninhkieupharma.pharmacy.common.service.ModuleCatalog;
import vn.ninhkieupharma.pharmacy.domain.sales.PromotionsLoyaltyService;
import vn.ninhkieupharma.pharmacy.domain.sales.SaleLineRequest;
import vn.ninhkieupharma.pharmacy.domain.sales.SaleOrderRequest;
import vn.ninhkieupharma.pharmacy.domain.sales.SaleReceipt;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledIfEnvironmentVariable(named = MySqlTestSchema.ENABLED_ENV, matches = "true")
class PromotionMySqlIntegrationTest {
    private static final Path MIGRATION =
            Path.of("environment", "migrations", "V2__promotion_loyalty_tiers.sql");

    @Test
    void migrationBackfillsLegacyProgramsWithoutChangingHistoricalReferences() throws Exception {
        try (MySqlTestSchema schema = MySqlTestSchema.open()) {
            schema.executeScript("""
                    CREATE TABLE loyalty_programs (
                        loyalty_program_id BIGINT UNSIGNED PRIMARY KEY,
                        points_per_1000_currency DECIMAL(10, 4) NOT NULL,
                        currency_per_point DECIMAL(12, 2) NOT NULL
                    );
                    CREATE TABLE promotions (
                        promotion_id BIGINT UNSIGNED PRIMARY KEY,
                        promotion_code VARCHAR(30) NOT NULL,
                        points_multiplier DECIMAL(8, 2) NOT NULL,
                        starts_at DATETIME NOT NULL,
                        ends_at DATETIME NOT NULL,
                        is_active BOOLEAN NOT NULL,
                        CONSTRAINT chk_promotions_dates CHECK (starts_at <= ends_at)
                    );
                    CREATE TABLE historical_sales (
                        sale_id INT PRIMARY KEY,
                        promotion_id BIGINT UNSIGNED NOT NULL,
                        FOREIGN KEY (promotion_id) REFERENCES promotions (promotion_id)
                    );
                    INSERT INTO loyalty_programs VALUES (1, 2.0, 156.25), (2, 8.0, 2000);
                    INSERT INTO promotions VALUES
                        (10, 'LEGACY10', 2.25, '2026-01-02 08:15:00', '2026-01-08 17:30:00', TRUE),
                        (11, 'LEGACY11', 0, '2026-01-04 09:00:00', '2026-02-01 19:00:00', FALSE);
                    INSERT INTO historical_sales VALUES (1, 10);
                    """);
            schema.executeScript(MIGRATION);

            try (var connection = schema.connect();
                 var statement = connection.createStatement();
                 var rows = statement.executeQuery("""
                         SELECT p.promotion_code, p.starts_at, p.points_redemption_starts_at,
                                p.points_earning_ends_at, p.ends_at, p.is_active, p.redemption_vnd_per_point,
                                t.minimum_invoice_total_vnd, t.points_rate_percent, s.sale_id
                         FROM promotions p
                         JOIN promotion_point_tiers t ON t.promotion_id = p.promotion_id
                         LEFT JOIN historical_sales s ON s.promotion_id = p.promotion_id
                         ORDER BY p.promotion_id
                         """)) {
                assertTrue(rows.next());
                assertEquals("LEGACY10", rows.getString("promotion_code"));
                assertEquals(LocalDateTime.of(2026, 1, 2, 8, 15),
                        rows.getObject("points_redemption_starts_at", LocalDateTime.class));
                assertEquals(LocalDateTime.of(2026, 1, 8, 17, 30),
                        rows.getObject("points_earning_ends_at", LocalDateTime.class));
                assertEquals(new BigDecimal("156.25000000"), rows.getBigDecimal("redemption_vnd_per_point"));
                assertEquals(new BigDecimal("0.00"), rows.getBigDecimal("minimum_invoice_total_vnd"));
                assertEquals(new BigDecimal("0.45000000"), rows.getBigDecimal("points_rate_percent"));
                assertTrue(rows.getBoolean("is_active"));
                assertEquals(1, rows.getInt("sale_id"));

                assertTrue(rows.next());
                assertEquals("LEGACY11", rows.getString("promotion_code"));
                assertFalse(rows.getBoolean("is_active"));
                assertEquals(rows.getObject("starts_at", LocalDateTime.class),
                        rows.getObject("points_redemption_starts_at", LocalDateTime.class));
                assertEquals(rows.getObject("ends_at", LocalDateTime.class),
                        rows.getObject("points_earning_ends_at", LocalDateTime.class));
                assertEquals(new BigDecimal("0.00000001"), rows.getBigDecimal("points_rate_percent"));
                assertFalse(rows.next());
            }
            assertThrows(SQLException.class, () -> schema.execute(
                    "UPDATE promotions SET points_redemption_starts_at = '2026-01-01' WHERE promotion_id = 10"));
        }
    }

    @Test
    void checkoutSelectsEligibleProgramsAndRespectsSeparatePointsWindows() throws Exception {
        try (MySqlTestSchema schema = MySqlTestSchema.withCurrentSchema()) {
            schema.executeScript("""
                    UPDATE promotions SET is_active = FALSE;
                    UPDATE products SET selling_price = 12000 WHERE product_id = 1;
                    UPDATE inventory_batches
                    SET expiry_date = '2099-12-31', quantity_on_hand = 20
                    WHERE product_id = 1;
                    """);
            schema.runInChild(CheckoutScenario.class);
        }
    }

    public static final class CheckoutScenario {
        public static void main(String[] args) {
            String schema = System.getenv("PHARMACY_DB_NAME");
            if (schema == null || !schema.matches("pharmacy_test_[a-f0-9]{32}")) {
                throw new IllegalStateException("The promotion scenario requires a disposable test database.");
            }
            RelationalFeatureModuleDAO repository =
                    new RelationalFeatureModuleDAO(new User(1, "admin_demo", "", UserRole.ADMIN, true));
            SalesCheckoutDAO sales = new SalesCheckoutDAO();
            LocalDate today = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));
            LocalDate yesterday = today.minusDays(1);
            LocalDate tomorrow = today.plusDays(1);
            LocalDate end = today.plusDays(3);
            var first = repository.create("UC-18", promotion(
                    "CURRENT-FIRST", "20", today, tomorrow, tomorrow, end, "1800", "0:0.05;8500:0.1", "ACTIVE"));
            var tied = repository.create("UC-18", promotion(
                    "CURRENT-TIE", "20", today, today, tomorrow, end, "2000", "0:0.5", "ACTIVE"));
            var completedAccrual = repository.create("UC-18", promotion(
                    "ACCRUAL-ENDED", "15", yesterday, yesterday, yesterday, end, "1800", "0:0.5", "ACTIVE"));
            repository.create("UC-18", promotion(
                    "EXPIRED", "90", yesterday, yesterday, yesterday, yesterday, "2000", "0:0.5", "ACTIVE"));
            repository.create("UC-18", promotion(
                    "FUTURE", "95", tomorrow, tomorrow, end, end, "2000", "0:0.5", "ACTIVE"));
            var disabled = repository.create("UC-18", promotion(
                    "DISABLED", "99", today, today, tomorrow, end, "2000", "0:0.5", "INACTIVE"));
            repository.create("UC-18", promotion(
                    "POINTS-ONLY", "0", today, today, tomorrow, end, "1750", "0:0.2", "ACTIVE"));

            var product = sales.findSaleProducts().stream()
                    .filter(row -> "SP0001".equals(row.code()))
                    .findFirst().orElseThrow();
            var customer = repository.findCustomersAvailableForSale().stream()
                    .filter(row -> row.id().getLeastSignificantBits() == 1)
                    .findFirst().orElseThrow();
            var request = new SaleOrderRequest(Optional.of(customer.id()),
                    List.of(new SaleLineRequest(product.id(), BigDecimal.ONE, "", 1)),
                    "", null, "", "", "", "", 1, List.of());
            var pending = sales.createPendingSale(2, request);
            java.util.function.Supplier<SaleReceipt> quote = () ->
                    sales.quotePendingSale(2, pending.id(), pending.revision(), request).amounts();

            assertQuote(quote.get(), "CURRENT-FIRST", "2400.00", "1000.00", 8);
            var module = ModuleCatalog.MODULES.stream().filter(row -> row.id().equals("UC-18"))
                    .findFirst().orElseThrow();
            var promotions = new PromotionsLoyaltyService(repository);
            promotions.delete(module, first.id());
            assertQuote(quote.get(), "CURRENT-TIE", "2400.00", "2000.00", 38);
            promotions.delete(module, tied.id());
            assertQuote(quote.get(), "ACCRUAL-ENDED", "1800.00", "1800.00", 8);
            promotions.delete(module, completedAccrual.id());
            assertQuote(quote.get(), "POINTS-ONLY", "0.00", "1750.00", 20);

            promotions.update(module, disabled.id(), promotion("DISABLED", "30", today, today,
                    tomorrow, end, "2000", "0:0.5", "ACTIVE"));
            assertQuote(quote.get(), "DISABLED", "3600.00", "2000.00", 32);
        }

        private static void assertQuote(SaleReceipt receipt, String code, String saleDiscount,
                String pointsDiscount, long pointsEarned) {
            assertEquals(code, receipt.promotionCode());
            assertEquals(new BigDecimal(saleDiscount), receipt.promotionDiscount());
            assertEquals(new BigDecimal(pointsDiscount), receipt.pointsDiscount());
            assertEquals(pointsEarned, receipt.pointsEarned());
        }

        private static List<String> promotion(String code, String discount,
                LocalDate start, LocalDate redemptionStart, LocalDate accrualEnd, LocalDate end,
                String redemptionValue, String tiers, String status) {
            return List.of(code, code, discount, start.toString(), redemptionStart.toString(), accrualEnd.toString(),
                    end.toString(), redemptionValue, tiers, status);
        }
    }
}
