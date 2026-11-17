package vn.ninhkieupharma.pharmacy.domain.sales;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record SaleProduct(
        UUID id,
        String code,
        String name,
        String unit,
        boolean prescriptionRequired,
        BigDecimal sellingPrice,
        BigDecimal stockOnHand,
        List<String> ingredients) {
    public SaleProduct {
        ingredients = List.copyOf(ingredients);
    }

    @Override
    public String toString() {
        return code + " · " + name;
    }
}
