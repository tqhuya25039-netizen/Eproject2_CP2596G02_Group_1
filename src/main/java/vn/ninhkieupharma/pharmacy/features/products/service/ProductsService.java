package vn.ninhkieupharma.pharmacy.features.products.service;

import vn.ninhkieupharma.pharmacy.features.common.dao.FeatureModuleDAO;
import vn.ninhkieupharma.pharmacy.features.common.service.SingleModuleService;

public final class ProductsService extends SingleModuleService {
    public ProductsService(FeatureModuleDAO repository) {
        super(repository, "UC-08");
    }
}
