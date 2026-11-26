package vn.ninhkieupharma.pharmacy.common.dao;

import org.junit.jupiter.api.Test;
import vn.ninhkieupharma.pharmacy.common.auth.User;
import vn.ninhkieupharma.pharmacy.common.auth.UserRole;
import vn.ninhkieupharma.pharmacy.common.service.FeatureServices;
import vn.ninhkieupharma.pharmacy.common.service.ModuleCatalog;
import vn.ninhkieupharma.pharmacy.domain.sales.SalesReport;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SalesAdministrationAccessTest {
    private static final UUID PROMOTION_ID = new UUID(0x4e4b504841524d41L, 1);
    private static final SalesReport.Filter REPORT_FILTER = new SalesReport.Filter(
            LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30), null, null, null);

    @Test
    void staffCannotReadOrChangePromotionsOrReadRevenueThroughDao() {
        RelationalFeatureModuleDAO dao = new RelationalFeatureModuleDAO(user(UserRole.STAFF, true));

        assertThrows(SecurityException.class, () -> dao.findAll("UC-18"));
        assertThrows(SecurityException.class, () -> dao.create("UC-18", List.of()));
        assertThrows(SecurityException.class, () -> dao.update("UC-18", PROMOTION_ID, List.of()));
        assertThrows(SecurityException.class, () -> dao.delete("UC-18", PROMOTION_ID));
        assertThrows(SecurityException.class, () -> dao.replaceAll("UC-18", List.of()));
        assertThrows(SecurityException.class, () -> dao.deactivatePromotion(PROMOTION_ID, Optional.empty()));
        assertThrows(SecurityException.class, () -> dao.deactivatePromotion(PROMOTION_ID, Optional.of(LocalDate.now())));
        assertThrows(SecurityException.class, () -> dao.findAll("UC-19"));
        assertThrows(SecurityException.class, () -> dao.findCompletedSalesByDay(null, null));
        assertThrows(SecurityException.class, dao::findSalesReportOptions);
        assertThrows(SecurityException.class, () -> dao.findSalesReportRows(REPORT_FILTER));
    }

    @Test
    void staffDashboardAndReportServicesCannotReadSalesTotals() {
        RelationalFeatureModuleDAO dao = new RelationalFeatureModuleDAO(user(UserRole.STAFF, true));
        FeatureServices services = new FeatureServices(dao);

        assertThrows(SecurityException.class, () -> services.salesService().completedSalesTotals());
        assertThrows(SecurityException.class,
                () -> services.salesService().salesReport(SalesReport.Period.MONTH, 2026, 11));
        assertThrows(SecurityException.class, services.salesService()::salesReportOptions);
        assertThrows(SecurityException.class, () -> services.salesService().salesReport(REPORT_FILTER));
        for (String moduleId : List.of("UC-18", "UC-19")) {
            var module = ModuleCatalog.MODULES.stream()
                    .filter(candidate -> candidate.id().equals(moduleId))
                    .findFirst()
                    .orElseThrow();
            assertFalse(module.staffAccessible());
            assertThrows(SecurityException.class, () -> services.forModule(module).findAll(module));
        }
    }

    @Test
    void unboundRepositoryAndInactiveAdministratorDoNotGrantAccess() {
        RelationalFeatureModuleDAO unbound = new RelationalFeatureModuleDAO();

        assertThrows(SecurityException.class, () -> unbound.findAll("UC-18"));
        assertThrows(SecurityException.class, () -> unbound.findCompletedSalesByDay(null, null));
        assertThrows(IllegalArgumentException.class, () -> new RelationalFeatureModuleDAO(user(UserRole.ADMIN, false)));
    }

    @Test
    void activeAdministratorCanReachPromotionAndReportInputValidation() {
        RelationalFeatureModuleDAO dao = new RelationalFeatureModuleDAO(user(UserRole.ADMIN, true));

        assertThrows(IllegalArgumentException.class, () -> dao.create("UC-18", List.of()));
        assertThrows(IllegalArgumentException.class, () -> dao.findCompletedSalesByDay(LocalDate.now(), null));
    }

    private static User user(UserRole role, boolean active) {
        return new User(1, "fixture", "", role, active);
    }
}
