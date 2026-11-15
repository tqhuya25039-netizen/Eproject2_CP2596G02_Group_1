package vn.ninhkieupharma.pharmacy.domain.hr;

import vn.ninhkieupharma.pharmacy.common.dao.FeatureModuleDAO;
import vn.ninhkieupharma.pharmacy.common.model.ModuleDefinition;
import vn.ninhkieupharma.pharmacy.common.model.ModuleRow;
import vn.ninhkieupharma.pharmacy.common.service.AbstractFeatureModuleService;

import java.util.List;
import java.util.Set;

public final class StoreScheduleService extends AbstractFeatureModuleService {
    private final FeatureModuleDAO repository;

    public StoreScheduleService(FeatureModuleDAO repository) {
        super(repository, Set.of("UC-03", "UC-06"));
        this.repository = repository;
    }

    @Override
    public List<ModuleRow> findAll(ModuleDefinition module) {
        if (!module.id().equals("UC-06") || repository.supportsNativeReports()) {
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
