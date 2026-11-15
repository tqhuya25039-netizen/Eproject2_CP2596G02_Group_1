package vn.ninhkieupharma.pharmacy.domain.catalog;

import java.util.UUID;

public record StorageLocation(UUID id, String locationCode, String name, String description, String status) {
}
