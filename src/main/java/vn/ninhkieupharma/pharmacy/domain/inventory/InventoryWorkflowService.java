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

    public List<InventoryWorkflowDAO.StocktakeDetail> findStocktakeDetails(UUID stocktakeId) {
        return workflowDAO().findStocktakeDetails(stocktakeId);
    }

    public ModuleRow receiveGoods(long authenticatedUserId, InventoryWorkflowDAO.ReceiptRequest request) {
        return workflowDAO().receiveGoods(authenticatedUserId, request);
    }

    public ModuleRow completeStocktake(
            long authenticatedUserId, InventoryWorkflowDAO.StocktakeRequest request) {
        return workflowDAO().completeStocktake(authenticatedUserId, request);
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
