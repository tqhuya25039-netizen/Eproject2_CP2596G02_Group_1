package vn.ninhkieupharma.pharmacy.common.dao;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import vn.ninhkieupharma.pharmacy.common.auth.User;
import vn.ninhkieupharma.pharmacy.common.auth.UserRole;
import vn.ninhkieupharma.pharmacy.common.model.ModuleRow;
import vn.ninhkieupharma.pharmacy.common.service.Localization;
import vn.ninhkieupharma.pharmacy.domain.sales.SaleLineRequest;
import vn.ninhkieupharma.pharmacy.domain.sales.SaleOrderRequest;
import vn.ninhkieupharma.pharmacy.domain.sales.SalePendingOrder;
import vn.ninhkieupharma.pharmacy.domain.sales.SaleQuote;
import vn.ninhkieupharma.pharmacy.domain.sales.SaleReceipt;
import vn.ninhkieupharma.pharmacy.domain.sales.SaleStockShortage;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledIfEnvironmentVariable(named = MySqlTestSchema.ENABLED_ENV, matches = "true")
class SalesCheckoutMySqlIntegrationTest {
    private static final long FIRST_STAFF = 2;
    private static final long SECOND_STAFF = 3;
    private static final long ROW_ID_PREFIX = 0x4e4b504841524d41L;
    private static final UUID CUSTOMER = new UUID(ROW_ID_PREFIX, 1);
    private static final UUID PRODUCT = new UUID(ROW_ID_PREFIX, 1);
    private static final long EARLY_BATCH = 1;
    private static final long LATE_BATCH = 3;
    private static final long EXPIRED_BATCH = 4;
    private static final long PROMOTION = 2;

    private MySqlTestSchema schema;
    private SalesCheckoutDAO checkout;

    @BeforeEach
    void setUp() throws Exception {
        schema = MySqlTestSchema.withCurrentSchema();
        checkout = new SalesCheckoutDAO(schema::connect);
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        schema.executeScript("""
                INSERT INTO users (user_id, role_id, username, password_hash, is_active)
                VALUES (3, 2, 'second_staff_fixture', 'test-fixture', 1);
                INSERT INTO employees (employee_id, user_id, employee_code, full_name, hire_date)
                VALUES (2, 3, 'NV0002', 'Second checkout staff', '%1$s');
                UPDATE customers SET loyalty_points = 20 WHERE customer_id = 1;
                UPDATE products SET selling_price = 5000 WHERE product_id = 1;
                UPDATE inventory_batches SET expiry_date = '%2$s', quantity_on_hand = 3 WHERE batch_id = 1;
                INSERT INTO inventory_batches (batch_id, product_id, location_id, batch_code, expiry_date, quantity_on_hand)
                VALUES (3, 1, 1, 'LATE-LOT', '%3$s', 10), (4, 1, 1, 'EXPIRED-LOT', '%4$s', 99);
                UPDATE promotions SET is_active = 0;
                INSERT INTO promotions (promotion_id, promotion_code, promotion_name, discount_percent,
                    starts_at, points_redemption_starts_at, points_earning_ends_at, ends_at,
                    redemption_vnd_per_point, is_active)
                VALUES (2, 'FIXTURE10', 'Checkout fixture', 10, '%4$s', '%4$s', '%3$s', '%3$s', 1000, 1);
                INSERT INTO promotion_point_tiers
                    (promotion_id, minimum_invoice_total_vnd, points_rate_percent)
                VALUES (2, 0, 0.05);
                """.formatted(today, today.plusDays(2), today.plusDays(90), today.minusDays(1)));
    }

    @AfterEach
    void cleanUp() throws SQLException {
        if (schema != null) {
            schema.close();
        }
    }

    @Test
    void completesAnEditedPendingOrderWithFefoAndLoyaltyPoints() throws Exception {
        SaleOrderRequest original = order(0, line(1, 1));
        SalePendingOrder draft = checkout.createPendingSale(FIRST_STAFF, original);
        SaleOrderRequest edited = order(3, line(2, 1), line(3, 10));
        SalePendingOrder pending = checkout.updatePendingSale(FIRST_STAFF, draft.id(), draft.revision(), edited);
        assertEquals(draft.revision() + 1, pending.revision());
        assertEquals(3, checkout.findPendingSale(pending.id()).request().requestedPoints());
        assertEquals("PENDING", status(pending));
        assertUnchangedStockAndPoints();

        SaleQuote quote = quote(pending, edited);
        assertEquals(new BigDecimal("19500.00"), quote.amounts().total());
        SaleReceipt receipt = checkout.completePendingSale(FIRST_STAFF, pending.id(), pending.revision(), edited, quote);

        assertEquals(quote.amounts(), receipt);
        assertEquals("COMPLETED", status(pending));
        assertEquals(2, scalar("SELECT COUNT(*) FROM sale_items WHERE sale_id = ?", id(pending)));
        assertEquals(2, movementCount(pending));
        assertEquals(1, stock(EARLY_BATCH));
        assertEquals(7, stock(LATE_BATCH));
        assertEquals(99, stock(EXPIRED_BATCH));
        assertEquals(3, receipt.pointsRedeemed());
        assertEquals(9, receipt.pointsEarned());
        assertEquals(26, points());
        assertEquals(3, scalar("SELECT points_redeemed FROM sales WHERE sale_id = ?", id(pending)));
        assertEquals(9, scalar("SELECT points_earned FROM sales WHERE sale_id = ?", id(pending)));
        assertThrows(IllegalStateException.class,
                () -> checkout.cancelPendingSale(FIRST_STAFF, pending.id(), pending.revision()));
        schema.runInChild(LoyaltyHistoryCheck.class, receipt.saleCode());
    }

    @Test
    void previewsFefoAfterAccountingForOtherLinesAndWritesNothing() throws SQLException {
        long movementCountBefore = scalar("SELECT COUNT(*) FROM inventory_movements WHERE movement_id >= ?", 1);
        assertTrue(checkout.previewSaleStock(List.of(line(3, 1), line(10, 10))).isEmpty());
        List<SaleStockShortage> shortages = checkout.previewSaleStock(List.of(line(6, 1), line(8, 10)));
        assertEquals(1, shortages.size());
        SaleStockShortage shortage = shortages.getFirst();
        assertEquals(new BigDecimal("6"), shortage.requested());
        assertEquals(new BigDecimal("5.000"), shortage.available());
        assertEquals(LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")), shortage.requiredExpiry());
        assertThrows(IllegalStateException.class,
                () -> checkout.createPendingSale(FIRST_STAFF, order(0, line(6, 1), line(8, 10))));

        assertEquals(1, scalar("SELECT COUNT(*) FROM sales WHERE customer_id = ?", CUSTOMER.getLeastSignificantBits()));
        assertEquals(movementCountBefore, scalar("SELECT COUNT(*) FROM inventory_movements WHERE movement_id >= ?", 1));
        assertUnchangedStockAndPoints();
    }

    @Test
    void cancelsOnlyCurrentRevisionWithoutChangingStockOrPoints() throws SQLException {
        SaleOrderRequest request = order(2, line(1, 1));
        SalePendingOrder draft = checkout.createPendingSale(FIRST_STAFF, request);
        SalePendingOrder edited = checkout.updatePendingSale(
                FIRST_STAFF, draft.id(), draft.revision(), order(2, line(2, 1)));
        assertThrows(IllegalStateException.class,
                () -> checkout.cancelPendingSale(SECOND_STAFF, draft.id(), draft.revision()));

        checkout.cancelPendingSale(SECOND_STAFF, edited.id(), edited.revision());

        assertEquals("VOIDED", status(edited));
        assertEquals(1, scalar("SELECT COUNT(*) FROM sale_items WHERE sale_id = ?", id(edited)));
        assertEquals(0, movementCount(edited));
        assertUnchangedStockAndPoints();
        assertThrows(IllegalStateException.class, () -> quote(edited, request));
    }

    @Test
    void rollsBackOrderItemsStockMovementsAndPointsWhenFinalPointWriteFails() throws SQLException {
        SalePendingOrder draft = checkout.createPendingSale(FIRST_STAFF, order(0, line(1, 1)));
        SalePendingOrder before = checkout.findPendingSale(draft.id());
        SaleOrderRequest edited = order(3, line(2, 1), line(3, 10));
        SaleQuote quote = quote(draft, edited);
        schema.execute("""
                CREATE TRIGGER checkout_test_reject_loyalty BEFORE UPDATE ON customers FOR EACH ROW
                SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'checkout rollback fixture'
                """);

        DataAccessException failure = assertThrows(DataAccessException.class,
                () -> checkout.completePendingSale(FIRST_STAFF, draft.id(), draft.revision(), edited, quote));

        SQLException sqlFailure = assertInstanceOf(SQLException.class, failure.getCause());
        assertEquals("45000", sqlFailure.getSQLState());
        assertTrue(sqlFailure.getMessage().contains("checkout rollback fixture"));
        assertEquals(before, checkout.findPendingSale(draft.id()));
        assertEquals("PENDING", status(draft));
        assertEquals(1, scalar("SELECT COUNT(*) FROM sale_items WHERE sale_id = ?", id(draft)));
        assertEquals(0, movementCount(draft));
        assertUnchangedStockAndPoints();

        schema.execute("DROP TRIGGER checkout_test_reject_loyalty");
        assertEquals(quote.amounts(),
                checkout.completePendingSale(FIRST_STAFF, draft.id(), draft.revision(), edited, quote));
        assertEquals("COMPLETED", status(draft));
    }

    @Test
    void allowsOnlyOneOfTwoEmployeesToCompleteTheSameRevision() throws Exception {
        SaleOrderRequest request = order(3, line(2, 1), line(3, 10));
        SalePendingOrder pending = checkout.createPendingSale(FIRST_STAFF, request);
        SaleQuote quote = quote(pending, request);
        CyclicBarrier start = new CyclicBarrier(2);
        var pool = Executors.newFixedThreadPool(2);
        try {
            var first = pool.submit(() -> raceCompletion(start, FIRST_STAFF, pending, request, quote));
            var second = pool.submit(() -> raceCompletion(start, SECOND_STAFF, pending, request, quote));
            assertEquals(1, first.get(15, TimeUnit.SECONDS) + second.get(15, TimeUnit.SECONDS));
        } finally {
            pool.shutdownNow();
        }

        assertEquals("COMPLETED", status(pending));
        assertEquals(2, movementCount(pending));
        assertEquals(1, stock(EARLY_BATCH));
        assertEquals(7, stock(LATE_BATCH));
        assertEquals(26, points());
    }

    @Test
    void refusesBlockedCustomersIncludingThoseBlockedAfterAQuote() throws SQLException {
        SaleOrderRequest request = order(2, line(1, 1));
        SalePendingOrder pending = checkout.createPendingSale(FIRST_STAFF, request);
        SaleQuote quote = quote(pending, request);
        schema.execute("UPDATE customers SET is_blocked = 1, block_reason = 'fixture' WHERE customer_id = 1");

        assertThrows(IllegalStateException.class, () -> checkout.createPendingSale(FIRST_STAFF, request));
        assertThrows(IllegalStateException.class, () -> quote(pending, request));
        assertThrows(IllegalStateException.class,
                () -> checkout.completePendingSale(FIRST_STAFF, pending.id(), pending.revision(), request, quote));

        assertEquals("PENDING", status(pending));
        assertEquals(0, movementCount(pending));
        assertUnchangedStockAndPoints();
    }

    @Test
    void deletesOnlyCustomersWithoutOrdersAndRequiresAdmin() throws Exception {
        schema.executeScript("""
                INSERT INTO customers (customer_id, customer_code, full_name) VALUES
                    (2, 'UNUSED', 'Customer without an order'),
                    (3, 'PENDING', 'Customer with a pending order'),
                    (4, 'VOIDED', 'Customer with a voided order');
                INSERT INTO customer_allergies (customer_id, allergen_name) VALUES
                    (2, 'Test allergen'), (3, 'Test allergen'), (4, 'Test allergen');
                """);
        SalePendingOrder pending = checkout.createPendingSale(FIRST_STAFF, orderForCustomer(3, 0, line(1, 1)));
        SalePendingOrder voided = checkout.createPendingSale(FIRST_STAFF, orderForCustomer(4, 0, line(1, 1)));
        checkout.cancelPendingSale(FIRST_STAFF, voided.id(), voided.revision());

        schema.runInChild(CustomerDeletionCheck.class);

        assertCustomerAndAllergyCount(2, 0);
        for (long customerId : new long[]{1, 3, 4}) {
            assertCustomerAndAllergyCount(customerId, 1);
        }
        assertEquals("COMPLETED", query("SELECT status FROM sales WHERE customer_id = ?", 1));
        assertEquals("PENDING", status(pending));
        assertEquals("VOIDED", status(voided));
    }

    @Test
    void ignoresAnExpiredProgramEvenWhenItHasAHigherDiscount() throws SQLException {
        LocalDate yesterday = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")).minusDays(1);
        schema.execute("""
                UPDATE promotions SET is_active = 1, discount_percent = 90,
                    starts_at = '%1$s', points_redemption_starts_at = '%1$s',
                    points_earning_ends_at = '%1$s', ends_at = '%1$s'
                WHERE promotion_id = 1
                """.formatted(yesterday));
        SaleOrderRequest request = order(0, line(2, 1));
        SalePendingOrder pending = checkout.createPendingSale(FIRST_STAFF, request);
        SaleQuote quote = quote(pending, request);
        assertEquals("FIXTURE10", quote.amounts().promotionCode());
        assertEquals(new BigDecimal("9000.00"), quote.amounts().total());

        checkout.completePendingSale(FIRST_STAFF, pending.id(), pending.revision(), request, quote);

        assertEquals(PROMOTION, scalar("SELECT promotion_id FROM sales WHERE sale_id = ?", id(pending)));
    }

    private int raceCompletion(
            CyclicBarrier start, long staff, SalePendingOrder pending, SaleOrderRequest request, SaleQuote quote)
            throws Exception {
        start.await(10, TimeUnit.SECONDS);
        try {
            checkout.completePendingSale(staff, pending.id(), pending.revision(), request, quote);
            return 1;
        } catch (IllegalStateException stale) {
            assertEquals(Localization.text("Đơn đang chờ đã thay đổi hoặc đã đóng. Hãy làm mới danh sách và mở lại đơn."),
                    stale.getMessage());
            return 0;
        }
    }

    private SaleQuote quote(SalePendingOrder pending, SaleOrderRequest request) {
        return checkout.quotePendingSale(FIRST_STAFF, pending.id(), pending.revision(), request);
    }

    private static SaleOrderRequest order(long points, SaleLineRequest... lines) {
        return orderForCustomer(CUSTOMER.getLeastSignificantBits(), points, lines);
    }

    private static SaleOrderRequest orderForCustomer(long customerId, long points, SaleLineRequest... lines) {
        return new SaleOrderRequest(Optional.of(new UUID(ROW_ID_PREFIX, customerId)), List.of(lines), "",
                null, "", "", "", "", points, List.of());
    }

    private static SaleLineRequest line(int quantity, int treatmentDays) {
        return new SaleLineRequest(PRODUCT, BigDecimal.valueOf(quantity), "", treatmentDays);
    }

    private static long id(SalePendingOrder order) {
        return order.id().getLeastSignificantBits();
    }

    private String status(SalePendingOrder order) throws SQLException {
        return query("SELECT status FROM sales WHERE sale_id = ?", id(order));
    }

    private long movementCount(SalePendingOrder order) throws SQLException {
        return scalar("SELECT COUNT(*) FROM inventory_movements WHERE source_type = 'SALE' AND source_id = ?", id(order));
    }

    private void assertUnchangedStockAndPoints() throws SQLException {
        assertEquals(3, stock(EARLY_BATCH));
        assertEquals(10, stock(LATE_BATCH));
        assertEquals(99, stock(EXPIRED_BATCH));
        assertEquals(20, points());
    }

    private long stock(long batchId) throws SQLException {
        return scalar("SELECT quantity_on_hand FROM inventory_batches WHERE batch_id = ?", batchId);
    }

    private long points() throws SQLException {
        return scalar("SELECT loyalty_points FROM customers WHERE customer_id = ?", CUSTOMER.getLeastSignificantBits());
    }

    private void assertCustomerAndAllergyCount(long customerId, int expected) throws SQLException {
        assertEquals(expected, scalar("SELECT COUNT(*) FROM customers WHERE customer_id = ?", customerId));
        assertEquals(expected, scalar("SELECT COUNT(*) FROM customer_allergies WHERE customer_id = ?", customerId));
    }

    private long scalar(String sql, long id) throws SQLException {
        return new BigDecimal(query(sql, id)).longValueExact();
    }

    private String query(String sql, long id) throws SQLException {
        try (var connection = schema.connect(); var statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (var result = statement.executeQuery()) {
                assertTrue(result.next(), "Expected a fixture row");
                return result.getString(1);
            }
        }
    }

    public static class LoyaltyHistoryCheck {
        public static void main(String[] args) {
            var dao = new RelationalFeatureModuleDAO(new User(FIRST_STAFF, "staff_demo", "", UserRole.STAFF, true));
            List<ModuleRow> events = dao.findCustomerLoyaltyHistory(CUSTOMER).stream()
                    .filter(row -> args[0].equals(row.values().getFirst()))
                    .toList();
            assertEquals(2, events.size());
            assertEquals(2, events.stream().map(ModuleRow::id).distinct().count());
            assertEquals(2, events.stream().map(row -> row.values().get(2)).distinct().count());
            assertEquals(List.of("Tích điểm", "9"), events.get(0).values().subList(2, 4));
            assertEquals(List.of("Đổi điểm", "-3"), events.get(1).values().subList(2, 4));
        }
    }

    public static class CustomerDeletionCheck {
        public static void main(String[] args) {
            String schema = System.getenv("PHARMACY_DB_NAME");
            if (schema == null || !schema.matches("pharmacy_test_[a-f0-9]{32}")) {
                throw new IllegalStateException("Customer deletion tests require an isolated schema.");
            }
            var staff = new RelationalFeatureModuleDAO(new User(FIRST_STAFF, "staff_demo", "", UserRole.STAFF, true));
            var admin = new RelationalFeatureModuleDAO(new User(1, "admin_demo", "", UserRole.ADMIN, true));
            UUID unused = new UUID(ROW_ID_PREFIX, 2);
            assertThrows(SecurityException.class, () -> staff.deleteCustomerWithoutSales(unused));
            admin.deleteCustomerWithoutSales(unused);
            for (long customerId : new long[]{1, 3, 4}) {
                UUID customer = new UUID(ROW_ID_PREFIX, customerId);
                assertThrows(IllegalStateException.class, () -> admin.deleteCustomerWithoutSales(customer));
            }
        }
    }
}
