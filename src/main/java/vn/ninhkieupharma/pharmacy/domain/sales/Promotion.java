package vn.ninhkieupharma.pharmacy.domain.sales;

import java.util.UUID;

public record Promotion(UUID id, String promotionCode, String name, String discount, String validity, String status) {
}
