package vn.ninhkieupharma.pharmacy.features.promotionsloyalty.service;

import vn.ninhkieupharma.pharmacy.features.common.dao.FeatureModuleDAO;
import vn.ninhkieupharma.pharmacy.features.common.service.SingleModuleService;

public final class PromotionsLoyaltyService extends SingleModuleService {
    public PromotionsLoyaltyService(FeatureModuleDAO repository) {
        super(repository, "UC-18");
    }
}
