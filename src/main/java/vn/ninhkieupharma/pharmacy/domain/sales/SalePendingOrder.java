package vn.ninhkieupharma.pharmacy.domain.sales;

import java.math.BigDecimal;
import java.util.UUID;

public record SalePendingOrder(
        UUID id,
        String saleCode,
        SaleOrderRequest request,
        BigDecimal subtotal,
        long revision) {
}
