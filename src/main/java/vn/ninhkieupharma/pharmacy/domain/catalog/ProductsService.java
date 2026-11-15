package vn.ninhkieupharma.pharmacy.domain.catalog;

import vn.ninhkieupharma.pharmacy.common.dao.FeatureModuleDAO;
import vn.ninhkieupharma.pharmacy.common.service.SingleModuleService;

public final class ProductsService extends SingleModuleService {
    public ProductsService(FeatureModuleDAO repository) {
        super(repository, "UC-08");
    }
}
