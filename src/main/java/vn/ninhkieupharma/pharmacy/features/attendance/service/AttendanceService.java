package vn.ninhkieupharma.pharmacy.features.attendance.service;

import vn.ninhkieupharma.pharmacy.features.common.model.ModuleRow;
import vn.ninhkieupharma.pharmacy.features.common.dao.FeatureModuleDAO;
import vn.ninhkieupharma.pharmacy.features.common.service.SingleModuleService;
import vn.ninhkieupharma.pharmacy.features.common.model.ModuleDefinition;
import vn.ninhkieupharma.pharmacy.features.common.service.Localization;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class AttendanceService extends SingleModuleService {
    public AttendanceService(FeatureModuleDAO repository) {
        super(repository, "UC-04");
    }

    @Override
    public ModuleRow create(ModuleDefinition module, List<String> values) {
        validateTimes(values);
        return super.create(module, values);
    }

    @Override
    public ModuleRow update(ModuleDefinition module, UUID rowId, List<String> values) {
        validateTimes(values);
        return super.update(module, rowId, values);
    }

    private void validateTimes(List<String> values) {
        if (values.size() < 4) {
            return;
        }
        validateTime(values.get(2), false);
        validateTime(values.get(3), true);
        if (values.size() > 4
                && values.get(4) != null
                && (values.get(4).contains("Trễ")
                || values.get(4).toLowerCase(Locale.ROOT).contains("late"))
                && !values.get(4).matches(".*\\d+.*")) {
            throw new IllegalArgumentException(Localization.text(
                    "Late status must include the delay in minutes."));
        }
    }

    private void validateTime(String value, boolean allowMissing) {
        if (value == null) {
            throw new IllegalArgumentException(Localization.text("Time value cannot be null."));
        }
        if (allowMissing && (value.isBlank() || value.equals("—"))) {
            return;
        }
        try {
            LocalTime.parse(value, DateTimeFormatter.ofPattern("H:mm"));
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException(
                    Localization.text("Time must use H:mm format: ") + value, exception);
        }
    }
}
