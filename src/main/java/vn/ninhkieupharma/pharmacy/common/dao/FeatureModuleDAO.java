package vn.ninhkieupharma.pharmacy.common.dao;

import vn.ninhkieupharma.pharmacy.common.model.ModuleRow;
import vn.ninhkieupharma.pharmacy.domain.sales.SaleAllergyWarning;
import vn.ninhkieupharma.pharmacy.domain.sales.SaleOrderRequest;
import vn.ninhkieupharma.pharmacy.domain.sales.SaleProduct;
import vn.ninhkieupharma.pharmacy.domain.sales.SaleReceipt;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FeatureModuleDAO {
    List<ModuleRow> findAll(String moduleId);

    default List<ModuleRow> findCustomerPurchaseHistory(UUID customerId) {
        throw new UnsupportedOperationException("Customer purchase history is not available.");
    }

    default List<ModuleRow> findCustomersAvailableForSale() {
        throw new UnsupportedOperationException("Customer lookup for sales is not available.");
    }

    default ModuleRow createCustomerFromSale(List<String> values) {
        throw new UnsupportedOperationException("Customer creation from sales is not available.");
    }

    default ModuleRow updateCustomerFromSale(UUID customerId, List<String> values) {
        throw new UnsupportedOperationException("Customer editing from sales is not available.");
    }

    default List<SaleProduct> findSaleProducts() {
        throw new UnsupportedOperationException("Product lookup for sales is not available.");
    }

    default List<SaleAllergyWarning> findSaleAllergyWarnings(
            Optional<UUID> customerId, List<UUID> productIds) {
        throw new UnsupportedOperationException("Allergy checks for sales are not available.");
    }

    default SaleReceipt completeSale(long authenticatedUserId, SaleOrderRequest request) {
        throw new UnsupportedOperationException("Sales checkout is not available.");
    }

    default boolean supportsNativeReports() {
        return false;
    }

    void replaceAll(String moduleId, List<List<String>> values);

    ModuleRow create(String moduleId, List<String> values);

    ModuleRow update(String moduleId, UUID rowId, List<String> values);

    void delete(String moduleId, UUID rowId);
}
