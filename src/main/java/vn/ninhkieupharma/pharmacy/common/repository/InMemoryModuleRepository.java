package vn.ninhkieupharma.pharmacy.common.repository;

import vn.ninhkieupharma.pharmacy.common.model.ModuleRow;
import vn.ninhkieupharma.pharmacy.common.dao.FeatureModuleDAO;
import vn.ninhkieupharma.pharmacy.common.service.DemoData;
import vn.ninhkieupharma.pharmacy.common.service.ModuleCatalog;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

public final class InMemoryModuleRepository implements FeatureModuleDAO {
    private final Map<String, List<ModuleRow>> rowsByModule = new LinkedHashMap<>();

    public InMemoryModuleRepository() {
        for (var module : ModuleCatalog.MODULES) {
            List<ModuleRow> rows = DemoData.rowsFor(module.id()).stream()
                    .map(values -> new ModuleRow(UUID.randomUUID(), values))
                    .toList();
            rowsByModule.put(module.id(), new ArrayList<>(rows));
        }
    }

    @Override
    public List<ModuleRow> findAll(String moduleId) {
        return List.copyOf(requireModule(moduleId));
    }

    @Override
    public void replaceAll(String moduleId, List<List<String>> values) {
        List<ModuleRow> rows = values.stream()
                .map(row -> new ModuleRow(UUID.randomUUID(), row))
                .toList();
        rowsByModule.put(moduleId, new ArrayList<>(rows));
    }

    @Override
    public ModuleRow create(String moduleId, List<String> values) {
        List<String> rowValues = "UC-01".equals(moduleId)
                ? List.of(values.get(0), values.get(1), values.get(2), values.get(3), values.get(4),
                        "Nhân viên", "Đang hoạt động")
                : values;
        ModuleRow row = new ModuleRow(UUID.randomUUID(), rowValues);
        requireModule(moduleId).add(row);
        return row;
    }

    @Override
    public ModuleRow update(String moduleId, UUID rowId, List<String> values) {
        List<ModuleRow> rows = requireModule(moduleId);
        for (int index = 0; index < rows.size(); index++) {
            if (rows.get(index).id().equals(rowId)) {
                ModuleRow updated = new ModuleRow(rowId, values);
                rows.set(index, updated);
                return updated;
            }
        }
        throw new NoSuchElementException("Could not find row " + rowId + " in " + moduleId + ".");
    }

    @Override
    public void delete(String moduleId, UUID rowId) {
        List<ModuleRow> rows = requireModule(moduleId);
        if (!rows.removeIf(row -> row.id().equals(rowId))) {
            throw new NoSuchElementException("Could not find row " + rowId + " in " + moduleId + ".");
        }
    }

    private List<ModuleRow> requireModule(String moduleId) {
        List<ModuleRow> rows = rowsByModule.get(moduleId);
        if (rows == null) {
            throw new IllegalArgumentException("Unknown module: " + moduleId);
        }
        return rows;
    }
}
