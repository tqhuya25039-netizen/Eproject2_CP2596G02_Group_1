package vn.ninhkieupharma.pharmacy.domain.sales;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SaleStockShortage(
        String productName, LocalDate requiredExpiry, BigDecimal requested, BigDecimal available) {
}
