package vn.ninhkieupharma.pharmacy.common.dao;

import vn.ninhkieupharma.pharmacy.common.model.ModuleRow;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface InventoryWorkflowDAO {
    record SupplierOption(long id, String code, String name) {
        @Override
        public String toString() {
            return code + " · " + name;
        }
    }

    record ProductOption(long id, String code, String name, String unit) {
        @Override
        public String toString() {
            return code + " · " + name + " (" + unit + ")";
        }
    }

    record LocationOption(long id, String code, String name) {
        @Override
        public String toString() {
            return code + " · " + name;
        }
    }

    record BatchOption(
            long id,
            long productId,
            long locationId,
            String productCode,
            String productName,
            String batchCode,
            LocalDate expiryDate,
            BigDecimal quantityOnHand,
            String locationCode,
            String locationName) {
        public String label() {
            return productCode + " · " + productName + " · " + batchCode
                    + " · " + quantityOnHand.stripTrailingZeros().toPlainString()
                    + " · " + (expiryDate == null ? "Không có HSD" : expiryDate)
                    + " · " + locationCode;
        }
    }

    record ReceiptLine(
            long productId,
            long locationId,
            String batchCode,
            LocalDate expiryDate,
            BigDecimal quantity,
            BigDecimal unitCost) {
    }

    record ReceiptRequest(
            long supplierId,
            LocalDate receivedDate,
            String note,
            List<ReceiptLine> lines) {
        public ReceiptRequest {
            lines = List.copyOf(lines);
        }
    }

    record ReceiptDraft(long supplierId, LocalDate receivedDate, String note, List<ReceiptLine> lines) {
        public ReceiptDraft {
            lines = List.copyOf(lines);
        }
    }

    record ReceiptDetail(
            String productCode,
            String productName,
            String unitName,
            String batchCode,
            LocalDate expiryDate,
            BigDecimal quantity,
            BigDecimal unitCost,
            String locationCode,
            String locationName) {
    }

    record StocktakeDetail(
            String productCode,
            String productName,
            String unitName,
            String batchCode,
            LocalDate expiryDate,
            BigDecimal expectedQuantity,
            BigDecimal countedQuantity,
            BigDecimal differenceQuantity,
            String locationCode,
            String locationName) {
    }

    record StocktakeDetails(String note, List<StocktakeDetail> items) {
        public StocktakeDetails {
            note = note == null ? "" : note;
            items = List.copyOf(items);
        }
    }

    record StocktakeCount(long batchId, BigDecimal countedQuantity) {
    }

    record StocktakeRequest(
            long locationId,
            LocalDate stocktakeDate,
            String note,
            List<StocktakeCount> counts) {
        public StocktakeRequest {
            counts = List.copyOf(counts);
        }
    }

    record StocktakeDraft(
            long locationId,
            LocalDate stocktakeDate,
            String note,
            List<StocktakeCount> counts) {
        public StocktakeDraft {
            counts = List.copyOf(counts);
        }
    }

    enum LossType {
        DAMAGED,
        EXPIRED
    }

    List<SupplierOption> findInventorySuppliers();

    List<ProductOption> findInventoryProducts();

    List<LocationOption> findInventoryLocations();

    List<BatchOption> findInventoryBatches(Long locationId);

    List<ReceiptDetail> findGoodsReceiptDetails(UUID receiptId);

    ReceiptDraft findGoodsReceiptDraft(UUID receiptId);

    StocktakeDetails findStocktakeDetails(UUID stocktakeId);

    StocktakeDraft findStocktakeDraft(UUID stocktakeId);

    ModuleRow createGoodsReceiptDraft(long authenticatedUserId, ReceiptRequest request);

    ModuleRow updateGoodsReceiptDraft(long authenticatedUserId, UUID receiptId, ReceiptRequest request);

    void cancelGoodsReceiptDraft(long authenticatedUserId, UUID receiptId);

    ModuleRow confirmGoodsReceipt(long authenticatedUserId, UUID receiptId);

    ModuleRow createStocktakeDraft(long authenticatedUserId, StocktakeRequest request);

    ModuleRow updateStocktakeDraft(long authenticatedUserId, UUID stocktakeId, StocktakeRequest request);

    void cancelStocktakeDraft(long authenticatedUserId, UUID stocktakeId);

    ModuleRow confirmStocktake(long authenticatedUserId, UUID stocktakeId);

    void recordInventoryLoss(long authenticatedUserId, long batchId, LossType lossType,
                             BigDecimal quantity, String note);
}
