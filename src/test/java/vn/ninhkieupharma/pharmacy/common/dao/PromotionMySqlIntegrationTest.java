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
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledIfEnvironmentVariable(named = MySqlTestSchema.ENABLED_ENV, matches = "true")
class PromotionMySqlIntegrationTest {
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
