package vn.ninhkieupharma.pharmacy.domain.sales;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class SalePricing {
    public record Result(
            BigDecimal subtotal,
            BigDecimal promotionDiscount,
            BigDecimal pointsDiscount,
            BigDecimal totalDiscount,
            BigDecimal total,
            long pointsRedeemed,
            long pointsEarned) {
    }

    private SalePricing() {
    }

    public static Result calculate(
            BigDecimal subtotal,
            BigDecimal promotionPercent,
            long availablePoints,
            boolean redeemPoints,
            BigDecimal currencyPerPoint,
            BigDecimal pointsRatePercent,
            boolean eligibleForPoints) {
        requireNonNegative(subtotal, "Subtotal");
        requireNonNegative(promotionPercent, "Promotion percentage");
        requireNonNegative(currencyPerPoint, "Point redemption rate");
        requireNonNegative(pointsRatePercent, "Point earning rate");
        if (promotionPercent.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("Promotion percentage cannot exceed 100.");
        }
        if (availablePoints < 0) {
            throw new IllegalArgumentException("Available points cannot be negative.");
        }

        BigDecimal normalizedSubtotal = subtotal.setScale(2, RoundingMode.HALF_UP);
        BigDecimal promotionDiscount = normalizedSubtotal.multiply(promotionPercent)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        long pointsRedeemed = 0;
        BigDecimal pointsDiscount = BigDecimal.ZERO.setScale(2);
        if (redeemPoints && eligibleForPoints && currencyPerPoint.signum() > 0) {
            BigDecimal redemptionCap = normalizedSubtotal.multiply(new BigDecimal("0.50"))
                    .min(normalizedSubtotal.subtract(promotionDiscount))
                    .max(BigDecimal.ZERO);
            BigDecimal allowedByAmount = redemptionCap.divide(currencyPerPoint, 0, RoundingMode.DOWN);
            pointsRedeemed = allowedByAmount.compareTo(BigDecimal.valueOf(availablePoints)) >= 0
                    ? availablePoints
                    : allowedByAmount.longValueExact();
            pointsDiscount = currencyPerPoint.multiply(BigDecimal.valueOf(pointsRedeemed))
                    .setScale(2, RoundingMode.DOWN);
        }
        BigDecimal totalDiscount = promotionDiscount.add(pointsDiscount).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = normalizedSubtotal.subtract(totalDiscount).setScale(2, RoundingMode.HALF_UP);
        long pointsEarned = eligibleForPoints
                ? toLongExact(total.multiply(pointsRatePercent)
                        .divide(new BigDecimal("100"), 0, RoundingMode.DOWN))
                : 0;
        return new Result(normalizedSubtotal, promotionDiscount, pointsDiscount, totalDiscount,
                total, pointsRedeemed, pointsEarned);
    }

    private static long toLongExact(BigDecimal value) {
        try {
            return value.longValueExact();
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("Calculated loyalty points exceed the supported range.", exception);
        }
    }

    private static void requireNonNegative(BigDecimal value, String label) {
        if (value == null || value.signum() < 0) {
            throw new IllegalArgumentException(label + " must be non-negative.");
        }
    }
}
