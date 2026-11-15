package vn.ninhkieupharma.pharmacy.domain.hr;

import vn.ninhkieupharma.pharmacy.common.dao.FeatureModuleDAO;
import vn.ninhkieupharma.pharmacy.common.service.SingleModuleService;

public final class AvailabilityService extends SingleModuleService {
    public AvailabilityService(FeatureModuleDAO repository) {
        super(repository, "UC-02");
    }
}
