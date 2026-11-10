package vn.ninhkieupharma.pharmacy.features.storeschedule.service;

import vn.ninhkieupharma.pharmacy.features.common.dao.FeatureModuleDAO;
import vn.ninhkieupharma.pharmacy.features.common.service.SingleModuleService;

public final class StoreScheduleService extends SingleModuleService {
    public StoreScheduleService(FeatureModuleDAO repository) {
        super(repository, "UC-03");
    }
}
