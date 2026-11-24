package vn.ninhkieupharma.pharmacy.domain.sales;

import vn.ninhkieupharma.pharmacy.common.dao.FeatureModuleDAO;
import vn.ninhkieupharma.pharmacy.common.service.SingleModuleService;
import vn.ninhkieupharma.pharmacy.common.model.ModuleRow;

import java.util.List;
import java.util.UUID;

public final class CustomerLoyaltyService extends SingleModuleService {
    private final FeatureModuleDAO repository;

    public CustomerLoyaltyService(FeatureModuleDAO repository) {
        super(repository, "UC-16");
        this.repository = repository;
    }

    public List<ModuleRow> findPurchaseHistory(UUID customerId) {
        return repository.findCustomerPurchaseHistory(customerId);
    }

    public List<ModuleRow> findLoyaltyHistory(UUID customerId) {
        return repository.findCustomerLoyaltyHistory(customerId);
    }

    public ModuleRow updateProfile(UUID customerId, List<String> values) {
        return repository.updateCustomerProfile(customerId, values);
    }

    public void deleteWithoutSales(UUID customerId) {
        repository.deleteCustomerWithoutSales(customerId);
    }

    public List<ModuleRow> findAvailableForSale() {
        return repository.findCustomersAvailableForSale();
    }

    public ModuleRow createFromSale(List<String> values) {
        return repository.createCustomerFromSale(values);
    }

    public ModuleRow updateFromSale(UUID customerId, List<String> values) {
        return repository.updateCustomerFromSale(customerId, values);
    }
}
