package vn.ninhkieupharma.pharmacy.features.goodsreceiving.model;

import java.util.UUID;

public record GoodsReceipt(UUID id, String receiptCode, String supplier, String receivedAt, String itemCount, String status) {
}
