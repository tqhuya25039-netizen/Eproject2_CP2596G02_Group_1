package vn.ninhkieupharma.pharmacy.features.common.dao;

import vn.ninhkieupharma.pharmacy.features.common.model.ModuleRow;
import vn.ninhkieupharma.pharmacy.features.common.model.ModuleDefinition;
import vn.ninhkieupharma.pharmacy.features.common.service.DemoData;
import vn.ninhkieupharma.pharmacy.features.common.service.ModuleCatalog;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

public final class InMemoryReportModuleDAO implements FeatureModuleDAO {
    private final Map<String, List<ModuleRow>> rowsByModule = new LinkedHashMap<>();

    public InMemoryReportModuleDAO() {
        for (ModuleDefinition module : ModuleCatalog.MODULES) {
            if (module.report()) {
                List<ModuleRow> rows = DemoData.rowsFor(module.id()).stream()
                        .map(values -> new ModuleRow(UUID.randomUUID(), values))
                        .toList();
                rowsByModule.put(module.id(), new ArrayList<>(rows));
            }
        }
    }

    @Override
    public List<ModuleRow> findAll(String moduleId) {
        return List.copyOf(requireModule(moduleId));
    }

    @Override
    public void replaceAll(String moduleId, List<List<String>> values) {
        requireModule(moduleId);
        rowsByModule.put(moduleId, values.stream()
                .map(row -> new ModuleRow(UUID.randomUUID(), row))
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll));
    }

    @Override
    public ModuleRow create(String moduleId, List<String> values) {
        throw new IllegalStateException("Report modules are read-only.");
    }

    @Override
    public ModuleRow update(String moduleId, UUID rowId, List<String> values) {
        throw new IllegalStateException("Report modules are read-only.");
    }

    @Override
    public void delete(String moduleId, UUID rowId) {
        throw new IllegalStateException("Report modules are read-only.");
    }

    private List<ModuleRow> requireModule(String moduleId) {
        List<ModuleRow> rows = rowsByModule.get(moduleId);
        if (rows == null) {
            throw new IllegalArgumentException("Unknown report module: " + moduleId);
        }
        return rows;
    }
}
