package vn.ninhkieupharma.pharmacy.domain.sales;

import vn.ninhkieupharma.pharmacy.common.service.Localization;

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
        return calculate(subtotal, promotionPercent, availablePoints,
                redeemPoints ? SaleOrderRequest.REDEEM_MAXIMUM : 0,
                currencyPerPoint, pointsRatePercent, eligibleForPoints);
    }

    public static Result calculate(
            BigDecimal subtotal,
            BigDecimal promotionPercent,
            long availablePoints,
            long requestedPoints,
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
        if (availablePoints < 0 || requestedPoints < 0) {
            throw new IllegalArgumentException("Available and requested points cannot be negative.");
        }

        BigDecimal normalizedSubtotal = subtotal.setScale(2, RoundingMode.HALF_UP);
        BigDecimal promotionDiscount = normalizedSubtotal.multiply(promotionPercent)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        long pointsRedeemed;
        BigDecimal pointsDiscount = BigDecimal.ZERO.setScale(2);
        long redeemablePoints = 0;
        if (requestedPoints > 0 && eligibleForPoints && currencyPerPoint.signum() > 0) {
            BigDecimal redemptionCap = normalizedSubtotal.multiply(new BigDecimal("0.50"))
                    .min(normalizedSubtotal.subtract(promotionDiscount))
                    .max(BigDecimal.ZERO);
            BigDecimal allowedByAmount = redemptionCap.divide(currencyPerPoint, 0, RoundingMode.DOWN);
            redeemablePoints = allowedByAmount.compareTo(BigDecimal.valueOf(availablePoints)) >= 0
                    ? availablePoints
                    : allowedByAmount.longValueExact();
        }
        if (requestedPoints == SaleOrderRequest.REDEEM_MAXIMUM) {
            pointsRedeemed = redeemablePoints;
        } else if (requestedPoints > redeemablePoints) {
            throw new IllegalArgumentException(Localization.text("Số điểm tối đa có thể đổi cho đơn này: ")
                    + redeemablePoints);
        } else {
            pointsRedeemed = requestedPoints;
        }
        if (pointsRedeemed > 0) {
            pointsDiscount = currencyPerPoint.multiply(BigDecimal.valueOf(pointsRedeemed))
                    .setScale(2, RoundingMode.DOWN);
            if (pointsDiscount.signum() == 0) {
                if (requestedPoints != SaleOrderRequest.REDEEM_MAXIMUM) {
                    throw new IllegalArgumentException(Localization.text("Số điểm đã nhập chưa tạo ra tiền giảm."));
                }
                pointsRedeemed = 0;
            }
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
