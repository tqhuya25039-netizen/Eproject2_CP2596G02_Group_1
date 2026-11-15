package vn.ninhkieupharma.pharmacy.domain.sales;

import vn.ninhkieupharma.pharmacy.common.dao.FeatureModuleDAO;
import vn.ninhkieupharma.pharmacy.common.service.SingleModuleService;

public final class CustomerLoyaltyService extends SingleModuleService {
    public CustomerLoyaltyService(FeatureModuleDAO repository) {
        super(repository, "UC-16");
    }
}
