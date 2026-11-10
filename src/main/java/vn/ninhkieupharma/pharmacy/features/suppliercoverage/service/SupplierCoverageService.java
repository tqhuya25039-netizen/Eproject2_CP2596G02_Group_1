package vn.ninhkieupharma.pharmacy.features.suppliercoverage.service;

import vn.ninhkieupharma.pharmacy.features.common.model.ModuleRow;
import vn.ninhkieupharma.pharmacy.features.common.dao.FeatureModuleDAO;
import vn.ninhkieupharma.pharmacy.features.common.service.AbstractFeatureModuleService;
import vn.ninhkieupharma.pharmacy.features.common.model.ModuleDefinition;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class SupplierCoverageService extends AbstractFeatureModuleService {
    private final FeatureModuleDAO repository;

    public SupplierCoverageService(FeatureModuleDAO repository) {
        super(repository, Set.of("UC-11"));
        this.repository = repository;
    }

    @Override
    public List<ModuleRow> findAll(ModuleDefinition module) {
        if (repository.supportsNativeReports()) {
            return super.findAll(module);
        }
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (ModuleRow product : repository.findAll("UC-08")) {
            List<String> values = product.values();
            if (values.size() > 6 && !values.get(6).isBlank()) {
                counts.merge(values.get(6), 1, Integer::sum);
            }
        }
        int total = counts.values().stream().mapToInt(Integer::intValue).sum();
        NumberFormat percentage = NumberFormat.getPercentInstance(Locale.forLanguageTag("vi-VN"));
        percentage.setMaximumFractionDigits(1);
        List<List<String>> report = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            double share = total == 0 ? 0 : (double) entry.getValue() / total;
            String dependency = share >= 0.4 ? "Cao" : share >= 0.25 ? "Trung bình" : "Thấp";
            report.add(List.of(entry.getKey(), Integer.toString(entry.getValue()),
                    percentage.format(share), dependency));
        }
        repository.replaceAll("UC-11", report);
        return super.findAll(module);
    }
}
