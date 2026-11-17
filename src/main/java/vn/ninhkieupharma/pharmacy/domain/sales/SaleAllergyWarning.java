package vn.ninhkieupharma.pharmacy.domain.sales;

import java.util.UUID;

public record SaleAllergyWarning(
        UUID productId,
        String productName,
        String allergenName,
        String ingredientName) {
}
