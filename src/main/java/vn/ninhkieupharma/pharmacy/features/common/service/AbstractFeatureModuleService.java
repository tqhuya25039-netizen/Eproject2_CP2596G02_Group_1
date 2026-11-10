package vn.ninhkieupharma.pharmacy.features.common.service;

import vn.ninhkieupharma.pharmacy.features.common.model.ModuleRow;
import vn.ninhkieupharma.pharmacy.features.common.dao.FeatureModuleDAO;
import vn.ninhkieupharma.pharmacy.features.common.model.ModuleDefinition;
import vn.ninhkieupharma.pharmacy.features.common.service.Localization;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public abstract class AbstractFeatureModuleService implements FeatureModuleService {
    private final FeatureModuleDAO repository;
    private final Set<String> moduleIds;

    protected AbstractFeatureModuleService(
            FeatureModuleDAO repository, Set<String> moduleIds) {
        this.repository = repository;
        this.moduleIds = Set.copyOf(moduleIds);
    }

    @Override
    public boolean supports(ModuleDefinition module) {
        return moduleIds.contains(module.id());
    }

    @Override
    public List<ModuleRow> findAll(ModuleDefinition module) {
        requireSupported(module);
        return repository.findAll(module.id());
    }

    @Override
    public ModuleRow create(ModuleDefinition module, List<String> values) {
        requireWritable(module);
        validate(module, values);
        return repository.create(module.id(), values);
    }

    @Override
    public ModuleRow update(ModuleDefinition module, UUID rowId, List<String> values) {
        requireWritable(module);
        validate(module, values);
        return repository.update(module.id(), rowId, values);
    }

    @Override
    public void delete(ModuleDefinition module, UUID rowId) {
        requireWritable(module);
        repository.delete(module.id(), rowId);
    }

    private void requireWritable(ModuleDefinition module) {
        requireSupported(module);
        if (module.report()) {
            throw new IllegalStateException(Localization.text("Report modules are read-only."));
        }
    }

    private void requireSupported(ModuleDefinition module) {
        if (!supports(module)) {
            throw new IllegalArgumentException("Module is not supported by this feature service: " + module.id());
        }
    }

    private void validate(ModuleDefinition module, List<String> values) {
        if (values.size() != module.columns().size()) {
            throw new IllegalArgumentException(
                    Localization.text("Expected ") + module.columns().size()
                            + Localization.text(" values."));
        }
        if (values.stream().anyMatch(value -> value == null)) {
            throw new IllegalArgumentException(Localization.text("Fields cannot be null."));
        }
        if (values.stream().allMatch(String::isBlank)) {
            throw new IllegalArgumentException(
                    Localization.text("Vui lòng nhập ít nhất một giá trị."));
        }
    }
}
