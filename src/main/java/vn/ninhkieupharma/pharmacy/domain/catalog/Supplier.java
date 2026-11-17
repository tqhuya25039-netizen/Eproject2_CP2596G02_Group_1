package vn.ninhkieupharma.pharmacy.domain.catalog;

import java.util.UUID;

public record Supplier(
        UUID id, String supplierCode, String name, String phone, String email, String address, String status) {
}
