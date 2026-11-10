package vn.ninhkieupharma.pharmacy.features.employeeaccounts.service;

import vn.ninhkieupharma.pharmacy.features.common.dao.FeatureModuleDAO;
import vn.ninhkieupharma.pharmacy.features.common.service.SingleModuleService;

public final class EmployeeAccountsService extends SingleModuleService {
    public EmployeeAccountsService(FeatureModuleDAO repository) {
        super(repository, "UC-01");
    }
}
