package vn.ninhkieupharma.pharmacy.domain.inventory;

import vn.ninhkieupharma.pharmacy.common.dao.FeatureModuleDAO;
import vn.ninhkieupharma.pharmacy.common.dao.InventoryWorkflowDAO;
import vn.ninhkieupharma.pharmacy.common.model.ModuleRow;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public final class InventoryWorkflowService {
    private final FeatureModuleDAO repository;

    public InventoryWorkflowService(FeatureModuleDAO repository) {
        this.repository = repository;
    }

    public List<InventoryWorkflowDAO.SupplierOption> findSuppliers() {
        return workflowDAO().findInventorySuppliers();
    }

    public List<InventoryWorkflowDAO.ProductOption> findProducts() {
        return workflowDAO().findInventoryProducts();
    }

    public List<InventoryWorkflowDAO.LocationOption> findLocations() {
        return workflowDAO().findInventoryLocations();
    }

    public List<InventoryWorkflowDAO.BatchOption> findBatches(Long locationId) {
        return workflowDAO().findInventoryBatches(locationId);
    }

    public List<InventoryWorkflowDAO.ReceiptDetail> findReceiptDetails(UUID receiptId) {
        return workflowDAO().findGoodsReceiptDetails(receiptId);
    }

    public InventoryWorkflowDAO.ReceiptDraft findReceiptDraft(UUID receiptId) {
        return workflowDAO().findGoodsReceiptDraft(receiptId);
    }

    public List<InventoryWorkflowDAO.StocktakeDetail> findStocktakeDetails(UUID stocktakeId) {
        return workflowDAO().findStocktakeDetails(stocktakeId);
    }

    public InventoryWorkflowDAO.StocktakeDraft findStocktakeDraft(UUID stocktakeId) {
        return workflowDAO().findStocktakeDraft(stocktakeId);
    }

    public ModuleRow createGoodsReceiptDraft(
            long authenticatedUserId, InventoryWorkflowDAO.ReceiptRequest request) {
        return workflowDAO().createGoodsReceiptDraft(authenticatedUserId, request);
    }

    public ModuleRow updateReceiptDraft(
            long authenticatedUserId, UUID receiptId, InventoryWorkflowDAO.ReceiptRequest request) {
        return workflowDAO().updateGoodsReceiptDraft(authenticatedUserId, receiptId, request);
    }

    public void cancelReceiptDraft(long authenticatedUserId, UUID receiptId) {
        workflowDAO().cancelGoodsReceiptDraft(authenticatedUserId, receiptId);
    }

    public ModuleRow confirmReceipt(long authenticatedUserId, UUID receiptId) {
        return workflowDAO().confirmGoodsReceipt(authenticatedUserId, receiptId);
    }

    public ModuleRow createStocktakeDraft(
            long authenticatedUserId, InventoryWorkflowDAO.StocktakeRequest request) {
        return workflowDAO().createStocktakeDraft(authenticatedUserId, request);
    }

    public ModuleRow updateStocktakeDraft(
            long authenticatedUserId, UUID stocktakeId, InventoryWorkflowDAO.StocktakeRequest request) {
        return workflowDAO().updateStocktakeDraft(authenticatedUserId, stocktakeId, request);
    }

    public void cancelStocktakeDraft(long authenticatedUserId, UUID stocktakeId) {
        workflowDAO().cancelStocktakeDraft(authenticatedUserId, stocktakeId);
    }

    public ModuleRow confirmStocktake(long authenticatedUserId, UUID stocktakeId) {
        return workflowDAO().confirmStocktake(authenticatedUserId, stocktakeId);
    }

    public void recordInventoryLoss(
            long authenticatedUserId, long batchId, InventoryWorkflowDAO.LossType lossType,
            BigDecimal quantity, String note) {
        workflowDAO().recordInventoryLoss(authenticatedUserId, batchId, lossType, quantity, note);
    }

    private InventoryWorkflowDAO workflowDAO() {
        if (repository instanceof InventoryWorkflowDAO inventoryWorkflowDAO) {
            return inventoryWorkflowDAO;
        }
        throw new UnsupportedOperationException("Inventory workflows require the relational inventory schema.");
    }
}
