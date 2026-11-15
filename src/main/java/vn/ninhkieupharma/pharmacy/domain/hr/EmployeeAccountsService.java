package vn.ninhkieupharma.pharmacy.domain.hr;

import vn.ninhkieupharma.pharmacy.common.dao.FeatureModuleDAO;
import vn.ninhkieupharma.pharmacy.common.service.SingleModuleService;

public final class EmployeeAccountsService extends SingleModuleService {
    public EmployeeAccountsService(FeatureModuleDAO repository) {
        super(repository, "UC-01");
    }
}
