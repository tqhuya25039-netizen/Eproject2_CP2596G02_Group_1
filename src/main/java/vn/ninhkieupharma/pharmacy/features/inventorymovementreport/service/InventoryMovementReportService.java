package vn.ninhkieupharma.pharmacy.features.inventorymovementreport.service;

import vn.ninhkieupharma.pharmacy.features.common.model.ModuleRow;
import vn.ninhkieupharma.pharmacy.features.common.dao.FeatureModuleDAO;
import vn.ninhkieupharma.pharmacy.features.common.service.AbstractFeatureModuleService;
import vn.ninhkieupharma.pharmacy.features.common.model.ModuleDefinition;
import vn.ninhkieupharma.pharmacy.features.common.service.Localization;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class InventoryMovementReportService extends AbstractFeatureModuleService {
    private static final Pattern QUANTITY = Pattern.compile("[+-]?\\d+(?:[,.]\\d+)?");
    private final FeatureModuleDAO repository;

    public InventoryMovementReportService(FeatureModuleDAO repository) {
        super(repository, Set.of("UC-15"));
        this.repository = repository;
    }

    @Override
    public List<ModuleRow> findAll(ModuleDefinition module) {
        if (repository.supportsNativeReports()) {
            return super.findAll(module);
        }
        List<List<String>> movements = new ArrayList<>();
        for (ModuleRow receipt : repository.findAll("UC-12")) {
            List<String> values = receipt.values();
            movements.add(List.of(values.get(2), values.get(1), "Nhập hàng",
                    quantity(values.get(3)), "Phiếu " + values.get(0)));
        }
        for (ModuleRow stocktake : repository.findAll("UC-14")) {
            List<String> values = stocktake.values();
            String variance = values.get(3);
            if (!variance.contains("Khớp tồn") && !variance.equals(Localization.text("Khớp tồn"))) {
                movements.add(List.of(values.get(2), values.get(1), "Điều chỉnh",
                        quantity(variance), "Phiên " + values.get(0)));
            }
        }
        for (ModuleRow sample : repository.findAll("UC-15")) {
            List<String> values = sample.values();
            if (values.size() > 2
                    && (values.get(2).equals("Bán hàng")
                    || values.get(2).equals("Hư hỏng")
                    || values.get(2).equals("Sắp hết hạn"))) {
                movements.add(values);
            }
        }
        repository.replaceAll("UC-15", movements);
        return super.findAll(module);
    }

    private String quantity(String value) {
        Matcher matcher = QUANTITY.matcher(value);
        if (!matcher.find()) {
            throw new IllegalArgumentException(
                    Localization.text("Expected a numeric quantity: ") + value);
        }
        return matcher.group() + " " + value.substring(matcher.end()).trim();
    }
}
