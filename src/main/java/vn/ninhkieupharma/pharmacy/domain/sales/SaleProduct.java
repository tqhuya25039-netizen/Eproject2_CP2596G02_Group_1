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
        List<String> ingredients,
        String barcode) {
    public SaleProduct {
        ingredients = List.copyOf(ingredients);
        barcode = barcode == null ? "" : barcode;
    }

    public SaleProduct(
            UUID id, String code, String name, String unit, boolean prescriptionRequired,
            BigDecimal sellingPrice, BigDecimal stockOnHand, List<String> ingredients) {
        this(id, code, name, unit, prescriptionRequired, sellingPrice, stockOnHand, ingredients, "");
    }

    @Override
    public String toString() {
        return code + " · " + name;
    }
}
