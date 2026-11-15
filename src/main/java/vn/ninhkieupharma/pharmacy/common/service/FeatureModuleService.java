package vn.ninhkieupharma.pharmacy.common.service;

import vn.ninhkieupharma.pharmacy.common.model.ModuleRow;
import vn.ninhkieupharma.pharmacy.common.model.ModuleDefinition;

import java.util.List;
import java.util.UUID;

public interface FeatureModuleService {
    boolean supports(ModuleDefinition module);

    List<ModuleRow> findAll(ModuleDefinition module);

    ModuleRow create(ModuleDefinition module, List<String> values);

    ModuleRow update(ModuleDefinition module, UUID rowId, List<String> values);

    void delete(ModuleDefinition module, UUID rowId);
}
