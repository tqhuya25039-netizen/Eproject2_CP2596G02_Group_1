package vn.ninhkieupharma.pharmacy.domain.sales;

import vn.ninhkieupharma.pharmacy.common.dao.FeatureModuleDAO;
import vn.ninhkieupharma.pharmacy.common.model.ModuleDefinition;
import vn.ninhkieupharma.pharmacy.common.model.ModuleRow;
import vn.ninhkieupharma.pharmacy.common.service.SingleModuleService;
import vn.ninhkieupharma.pharmacy.common.service.Localization;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class PromotionsLoyaltyService extends SingleModuleService {
    private final FeatureModuleDAO repository;

    public PromotionsLoyaltyService(FeatureModuleDAO repository) {
        super(repository, "UC-18");
        this.repository = repository;
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
        requirePromotionModule(module);
        repository.deactivatePromotion(rowId, Optional.empty());
    }

    public void finishImmediately(ModuleDefinition module, UUID rowId) {
        requirePromotionModule(module);
        repository.deactivatePromotion(rowId, Optional.of(LocalDate.now()));
    }

    private ModuleRow findRow(ModuleDefinition module, UUID rowId) {
        requirePromotionModule(module);
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
        requirePromotionModule(module);
        requireValues(values);
        if (values.get(0).isBlank() || values.get(1).isBlank()) {
            throw new IllegalArgumentException("Program code and name are required.");
        }
        if (values.get(0).length() > 30 || values.get(1).length() > 120) {
            throw new IllegalArgumentException("Program code and name may contain up to 30 and 120 characters.");
        }
        BigDecimal discount = decimal(values.get(2), "Discount percentage", 5, 2);
        if (discount.signum() < 0 || discount.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("Discount percentage must be between 0 and 100.");
        }
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
        if (decimal(values.get(7), "Redemption value per point", 20, 8).signum() <= 0) {
            throw new IllegalArgumentException("Redemption value per point must be greater than zero.");
        }
        validateTiers(values.get(8));
        if (!Set.of("ACTIVE", "INACTIVE").contains(values.get(9).toUpperCase(Locale.ROOT))) {
            throw new IllegalArgumentException("Program status must be ACTIVE or INACTIVE.");
        }

        if (existing != null && !validateStartDate
                && !existing.values().get(3).equals(values.get(3))) {
            throw new IllegalArgumentException("An accrual start date changed during validation.");
        }
    }

    private void requirePromotionModule(ModuleDefinition module) {
        if (!supports(module) || module.report()) {
            throw new IllegalArgumentException("Module is not supported by the promotions service.");
        }
    }

    private void requireValues(List<String> values) {
        if (values == null || values.size() != 10 || values.stream().anyMatch(value -> value == null)) {
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
            BigDecimal threshold = decimal(parts[0], "Tier threshold", 20, 2);
            if (threshold.signum() < 0 || !thresholds.add(threshold)) {
                throw new IllegalArgumentException(
                        "Tier thresholds must be non-negative and unique.");
            }
            BigDecimal rate = decimal(parts[1], "Tier rate", 20, 8);
            if (rate.signum() <= 0) {
                throw new IllegalArgumentException("Each points rate must be greater than zero.");
            }
        }
    }

    private BigDecimal decimal(String value, String label, int precision, int scale) {
        BigDecimal parsed;
        try {
            parsed = new BigDecimal(value.trim().replace(',', '.')).stripTrailingZeros();
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Enter a valid decimal number: " + value, exception);
        }
        if (parsed.scale() > scale || parsed.abs().compareTo(BigDecimal.TEN.pow(precision - scale)) >= 0) {
            throw new IllegalArgumentException(label + " is outside the supported precision.");
        }
        return parsed;
    }

    private LocalDate date(String value, String label) {
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException(label + " must use YYYY-MM-DD.", exception);
        }
    }
}
