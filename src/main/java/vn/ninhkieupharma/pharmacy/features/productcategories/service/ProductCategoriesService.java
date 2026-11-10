package vn.ninhkieupharma.pharmacy.features.productcategories.service;

import vn.ninhkieupharma.pharmacy.features.common.dao.FeatureModuleDAO;
import vn.ninhkieupharma.pharmacy.features.common.service.SingleModuleService;

public final class ProductCategoriesService extends SingleModuleService {
    public ProductCategoriesService(FeatureModuleDAO repository) {
        super(repository, "UC-07");
    }
}
