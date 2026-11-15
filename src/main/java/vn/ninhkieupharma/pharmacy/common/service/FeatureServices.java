package vn.ninhkieupharma.pharmacy.common.service;

import vn.ninhkieupharma.pharmacy.common.repository.InMemoryModuleRepository;
import vn.ninhkieupharma.pharmacy.common.dao.FeatureModuleDAO;
import vn.ninhkieupharma.pharmacy.domain.hr.EmployeeAccountsService;
import vn.ninhkieupharma.pharmacy.domain.hr.AvailabilityService;
import vn.ninhkieupharma.pharmacy.domain.hr.StoreScheduleService;
import vn.ninhkieupharma.pharmacy.domain.hr.AttendanceService;
import vn.ninhkieupharma.pharmacy.domain.catalog.ProductCategoriesService;
import vn.ninhkieupharma.pharmacy.domain.catalog.ProductsService;
import vn.ninhkieupharma.pharmacy.domain.catalog.SuppliersService;
import vn.ninhkieupharma.pharmacy.domain.inventory.GoodsReceivingService;
import vn.ninhkieupharma.pharmacy.domain.catalog.StorageLocationsService;
import vn.ninhkieupharma.pharmacy.domain.inventory.StocktakeAdjustmentsService;
import vn.ninhkieupharma.pharmacy.domain.sales.CustomerLoyaltyService;
import vn.ninhkieupharma.pharmacy.domain.sales.SalesService;
import vn.ninhkieupharma.pharmacy.domain.sales.PromotionsLoyaltyService;
import vn.ninhkieupharma.pharmacy.common.model.ModuleDefinition;

import java.util.List;

public final class FeatureServices {
    private final List<FeatureModuleService> services;

    public FeatureServices() {
        this(new InMemoryModuleRepository());
    }

    public FeatureServices(FeatureModuleDAO repository) {
        services = List.of(
                new EmployeeAccountsService(repository),
                new AvailabilityService(repository),
                new StoreScheduleService(repository),
                new AttendanceService(repository),
                new ProductCategoriesService(repository),
                new ProductsService(repository),
                new SuppliersService(repository),
                new GoodsReceivingService(repository),
                new StorageLocationsService(repository),
                new StocktakeAdjustmentsService(repository),
                new CustomerLoyaltyService(repository),
                new SalesService(repository),
                new PromotionsLoyaltyService(repository));
    }

    public FeatureModuleService forModule(ModuleDefinition module) {
        return services.stream()
                .filter(service -> service.supports(module))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No feature service registered for module " + module.id()));
    }
}
