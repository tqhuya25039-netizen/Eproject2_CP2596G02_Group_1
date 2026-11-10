package vn.ninhkieupharma.pharmacy.features.common.dao;

import vn.ninhkieupharma.pharmacy.features.common.model.ModuleRow;
import vn.ninhkieupharma.pharmacy.features.common.model.ModuleDefinition;
import vn.ninhkieupharma.pharmacy.features.common.service.ModuleCatalog;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public final class RoutingFeatureModuleDAO implements FeatureModuleDAO {
    private static final Set<String> REPORT_MODULES = ModuleCatalog.MODULES.stream()
            .filter(ModuleDefinition::report)
            .map(ModuleDefinition::id)
            .collect(Collectors.toUnmodifiableSet());

    private final FeatureModuleDAO businessDAO;
    private final FeatureModuleDAO customerDAO;
    private final FeatureModuleDAO reportDAO;
    private final Map<String, FeatureModuleDAO> typedDAOs;

    public RoutingFeatureModuleDAO(FeatureModuleDAO businessDAO, FeatureModuleDAO customerDAO) {
        this(businessDAO, customerDAO, new InMemoryReportModuleDAO(), Map.of());
    }

    public RoutingFeatureModuleDAO(
            FeatureModuleDAO businessDAO,
            FeatureModuleDAO customerDAO,
            FeatureModuleDAO reportDAO,
            Map<String, FeatureModuleDAO> typedDAOs) {
        this.businessDAO = businessDAO;
        this.customerDAO = customerDAO;
        this.reportDAO = reportDAO;
        this.typedDAOs = Map.copyOf(typedDAOs);
    }

    @Override
    public List<ModuleRow> findAll(String moduleId) {
        return route(moduleId).findAll(moduleId);
    }

    @Override
    public void replaceAll(String moduleId, List<List<String>> values) {
        route(moduleId).replaceAll(moduleId, values);
    }

    @Override
    public ModuleRow create(String moduleId, List<String> values) {
        return route(moduleId).create(moduleId, values);
    }

    @Override
    public ModuleRow update(String moduleId, UUID rowId, List<String> values) {
        return route(moduleId).update(moduleId, rowId, values);
    }

    @Override
    public void delete(String moduleId, UUID rowId) {
        route(moduleId).delete(moduleId, rowId);
    }

    private FeatureModuleDAO route(String moduleId) {
        if (REPORT_MODULES.contains(moduleId)) {
            return reportDAO;
        }
        if ("UC-16".equals(moduleId)) {
            return customerDAO;
        }
        FeatureModuleDAO typedDAO = typedDAOs.get(moduleId);
        if (typedDAO != null) {
            return typedDAO;
        }
        return businessDAO;
    }
}
