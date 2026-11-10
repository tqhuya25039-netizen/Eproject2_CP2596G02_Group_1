package vn.ninhkieupharma.pharmacy.features.storagelocations.model;

import java.util.UUID;

public record StorageLocation(UUID id, String locationCode, String name, String description, String status) {
}
