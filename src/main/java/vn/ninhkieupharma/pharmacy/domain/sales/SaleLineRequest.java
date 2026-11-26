package vn.ninhkieupharma.pharmacy.domain.sales;

import java.math.BigDecimal;
import java.util.UUID;

public record SaleLineRequest(
        UUID productId,
        BigDecimal quantity,
        String dosageInstructions,
        int treatmentDays,
        SaleLineKind kind,
        DoseInstructions dose) {
    public SaleLineRequest {
        dose = dose == null ? DoseInstructions.EMPTY : dose;
    }

    public SaleLineRequest(UUID productId, BigDecimal quantity, String dosageInstructions, int treatmentDays) {
        this(productId, quantity, dosageInstructions, treatmentDays, null, DoseInstructions.EMPTY);
    }
}
