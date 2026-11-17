package vn.ninhkieupharma.pharmacy.domain.sales;

import java.math.BigDecimal;

public record SaleReceipt(
        String saleCode,
        BigDecimal subtotal,
        BigDecimal promotionDiscount,
        BigDecimal pointsDiscount,
        BigDecimal total,
        long pointsRedeemed,
        long pointsEarned,
        long remainingPoints,
        String promotionCode) {
}
