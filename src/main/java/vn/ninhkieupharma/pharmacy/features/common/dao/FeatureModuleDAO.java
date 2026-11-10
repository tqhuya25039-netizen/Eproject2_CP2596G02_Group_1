package vn.ninhkieupharma.pharmacy.features.common.dao;

import vn.ninhkieupharma.pharmacy.features.common.model.ModuleRow;

import java.util.List;
import java.util.UUID;

public interface FeatureModuleDAO {
    List<ModuleRow> findAll(String moduleId);

    default boolean supportsNativeReports() {
        return false;
    }

    void replaceAll(String moduleId, List<List<String>> values);

    ModuleRow create(String moduleId, List<String> values);

    ModuleRow update(String moduleId, UUID rowId, List<String> values);

    void delete(String moduleId, UUID rowId);
}
