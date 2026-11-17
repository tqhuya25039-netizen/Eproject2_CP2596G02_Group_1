package vn.ninhkieupharma.pharmacy.domain.sales;

import vn.ninhkieupharma.pharmacy.common.model.ModuleRow;
import vn.ninhkieupharma.pharmacy.common.dao.FeatureModuleDAO;
import vn.ninhkieupharma.pharmacy.common.service.AbstractFeatureModuleService;
import vn.ninhkieupharma.pharmacy.common.model.ModuleDefinition;
import vn.ninhkieupharma.pharmacy.common.service.Localization;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.List;

public final class SalesService extends AbstractFeatureModuleService {
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ROOT);
    private final FeatureModuleDAO repository;

    public SalesService(FeatureModuleDAO repository) {
        super(repository, Set.of("UC-10", "UC-17", "UC-19"));
        this.repository = repository;
    }

    public List<SaleProduct> findSaleProducts() {
        return repository.findSaleProducts();
    }

    public List<SaleAllergyWarning> findAllergyWarnings(
            Optional<UUID> customerId, List<UUID> productIds) {
        return repository.findSaleAllergyWarnings(customerId, productIds);
    }

    public SaleReceipt completeSale(long authenticatedUserId, SaleOrderRequest request) {
        return repository.completeSale(authenticatedUserId, request);
    }

    @Override
    public List<ModuleRow> findAll(ModuleDefinition module) {
        if (repository.supportsNativeReports()
                || (!module.id().equals("UC-10") && !module.id().equals("UC-19"))) {
            return super.findAll(module);
        }
        if (module.id().equals("UC-10")) {
            return categoryTrends(module);
        }
        return revenueReport(module);
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

    private List<ModuleRow> categoryTrends(ModuleDefinition module) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        Map<String, Set<String>> dosageForms = new LinkedHashMap<>();
        Map<String, Set<String>> suppliers = new LinkedHashMap<>();
        for (ModuleRow product : repository.findAll("UC-08")) {
            List<String> values = product.values();
            if (values.size() > 2 && !values.get(2).isBlank()) {
                String category = values.get(2);
                counts.merge(category, 1, Integer::sum);
                if (values.size() > 3) {
                    dosageForms.computeIfAbsent(category, ignored -> new LinkedHashSet<>())
                            .add(values.get(3).isBlank() ? "Mặc định" : values.get(3));
                }
                if (values.size() > 7 && !values.get(7).isBlank()) {
                    suppliers.computeIfAbsent(category, ignored -> new LinkedHashSet<>())
                            .add(values.get(7));
                }
            }
        }
        int total = counts.values().stream().mapToInt(Integer::intValue).sum();
        NumberFormat percentage = NumberFormat.getPercentInstance(Locale.forLanguageTag("vi-VN"));
        percentage.setMaximumFractionDigits(1);
        YearMonth currentMonth = YearMonth.now();
        String quarter = currentMonth.getYear() + "-Q" + ((currentMonth.getMonthValue() - 1) / 3 + 1);
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

    private List<ModuleRow> revenueReport(ModuleDefinition module) {
        Map<LocalDate, DailyRevenue> totals = new TreeMap<>((left, right) -> right.compareTo(left));
        for (ModuleRow invoice : repository.findAll("UC-17")) {
            if (invoice.values().size() < 5) {
                throw new IllegalStateException("Invoice record is missing date or total values.");
            }
            LocalDate date = parseDate(invoice.values().get(1));
            long amount = parseAmount(invoice.values().get(4));
            totals.merge(date, new DailyRevenue(1, amount),
                    (left, right) -> new DailyRevenue(
                            left.count() + right.count(), left.amount() + right.amount()));
        }

        List<List<String>> report = new ArrayList<>();
        for (Map.Entry<LocalDate, DailyRevenue> entry : totals.entrySet()) {
            DailyRevenue revenue = entry.getValue();
            report.add(List.of(
                    entry.getKey().format(DATE_FORMAT),
                    Integer.toString(revenue.count()),
                    formatCurrency(revenue.amount()),
                    formatCurrency(0),
                    formatCurrency(revenue.amount() / revenue.count())));
        }
        repository.replaceAll("UC-19", report);
        return super.findAll(module);
    }

    private LocalDate parseDate(String value) {
        String date = value.length() >= 10 ? value.substring(0, 10) : value;
        try {
            return LocalDate.parse(date, DATE_FORMAT);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException(
                    Localization.text("Invoice date must use dd/MM/yyyy format: ") + value, exception);
        }
    }

    private long parseAmount(String value) {
        String digits = value.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) {
            throw new IllegalArgumentException(
                    Localization.text("Invoice total must contain a numeric amount: ") + value);
        }
        try {
            return Long.parseLong(digits);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    Localization.text("Invoice total is outside the supported range: ") + value, exception);
        }
    }

    private String formatCurrency(long amount) {
        return NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN")).format(amount) + " ₫";
    }

    private record DailyRevenue(int count, long amount) {
    }
}
