package vn.ninhkieupharma.pharmacy.features.common.service;

import vn.ninhkieupharma.pharmacy.features.common.dao.FeatureModuleDAO;

import java.util.Set;

public class SingleModuleService extends AbstractFeatureModuleService {
    public SingleModuleService(FeatureModuleDAO repository, String moduleId) {
        super(repository, Set.of(moduleId));
    }
}
