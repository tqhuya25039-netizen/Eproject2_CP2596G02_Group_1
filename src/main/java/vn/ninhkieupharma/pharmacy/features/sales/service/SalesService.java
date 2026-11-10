package vn.ninhkieupharma.pharmacy.features.sales.service;

import vn.ninhkieupharma.pharmacy.features.common.model.ModuleRow;
import vn.ninhkieupharma.pharmacy.features.common.dao.FeatureModuleDAO;
import vn.ninhkieupharma.pharmacy.features.common.service.SingleModuleService;
import vn.ninhkieupharma.pharmacy.features.common.model.ModuleDefinition;
import vn.ninhkieupharma.pharmacy.features.common.service.Localization;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class SalesService extends SingleModuleService {
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ROOT);

    public SalesService(FeatureModuleDAO repository) {
        super(repository, "UC-17");
    }

    @Override
    public ModuleRow create(ModuleDefinition module, List<String> values) {
        validateInvoice(values);
        return super.create(module, values);
    }

    @Override
    public ModuleRow update(ModuleDefinition module, UUID rowId, List<String> values) {
        validateInvoice(values);
        return super.update(module, rowId, values);
    }

    private void validateInvoice(List<String> values) {
        if (values.size() != 6) {
            return;
        }
        if (values.get(1) == null || values.get(4) == null) {
            throw new IllegalArgumentException(Localization.text("Invoice date and total are required."));
        }
        String date = values.get(1).length() >= 10 ? values.get(1).substring(0, 10) : values.get(1);
        try {
            LocalDate.parse(date, DATE_FORMAT);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException(
                    Localization.text("Invoice date must use dd/MM/yyyy format: ") + values.get(1), exception);
        }
        String digits = values.get(4).replaceAll("[^0-9]", "");
        if (digits.isEmpty()) {
            throw new IllegalArgumentException(
                    Localization.text("Invoice total must contain a numeric amount: ") + values.get(4));
        }
        try {
            Long.parseLong(digits);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    Localization.text("Invoice total is outside the supported range: ") + values.get(4),
                    exception);
        }
    }
}
