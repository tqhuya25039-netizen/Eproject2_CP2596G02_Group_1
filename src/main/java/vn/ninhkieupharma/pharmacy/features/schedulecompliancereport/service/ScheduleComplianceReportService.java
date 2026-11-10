package vn.ninhkieupharma.pharmacy.features.schedulecompliancereport.service;

import vn.ninhkieupharma.pharmacy.features.common.model.ModuleRow;
import vn.ninhkieupharma.pharmacy.features.common.dao.FeatureModuleDAO;
import vn.ninhkieupharma.pharmacy.features.common.service.AbstractFeatureModuleService;
import vn.ninhkieupharma.pharmacy.features.common.model.ModuleDefinition;

import java.util.List;
import java.util.Set;

public final class ScheduleComplianceReportService extends AbstractFeatureModuleService {
    private final FeatureModuleDAO repository;

    public ScheduleComplianceReportService(FeatureModuleDAO repository) {
        super(repository, Set.of("UC-06"));
        this.repository = repository;
    }

    @Override
    public List<ModuleRow> findAll(ModuleDefinition module) {
        if (repository.supportsNativeReports()) {
            return super.findAll(module);
        }
        List<List<String>> report = repository.findAll("UC-04").stream()
                .map(ModuleRow::values)
                .map(row -> List.of(
                        row.get(0),
                        scheduledTime(row.get(1)),
                        row.get(2) + "–" + row.get(3),
                        row.get(4),
                        row.get(4)))
                .toList();
        repository.replaceAll("UC-06", report);
        return super.findAll(module);
    }

    private String scheduledTime(String shift) {
        int separator = shift.indexOf('·');
        return separator >= 0 ? shift.substring(separator + 1).trim() : shift;
    }
}
