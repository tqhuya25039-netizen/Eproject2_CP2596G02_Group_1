package vn.ninhkieupharma.pharmacy.domain.catalog;

import java.util.UUID;

public record ProductCategory(UUID id, String categoryCode, String name, String description, String status) {
}
