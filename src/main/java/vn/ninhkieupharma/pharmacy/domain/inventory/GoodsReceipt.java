package vn.ninhkieupharma.pharmacy.domain.inventory;

import java.util.UUID;

public record GoodsReceipt(UUID id, String receiptCode, String supplier, String receivedAt, String itemCount, String status) {
}
