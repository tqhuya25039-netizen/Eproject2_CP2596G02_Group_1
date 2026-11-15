package vn.ninhkieupharma.pharmacy.domain.sales;

import java.util.UUID;

public record Customer(
        UUID id,
        String customerCode,
        String fullName,
        String phone,
        String allergies,
        int loyaltyPoints,
        String status) {
}
