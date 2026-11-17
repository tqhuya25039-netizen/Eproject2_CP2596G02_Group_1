package vn.ninhkieupharma.pharmacy.domain.sales;

import java.math.BigDecimal;
import java.util.UUID;

public record SaleLineRequest(
        UUID productId,
        BigDecimal quantity,
        String dosageInstructions,
        int treatmentDays) {
}
