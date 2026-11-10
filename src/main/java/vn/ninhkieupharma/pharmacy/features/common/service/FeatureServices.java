package vn.ninhkieupharma.pharmacy.features.common.service;

import vn.ninhkieupharma.pharmacy.features.common.repository.InMemoryModuleRepository;
import vn.ninhkieupharma.pharmacy.features.common.dao.FeatureModuleDAO;
import vn.ninhkieupharma.pharmacy.features.employeeaccounts.service.EmployeeAccountsService;
import vn.ninhkieupharma.pharmacy.features.availability.service.AvailabilityService;
import vn.ninhkieupharma.pharmacy.features.storeschedule.service.StoreScheduleService;
import vn.ninhkieupharma.pharmacy.features.attendance.service.AttendanceService;
import vn.ninhkieupharma.pharmacy.features.workhoursreport.service.WorkHoursReportService;
import vn.ninhkieupharma.pharmacy.features.schedulecompliancereport.service.ScheduleComplianceReportService;
import vn.ninhkieupharma.pharmacy.features.productcategories.service.ProductCategoriesService;
import vn.ninhkieupharma.pharmacy.features.products.service.ProductsService;
import vn.ninhkieupharma.pharmacy.features.suppliers.service.SuppliersService;
import vn.ninhkieupharma.pharmacy.features.categorytrendsreport.service.CategoryTrendsReportService;
import vn.ninhkieupharma.pharmacy.features.suppliercoverage.service.SupplierCoverageService;
import vn.ninhkieupharma.pharmacy.features.goodsreceiving.service.GoodsReceivingService;
import vn.ninhkieupharma.pharmacy.features.storagelocations.service.StorageLocationsService;
import vn.ninhkieupharma.pharmacy.features.stocktakeadjustments.service.StocktakeAdjustmentsService;
import vn.ninhkieupharma.pharmacy.features.inventorymovementreport.service.InventoryMovementReportService;
import vn.ninhkieupharma.pharmacy.features.customerloyalty.service.CustomerLoyaltyService;
import vn.ninhkieupharma.pharmacy.features.sales.service.SalesService;
import vn.ninhkieupharma.pharmacy.features.promotionsloyalty.service.PromotionsLoyaltyService;
import vn.ninhkieupharma.pharmacy.features.revenuereport.service.RevenueReportService;
import vn.ninhkieupharma.pharmacy.features.common.model.ModuleDefinition;

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
                new WorkHoursReportService(repository),
                new ScheduleComplianceReportService(repository),
                new ProductCategoriesService(repository),
                new ProductsService(repository),
                new SuppliersService(repository),
                new CategoryTrendsReportService(repository),
                new SupplierCoverageService(repository),
                new GoodsReceivingService(repository),
                new StorageLocationsService(repository),
                new StocktakeAdjustmentsService(repository),
                new InventoryMovementReportService(repository),
                new CustomerLoyaltyService(repository),
                new SalesService(repository),
                new PromotionsLoyaltyService(repository),
                new RevenueReportService(repository));
    }

    public FeatureModuleService forModule(ModuleDefinition module) {
        return services.stream()
                .filter(service -> service.supports(module))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No feature service registered for module " + module.id()));
    }
}
