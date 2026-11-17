package vn.ninhkieupharma.pharmacy.domain.sales;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SalePricingTest {
    @Test
    void redeemsAvailablePointsUpToHalfOfOriginalSubtotalAndEarnsFromFinalTotal() {
        SalePricing.Result result = SalePricing.calculate(
                new BigDecimal("1000.00"),
                new BigDecimal("10"),
                1000,
                true,
                new BigDecimal("10.00"),
                new BigDecimal("5"),
                true);

        assertEquals(new BigDecimal("100.00"), result.promotionDiscount());
        assertEquals(new BigDecimal("500.00"), result.pointsDiscount());
        assertEquals(new BigDecimal("400.00"), result.total());
        assertEquals(50, result.pointsRedeemed());
        assertEquals(20, result.pointsEarned());
    }

    @Test
    void capsPointsAtRemainingValueWhenPromotionAlreadyExceedsHalfTheSubtotal() {
        SalePricing.Result result = SalePricing.calculate(
                new BigDecimal("1000.00"),
                new BigDecimal("60"),
                1000,
                true,
                new BigDecimal("10.00"),
                BigDecimal.ZERO,
                true);

        assertEquals(new BigDecimal("400.00"), result.pointsDiscount());
        assertEquals(BigDecimal.ZERO.setScale(2), result.total());
        assertEquals(40, result.pointsRedeemed());
    }

    @Test
    void doesNotRedeemPointsWithoutConsent() {
        SalePricing.Result result = SalePricing.calculate(
                new BigDecimal("1000.00"),
                new BigDecimal("10"),
                100,
                false,
                new BigDecimal("10.00"),
                new BigDecimal("5"),
                true);

        assertEquals(0, result.pointsRedeemed());
        assertEquals(BigDecimal.ZERO.setScale(2), result.pointsDiscount());
        assertEquals(new BigDecimal("900.00"), result.total());
        assertEquals(45, result.pointsEarned());
    }

    @Test
    void rejectsEarnedPointsOutsideTheSupportedNumericRange() {
        assertThrows(IllegalArgumentException.class, () -> SalePricing.calculate(
                new BigDecimal("1000.00"),
                BigDecimal.ZERO,
                0,
                false,
                BigDecimal.ZERO,
                new BigDecimal("1000000000000000000"),
                true));
    }
}
