package vn.ninhkieupharma.pharmacy.domain.sales;

import vn.ninhkieupharma.pharmacy.common.dao.FeatureModuleDAO;
import vn.ninhkieupharma.pharmacy.common.service.SingleModuleService;

public final class PromotionsLoyaltyService extends SingleModuleService {
    public PromotionsLoyaltyService(FeatureModuleDAO repository) {
        super(repository, "UC-18");
    }
}
