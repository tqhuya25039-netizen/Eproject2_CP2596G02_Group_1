package vn.ninhkieupharma.pharmacy.features.goodsreceiving.service;

import vn.ninhkieupharma.pharmacy.features.common.dao.FeatureModuleDAO;
import vn.ninhkieupharma.pharmacy.features.common.service.SingleModuleService;
import vn.ninhkieupharma.pharmacy.features.common.service.Localization;

import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class GoodsReceivingService extends SingleModuleService {
    private static final Pattern QUANTITY =
            Pattern.compile("[+-]?\\d+(?:[,.]\\d+)?(?:\\s+\\p{L}+(?:\\s+\\p{L}+)*)?");

    public GoodsReceivingService(FeatureModuleDAO repository) {
        super(repository, "UC-12");
    }

    @Override
    public vn.ninhkieupharma.pharmacy.features.common.model.ModuleRow create(
            vn.ninhkieupharma.pharmacy.features.common.model.ModuleDefinition module, List<String> values) {
        validateQuantity(values);
        return super.create(module, values);
    }

    @Override
    public vn.ninhkieupharma.pharmacy.features.common.model.ModuleRow update(
            vn.ninhkieupharma.pharmacy.features.common.model.ModuleDefinition module, UUID rowId, List<String> values) {
        validateQuantity(values);
        return super.update(module, rowId, values);
    }

    private void validateQuantity(List<String> values) {
        if (values.size() <= 3 || values.get(3) == null) {
            return;
        }
        Matcher matcher = QUANTITY.matcher(values.get(3).trim());
        if (!matcher.matches()) {
            throw new IllegalArgumentException(
                    Localization.text("Expected a numeric quantity: ") + values.get(3));
        }
    }
}
