package vn.ninhkieupharma.pharmacy.features.storagelocations.service;

import vn.ninhkieupharma.pharmacy.features.common.dao.FeatureModuleDAO;
import vn.ninhkieupharma.pharmacy.features.common.service.SingleModuleService;

public final class StorageLocationsService extends SingleModuleService {
    public StorageLocationsService(FeatureModuleDAO repository) {
        super(repository, "UC-13");
    }
}
