package vn.ninhkieupharma.pharmacy.features.customerloyalty.service;

import vn.ninhkieupharma.pharmacy.features.common.dao.FeatureModuleDAO;
import vn.ninhkieupharma.pharmacy.features.common.service.SingleModuleService;

public final class CustomerLoyaltyService extends SingleModuleService {
    public CustomerLoyaltyService(FeatureModuleDAO repository) {
        super(repository, "UC-16");
    }
}
