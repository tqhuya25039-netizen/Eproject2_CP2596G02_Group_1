package vn.ninhkieupharma.pharmacy.domain.catalog;

import vn.ninhkieupharma.pharmacy.common.dao.FeatureModuleDAO;
import vn.ninhkieupharma.pharmacy.common.model.ModuleDefinition;
import vn.ninhkieupharma.pharmacy.common.model.ModuleRow;
import vn.ninhkieupharma.pharmacy.common.service.AbstractFeatureModuleService;

import java.text.NumberFormat;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class SuppliersService extends AbstractFeatureModuleService {
    private final FeatureModuleDAO repository;

    public SuppliersService(FeatureModuleDAO repository) {
        super(repository, Set.of("UC-09", "UC-11"));
        this.repository = repository;
    }

    @Override
    public List<ModuleRow> findAll(ModuleDefinition module) {
        if (!module.id().equals("UC-11") || repository.supportsNativeReports()) {
            return super.findAll(module);
        }
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (ModuleRow product : repository.findAll("UC-08")) {
            List<String> values = product.values();
            if (values.size() > 7 && !values.get(7).isBlank()) {
                counts.merge(values.get(7), 1, Integer::sum);
            }
        }
        int total = counts.values().stream().mapToInt(Integer::intValue).sum();
        NumberFormat percentage = NumberFormat.getPercentInstance(Locale.forLanguageTag("vi-VN"));
        percentage.setMaximumFractionDigits(1);
        String month = YearMonth.now().toString();
        List<List<String>> report = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            double share = total == 0 ? 0 : (double) entry.getValue() / total;
            String dependency = share >= 0.4 ? "Cao" : share >= 0.25 ? "Trung bình" : "Thấp";
            report.add(List.of(month, entry.getKey(), Integer.toString(entry.getValue()),
                    percentage.format(share), dependency));
        }
        repository.replaceAll("UC-11", report);
        return super.findAll(module);
    }
}
