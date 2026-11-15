package vn.ninhkieupharma.pharmacy.domain.catalog;

import vn.ninhkieupharma.pharmacy.common.dao.FeatureModuleDAO;
import vn.ninhkieupharma.pharmacy.common.service.SingleModuleService;

public final class ProductCategoriesService extends SingleModuleService {
    public ProductCategoriesService(FeatureModuleDAO repository) {
        super(repository, "UC-07");
    }
}
