package vn.ninhkieupharma.pharmacy.features.suppliers.model;

import java.util.UUID;

public record Supplier(UUID id, String supplierCode, String name, String phone, String email, String status) {
}
