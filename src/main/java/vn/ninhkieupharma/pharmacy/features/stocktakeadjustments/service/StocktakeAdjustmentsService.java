package vn.ninhkieupharma.pharmacy.features.stocktakeadjustments.service;

import vn.ninhkieupharma.pharmacy.features.common.dao.FeatureModuleDAO;
import vn.ninhkieupharma.pharmacy.features.common.service.SingleModuleService;
import vn.ninhkieupharma.pharmacy.features.common.service.Localization;

import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

public final class StocktakeAdjustmentsService extends SingleModuleService {
    private static final Pattern QUANTITY =
            Pattern.compile("[+-]?\\d+(?:[,.]\\d+)?(?:\\s+\\p{L}+(?:\\s+\\p{L}+)*)?");

    public StocktakeAdjustmentsService(FeatureModuleDAO repository) {
        super(repository, "UC-14");
    }

    @Override
    public vn.ninhkieupharma.pharmacy.features.common.model.ModuleRow create(
            vn.ninhkieupharma.pharmacy.features.common.model.ModuleDefinition module, List<String> values) {
        validateVariance(values);
        return super.create(module, values);
    }

    @Override
    public vn.ninhkieupharma.pharmacy.features.common.model.ModuleRow update(
            vn.ninhkieupharma.pharmacy.features.common.model.ModuleDefinition module, UUID rowId, List<String> values) {
        validateVariance(values);
        return super.update(module, rowId, values);
    }

    private void validateVariance(List<String> values) {
        if (values.size() <= 3 || values.get(3) == null
                || values.get(3).equals("Khớp tồn")
                || values.get(3).equals(Localization.text("Khớp tồn"))) {
            return;
        }
        if (!QUANTITY.matcher(values.get(3).trim()).matches()) {
            throw new IllegalArgumentException(
                    Localization.text("Expected a numeric quantity: ") + values.get(3));
        }
    }
}
