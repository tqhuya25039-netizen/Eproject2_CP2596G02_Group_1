package vn.ninhkieupharma.pharmacy.features.products.model;

import java.util.UUID;

public record Product(
        UUID id, String productCode, String name, String category, String unit,
        String prescription, String status, String supplier) {
}
