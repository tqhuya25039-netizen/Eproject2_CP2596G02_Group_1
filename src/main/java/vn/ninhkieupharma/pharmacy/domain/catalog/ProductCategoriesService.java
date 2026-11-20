package vn.ninhkieupharma.pharmacy.domain.catalog;

import vn.ninhkieupharma.pharmacy.common.dao.FeatureModuleDAO;
import vn.ninhkieupharma.pharmacy.common.model.ModuleDefinition;
import vn.ninhkieupharma.pharmacy.common.model.ModuleRow;
import vn.ninhkieupharma.pharmacy.common.service.AbstractFeatureModuleService;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** UC-07 (danh mục sản phẩm) và UC-10 (báo cáo xu hướng ngành hàng theo quý). */
public final class ProductCategoriesService extends AbstractFeatureModuleService {
    private static final int CATEGORY_COLUMN = 2;
    private static final int DOSAGE_COLUMN = 3;
    private static final int STATUS_COLUMN = 7;
    private static final int SUPPLIER_COLUMN = 8;

    private final FeatureModuleDAO repository;

    public ProductCategoriesService(FeatureModuleDAO repository) {
        super(repository, Set.of("UC-07", "UC-10"));
        this.repository = repository;
    }

    @Override
    public List<ModuleRow> findAll(ModuleDefinition module) {
        if (!module.id().equals("UC-10") || repository.supportsNativeReports()) {
            return super.findAll(module);
        }
        return categoryTrends(module);
    }

    private List<ModuleRow> categoryTrends(ModuleDefinition module) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        Map<String, Set<String>> dosageForms = new LinkedHashMap<>();
        Map<String, Set<String>> suppliers = new LinkedHashMap<>();
        for (ModuleRow product : repository.findAll("UC-08")) {
            List<String> values = product.values();
            if (values.size() <= STATUS_COLUMN
                    || !isActive(values.get(STATUS_COLUMN))
                    || values.get(CATEGORY_COLUMN).isBlank()) {
                continue;
            }
            String category = values.get(CATEGORY_COLUMN);
            counts.merge(category, 1, Integer::sum);
            dosageForms.computeIfAbsent(category, ignored -> new LinkedHashSet<>())
                    .add(values.get(DOSAGE_COLUMN).isBlank() ? "Mặc định" : values.get(DOSAGE_COLUMN));
            if (values.size() > SUPPLIER_COLUMN) {
                for (String supplier : values.get(SUPPLIER_COLUMN).split(",")) {
                    if (!supplier.isBlank()) {
                        suppliers.computeIfAbsent(category, ignored -> new LinkedHashSet<>())
                                .add(supplier.trim());
                    }
                }
            }
        }

        int total = counts.values().stream().mapToInt(Integer::intValue).sum();
        NumberFormat percentage = NumberFormat.getPercentInstance(Locale.forLanguageTag("vi-VN"));
        percentage.setMaximumFractionDigits(1);
        LocalDate today = LocalDate.now();
        String quarter = today.getYear() + "-Q" + ((today.getMonthValue() - 1) / 3 + 1);
        List<List<String>> report = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            double share = total == 0 ? 0 : (double) entry.getValue() / total;
            int supplierCount = suppliers.getOrDefault(entry.getKey(), Set.of()).size();
            String assessment = entry.getValue() >= 5 && supplierCount >= 2
                    ? "Nhiều lựa chọn thay thế"
                    : entry.getValue() >= 2 ? "Có thể thay thế cơ bản" : "Ít mặt hàng - Cần mở rộng";
            String itemSummary = entry.getValue() + " SP ("
                    + dosageForms.getOrDefault(entry.getKey(), Set.of()).size() + " dạng / "
                    + supplierCount + " NCC)";
            report.add(List.of(quarter, entry.getKey(), itemSummary,
                    percentage.format(share), assessment));
        }
        repository.replaceAll("UC-10", report);
        return super.findAll(module);
    }

    private boolean isActive(String status) {
        return "ACTIVE".equalsIgnoreCase(status) || "Đang bán".equalsIgnoreCase(status);
    }
}
