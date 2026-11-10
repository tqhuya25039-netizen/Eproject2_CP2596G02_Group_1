package vn.ninhkieupharma.pharmacy.features.suppliers.service;

import vn.ninhkieupharma.pharmacy.features.common.dao.FeatureModuleDAO;
import vn.ninhkieupharma.pharmacy.features.common.service.SingleModuleService;

public final class SuppliersService extends SingleModuleService {
    public SuppliersService(FeatureModuleDAO repository) {
        super(repository, "UC-09");
    }
}
