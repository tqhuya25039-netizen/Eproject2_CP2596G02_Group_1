package vn.ninhkieupharma.pharmacy.domain.sales;

import vn.ninhkieupharma.pharmacy.common.model.ModuleRow;
import vn.ninhkieupharma.pharmacy.common.dao.FeatureModuleDAO;
import vn.ninhkieupharma.pharmacy.common.service.AbstractFeatureModuleService;
import vn.ninhkieupharma.pharmacy.common.model.ModuleDefinition;
import vn.ninhkieupharma.pharmacy.common.service.Localization;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

public final class SalesService extends AbstractFeatureModuleService {
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ROOT);
    private final FeatureModuleDAO repository;

    public SalesService(FeatureModuleDAO repository) {
        super(repository, Set.of("UC-17", "UC-19"));
        this.repository = repository;
    }

    public List<SaleProduct> findSaleProducts() {
        return repository.findSaleProducts();
    }

    public List<SaleStockShortage> previewSaleStock(List<SaleLineRequest> lines) {
        return repository.previewSaleStock(lines);
    }

    public List<SaleAllergyWarning> findAllergyWarnings(
            Optional<UUID> customerId, List<UUID> productIds) {
        return repository.findSaleAllergyWarnings(customerId, productIds);
    }

    public SaleReceipt completeSale(long authenticatedUserId, SaleOrderRequest request) {
        return repository.completeSale(authenticatedUserId, request);
    }

    public SalePendingOrder createPendingSale(long authenticatedUserId, SaleOrderRequest request) {
        return repository.createPendingSale(authenticatedUserId, request);
    }

    public SalePendingOrder updatePendingSale(
            long authenticatedUserId, UUID saleId, long revision, SaleOrderRequest request) {
        return repository.updatePendingSale(authenticatedUserId, saleId, revision, request);
    }

    public SalePendingOrder findPendingSale(UUID saleId) {
        return repository.findPendingSale(saleId);
    }

    public SaleQuote quotePendingSale(
            long authenticatedUserId, UUID saleId, long revision, SaleOrderRequest request) {
        return repository.quotePendingSale(authenticatedUserId, saleId, revision, request);
    }

    public SaleReceipt completePendingSale(
            long authenticatedUserId, UUID saleId, long revision, SaleOrderRequest request, SaleQuote acceptedQuote) {
        return repository.completePendingSale(authenticatedUserId, saleId, revision, request, acceptedQuote);
    }

    public void cancelPendingSale(long authenticatedUserId, UUID saleId, long revision) {
        repository.cancelPendingSale(authenticatedUserId, saleId, revision);
    }

    public List<ModuleRow> findSaleDetails(UUID saleId) {
        return repository.findSaleDetails(saleId);
    }

    public SalesReport salesReport(SalesReport.Period period, int year, int selection) {
        LocalDate start = period.start(year, selection);
        List<SalesReport.Row> days = completedSalesByDay(start, period.endExclusive(start));
        Map<LocalDate, SalesReport.Totals> grouped = new TreeMap<>();
        for (SalesReport.Row day : days) {
            grouped.merge(period.groupStart(day.start()), day.totals(), SalesReport.Totals::plus);
        }
        List<SalesReport.Row> rows = grouped.entrySet().stream()
                .map(entry -> new SalesReport.Row(entry.getKey(), entry.getValue()))
                .toList();
        return new SalesReport(period, start, rows);
    }

    public SalesReport.Totals completedSalesTotals() {
        return completedSalesByDay(null, null).stream()
                .map(SalesReport.Row::totals)
                .reduce(SalesReport.Totals.ZERO, SalesReport.Totals::plus);
    }

    @Override
    public List<ModuleRow> findAll(ModuleDefinition module) {
        if (repository.supportsNativeReports() || !module.id().equals("UC-19")) {
            return super.findAll(module);
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

    private List<ModuleRow> revenueReport(ModuleDefinition module) {
        List<List<String>> report = completedSalesByDay(null, null).reversed().stream()
                .map(day -> {
                    SalesReport.Totals totals = day.totals();
                    return List.of(day.start().format(DATE_FORMAT),
                            Long.toString(totals.orderCount()),
                            formatCurrency(totals.revenue()),
                            formatCurrency(totals.discount()),
                            formatCurrency(totals.revenue().divideToIntegralValue(
                                    BigDecimal.valueOf(totals.orderCount()))));
                })
                .toList();
        repository.replaceAll("UC-19", report);
        return super.findAll(module);
    }

    private List<SalesReport.Row> completedSalesByDay(LocalDate fromInclusive, LocalDate toExclusive) {
        if (repository.supportsNativeReports()) {
            return repository.findCompletedSalesByDay(fromInclusive, toExclusive);
        }
        Map<LocalDate, SalesReport.Totals> totals = new TreeMap<>();
        for (ModuleRow invoice : repository.findAll("UC-17")) {
            if (invoice.values().size() < 6) {
                throw new IllegalStateException("Invoice record is missing date, total or status.");
            }
            String status = invoice.values().get(5);
            if (!"COMPLETED".equalsIgnoreCase(status) && !"Hoàn tất".equalsIgnoreCase(status)) {
                continue;
            }
            LocalDate date = parseDate(invoice.values().get(1));
            if (fromInclusive != null && (date.isBefore(fromInclusive) || !date.isBefore(toExclusive))) {
                continue;
            }
            BigDecimal amount = BigDecimal.valueOf(parseAmount(invoice.values().get(4)));
            totals.merge(date, new SalesReport.Totals(1, amount, BigDecimal.ZERO), SalesReport.Totals::plus);
        }
        return totals.entrySet().stream()
                .map(entry -> new SalesReport.Row(entry.getKey(), entry.getValue()))
                .toList();
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

    private String formatCurrency(BigDecimal amount) {
        return NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN")).format(amount) + " ₫";
    }
}
