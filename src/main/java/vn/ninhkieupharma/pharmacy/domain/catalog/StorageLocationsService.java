package vn.ninhkieupharma.pharmacy.domain.catalog;

import vn.ninhkieupharma.pharmacy.common.dao.FeatureModuleDAO;
import vn.ninhkieupharma.pharmacy.common.service.SingleModuleService;

public final class StorageLocationsService extends SingleModuleService {
    public StorageLocationsService(FeatureModuleDAO repository) {
        super(repository, "UC-13");
    }
}
