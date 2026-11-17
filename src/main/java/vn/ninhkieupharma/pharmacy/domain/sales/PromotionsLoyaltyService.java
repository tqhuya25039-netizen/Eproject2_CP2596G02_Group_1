package vn.ninhkieupharma.pharmacy.domain.sales;

import vn.ninhkieupharma.pharmacy.common.dao.FeatureModuleDAO;
import vn.ninhkieupharma.pharmacy.common.model.ModuleDefinition;
import vn.ninhkieupharma.pharmacy.common.model.ModuleRow;
import vn.ninhkieupharma.pharmacy.common.service.SingleModuleService;
import vn.ninhkieupharma.pharmacy.common.service.Localization;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class PromotionsLoyaltyService extends SingleModuleService {
    public PromotionsLoyaltyService(FeatureModuleDAO repository) {
        super(repository, "UC-18");
    }

    @Override
    public ModuleRow create(ModuleDefinition module, List<String> values) {
        validateConfiguration(module, values, true, null);
        return super.create(module, values);
    }

    @Override
    public ModuleRow update(ModuleDefinition module, UUID rowId, List<String> values) {
        requireValues(values);
        ModuleRow existing = findRow(module, rowId);
        boolean startDateChanged = !existing.values().get(3).equals(values.get(3));
        validateConfiguration(module, values, startDateChanged, existing);
        return super.update(module, rowId, values);
    }

    public void validateConfiguration(
            ModuleDefinition module, List<String> values, boolean validateStartDate, ModuleRow existing) {
        validate(module, values, validateStartDate, existing);
    }

    @Override
    public void delete(ModuleDefinition module, UUID rowId) {
        ModuleRow existing = findRow(module, rowId);
        List<String> values = new ArrayList<>(existing.values());
        values.set(9, "INACTIVE");
        super.update(module, rowId, values);
    }

    public void finishImmediately(ModuleDefinition module, UUID rowId) {
        ModuleRow existing = findRow(module, rowId);
        List<String> values = new ArrayList<>(existing.values());
        LocalDate today = LocalDate.now();
        LocalDate redemptionStart = LocalDate.parse(values.get(4));
        if (!today.isBefore(redemptionStart)) {
            LocalDate accrualEnd = LocalDate.parse(values.get(5));
            values.set(5, accrualEnd.isAfter(today) ? today.toString() : accrualEnd.toString());
            values.set(6, today.toString());
        }
        values.set(9, "INACTIVE");
        super.update(module, rowId, values);
    }

    private ModuleRow findRow(ModuleDefinition module, UUID rowId) {
        if (!supports(module)) {
            throw new IllegalArgumentException("Module is not supported by the promotions service.");
        }
        return findAll(module).stream()
                .filter(row -> row.id().equals(rowId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("The selected program no longer exists."));
    }

    private void validate(
            ModuleDefinition module,
            List<String> values,
            boolean validateStartDate,
            ModuleRow existing) {
        if (!supports(module)) {
            throw new IllegalArgumentException("Module is not supported by the promotions service.");
        }
        requireValues(values);
        if (values.stream().anyMatch(value -> value == null)) {
            throw new IllegalArgumentException("A points program requires 10 non-null field values.");
        }
        if (values.get(0).isBlank() || values.get(1).isBlank()) {
            throw new IllegalArgumentException("Program code and name are required.");
        }
        decimal(values.get(2), "Discount percentage", BigDecimal.ZERO, new BigDecimal("100"));
        LocalDate accrualStart = date(values.get(3), "Accrual start");
        LocalDate redemptionStart = date(values.get(4), "Redemption start");
        LocalDate accrualEnd = date(values.get(5), "Accrual end");
        LocalDate redemptionEnd = date(values.get(6), "Redemption end");
        LocalDate today = LocalDate.now();
        if (validateStartDate && accrualStart.isBefore(today)) {
            throw new IllegalArgumentException("Accrual start date cannot be before today.");
        }
        if (accrualStart.isAfter(redemptionStart)
                || redemptionStart.isAfter(accrualEnd)
                || accrualEnd.isAfter(redemptionEnd)) {
            throw new IllegalArgumentException(
                    Localization.text("Điều kiện ngày: bắt đầu tích ≤ bắt đầu đổi ≤ kết thúc tích ≤ kết thúc đổi."));
        }
        decimal(values.get(7), "Redemption value per point", BigDecimal.ZERO, null);
        if (parseDecimal(values.get(7)).signum() == 0) {
            throw new IllegalArgumentException("Redemption value per point must be greater than zero.");
        }
        validateTiers(values.get(8));
        if (!Set.of("ACTIVE", "INACTIVE").contains(values.get(9).toUpperCase(java.util.Locale.ROOT))) {
            throw new IllegalArgumentException("Program status must be ACTIVE or INACTIVE.");
        }

        if (existing != null && !validateStartDate
                && !existing.values().get(3).equals(values.get(3))) {
            throw new IllegalArgumentException("An accrual start date changed during validation.");
        }
    }

    private void requireValues(List<String> values) {
        if (values == null || values.size() != 10) {
            throw new IllegalArgumentException("A points program requires 10 non-null field values.");
        }
    }

    private void validateTiers(String encodedTiers) {
        if (encodedTiers.isBlank()) {
            throw new IllegalArgumentException("Add at least one points tier.");
        }
        Set<BigDecimal> thresholds = new HashSet<>();
        for (String encodedTier : encodedTiers.split(";", -1)) {
            String[] parts = encodedTier.split(":", -1);
            if (parts.length != 2) {
                throw new IllegalArgumentException("Each points tier must contain threshold:rate.");
            }
            BigDecimal threshold = parseDecimal(parts[0]);
            if (threshold.signum() < 0 || !thresholds.add(threshold.stripTrailingZeros())) {
                throw new IllegalArgumentException(
                        "Tier thresholds must be non-negative and unique.");
            }
            BigDecimal rate = parseDecimal(parts[1]);
            if (rate.signum() <= 0) {
                throw new IllegalArgumentException("Each points rate must be greater than zero.");
            }
        }
    }

    private BigDecimal decimal(String value, String label, BigDecimal minimum, BigDecimal maximum) {
        BigDecimal parsed = parseDecimal(value);
        if (parsed.compareTo(minimum) < 0 || (maximum != null && parsed.compareTo(maximum) > 0)) {
            String range = maximum == null ? "must be greater than or equal to zero"
                    : "must be between " + minimum + " and " + maximum;
            throw new IllegalArgumentException(label + " " + range + ".");
        }
        return parsed;
    }

    private BigDecimal parseDecimal(String value) {
        try {
            return new BigDecimal(value.trim().replace(',', '.'));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Enter a valid decimal number: " + value, exception);
        }
    }

    private LocalDate date(String value, String label) {
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException(label + " must use YYYY-MM-DD.", exception);
        }
    }
}
