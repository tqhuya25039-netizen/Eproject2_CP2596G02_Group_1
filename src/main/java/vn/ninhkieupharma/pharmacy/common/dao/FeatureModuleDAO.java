package vn.ninhkieupharma.pharmacy.common.dao;

import vn.ninhkieupharma.pharmacy.common.model.ModuleRow;
import vn.ninhkieupharma.pharmacy.domain.sales.SaleAllergyWarning;
import vn.ninhkieupharma.pharmacy.domain.sales.SaleLineRequest;
import vn.ninhkieupharma.pharmacy.domain.sales.SaleStockPreview;
import vn.ninhkieupharma.pharmacy.domain.sales.SaleStockShortage;
import vn.ninhkieupharma.pharmacy.domain.sales.SaleOrderRequest;
import vn.ninhkieupharma.pharmacy.domain.sales.SaleProduct;
import vn.ninhkieupharma.pharmacy.domain.sales.SalePendingOrder;
import vn.ninhkieupharma.pharmacy.domain.sales.SaleQuote;
import vn.ninhkieupharma.pharmacy.domain.sales.SaleReceipt;
import vn.ninhkieupharma.pharmacy.domain.sales.SalesReport;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FeatureModuleDAO {
    List<ModuleRow> findAll(String moduleId);

    default void deactivatePromotion(UUID promotionId, Optional<LocalDate> endOn) {
        ModuleRow existing = findAll("UC-18").stream()
                .filter(row -> row.id().equals(promotionId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("The selected program no longer exists."));
        List<String> values = new ArrayList<>(existing.values());
        if (endOn.isPresent()) {
            LocalDate end = endOn.orElseThrow();
            if (!end.isBefore(LocalDate.parse(values.get(4)))) {
                if (end.isBefore(LocalDate.parse(values.get(5)))) {
                    values.set(5, end.toString());
                }
                if (end.isBefore(LocalDate.parse(values.get(6)))) {
                    values.set(6, end.toString());
                }
            }
        }
        values.set(9, "INACTIVE");
        update("UC-18", promotionId, values);
    }

    default List<ModuleRow> findCustomerPurchaseHistory(UUID customerId) {
        throw new UnsupportedOperationException("Customer purchase history is not available.");
    }

    default List<ModuleRow> findCustomerLoyaltyHistory(UUID customerId) {
        throw new UnsupportedOperationException("Customer loyalty history is not available.");
    }

    default ModuleRow updateCustomerProfile(UUID customerId, List<String> values) {
        throw new UnsupportedOperationException("Customer profile editing is not available.");
    }

    default void deleteCustomerWithoutSales(UUID customerId) {
        throw new UnsupportedOperationException("Permanent customer deletion is not available.");
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

    default List<SaleStockShortage> previewSaleStock(List<SaleLineRequest> lines) {
        throw new UnsupportedOperationException("Batch stock checks are not available.");
    }

    default SaleStockPreview previewSalePicking(List<SaleLineRequest> lines) {
        return new SaleStockPreview(List.of(), previewSaleStock(lines));
    }

    default List<SaleAllergyWarning> findSaleAllergyWarnings(
            Optional<UUID> customerId, List<UUID> productIds) {
        throw new UnsupportedOperationException("Allergy checks for sales are not available.");
    }

    default SaleReceipt completeSale(long authenticatedUserId, SaleOrderRequest request) {
        throw new UnsupportedOperationException("Sales checkout is not available.");
    }

    default SalePendingOrder createPendingSale(long authenticatedUserId, SaleOrderRequest request) {
        throw new UnsupportedOperationException("Pending sales are not available.");
    }

    default SalePendingOrder updatePendingSale(
            long authenticatedUserId, UUID saleId, long revision, SaleOrderRequest request) {
        throw new UnsupportedOperationException("Pending sales are not available.");
    }

    default SalePendingOrder findPendingSale(UUID saleId) {
        throw new UnsupportedOperationException("Pending sales are not available.");
    }

    default SaleQuote quotePendingSale(
            long authenticatedUserId, UUID saleId, long revision, SaleOrderRequest request) {
        throw new UnsupportedOperationException("Pending sales are not available.");
    }

    default SaleReceipt completePendingSale(
            long authenticatedUserId, UUID saleId, long revision, SaleOrderRequest request, SaleQuote acceptedQuote) {
        throw new UnsupportedOperationException("Pending sales are not available.");
    }

    default void cancelPendingSale(long authenticatedUserId, UUID saleId, long revision) {
        throw new UnsupportedOperationException("Pending sales are not available.");
    }

    default List<ModuleRow> findSaleDetails(UUID saleId) {
        throw new UnsupportedOperationException("Sales details are not available.");
    }

    default boolean supportsNativeReports() {
        return false;
    }

    /** Returns completed sales totals by day. Omit both bounds to include all dates. */
    default List<SalesReport.Row> findCompletedSalesByDay(
            LocalDate fromInclusive, LocalDate toExclusive) {
        throw new UnsupportedOperationException("Sales reporting is not available.");
    }

    default SalesReport.FilterOptions findSalesReportOptions() {
        return new SalesReport.FilterOptions(List.of(), List.of(), List.of());
    }

    default SalesReport.Rows findSalesReportRows(SalesReport.Filter filter) {
        throw new UnsupportedOperationException("Sales reporting with filters is not available.");
    }

    void replaceAll(String moduleId, List<List<String>> values);

    ModuleRow create(String moduleId, List<String> values);

    ModuleRow update(String moduleId, UUID rowId, List<String> values);

    void delete(String moduleId, UUID rowId);
}
