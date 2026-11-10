package vn.ninhkieupharma.pharmacy.features.workhoursreport.service;

import vn.ninhkieupharma.pharmacy.features.common.model.ModuleRow;
import vn.ninhkieupharma.pharmacy.features.common.dao.FeatureModuleDAO;
import vn.ninhkieupharma.pharmacy.features.common.service.AbstractFeatureModuleService;
import vn.ninhkieupharma.pharmacy.features.common.model.ModuleDefinition;
import vn.ninhkieupharma.pharmacy.features.common.service.Localization;

import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class WorkHoursReportService extends AbstractFeatureModuleService {
    private final FeatureModuleDAO repository;

    public WorkHoursReportService(FeatureModuleDAO repository) {
        super(repository, Set.of("UC-05"));
        this.repository = repository;
    }

    @Override
    public List<ModuleRow> findAll(ModuleDefinition module) {
        if (repository.supportsNativeReports()) {
            return super.findAll(module);
        }
        Map<String, List<ModuleRow>> byEmployee = new LinkedHashMap<>();
        for (ModuleRow attendance : repository.findAll("UC-04")) {
            byEmployee.computeIfAbsent(attendance.values().get(0), ignored -> new ArrayList<>())
                    .add(attendance);
        }
        List<List<String>> report = new ArrayList<>();
        for (Map.Entry<String, List<ModuleRow>> entry : byEmployee.entrySet()) {
            double hours = entry.getValue().stream().mapToDouble(this::workedHours).sum();
            String status = entry.getValue().stream()
                    .anyMatch(row -> row.values().get(4).contains("Đang làm việc")
                            || row.values().get(4).equalsIgnoreCase("Working"))
                    ? "Đang làm việc" : "Dữ liệu mẫu";
            report.add(List.of(
                    entry.getKey(),
                    "11/2026",
                    Integer.toString(entry.getValue().size()),
                    String.format(Locale.forLanguageTag("vi-VN"), "%.1f giờ", hours),
                    status));
        }
        repository.replaceAll("UC-05", report);
        return super.findAll(module);
    }

    private double workedHours(ModuleRow row) {
        List<String> values = row.values();
        if (values.get(2).isBlank() || values.get(3).isBlank() || values.get(3).equals("—")) {
            return 0;
        }
        try {
            LocalTime start = LocalTime.parse(values.get(2), DateTimeFormatter.ofPattern("H:mm"));
            LocalTime end = LocalTime.parse(values.get(3), DateTimeFormatter.ofPattern("H:mm"));
            return Duration.between(start, end).toMinutes() / 60.0;
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException(
                    Localization.text("Attendance times must use H:mm format: ")
                            + values.get(2) + "–" + values.get(3),
                    exception);
        }
    }
}
