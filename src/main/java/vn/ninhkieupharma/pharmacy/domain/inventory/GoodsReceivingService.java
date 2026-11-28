package vn.ninhkieupharma.pharmacy.domain.inventory;

import vn.ninhkieupharma.pharmacy.common.dao.FeatureModuleDAO;
import vn.ninhkieupharma.pharmacy.common.service.SingleModuleService;

import java.util.List;

public final class GoodsReceivingService extends SingleModuleService {
    public GoodsReceivingService(FeatureModuleDAO repository) {
        super(repository, "UC-12");
    }
}
