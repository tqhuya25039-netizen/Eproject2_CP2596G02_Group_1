package vn.ninhkieupharma.pharmacy.features.availability.service;

import vn.ninhkieupharma.pharmacy.features.common.dao.FeatureModuleDAO;
import vn.ninhkieupharma.pharmacy.features.common.service.SingleModuleService;

public final class AvailabilityService extends SingleModuleService {
    public AvailabilityService(FeatureModuleDAO repository) {
        super(repository, "UC-02");
    }
}
