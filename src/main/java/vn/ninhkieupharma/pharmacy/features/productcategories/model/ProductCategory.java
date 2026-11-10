package vn.ninhkieupharma.pharmacy.features.productcategories.model;

import java.util.UUID;

public record ProductCategory(UUID id, String categoryCode, String name, String description, String status) {
}
