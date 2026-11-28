package vn.ninhkieupharma.pharmacy.domain.hr;

import vn.ninhkieupharma.pharmacy.common.model.ModuleRow;
import vn.ninhkieupharma.pharmacy.common.dao.FeatureModuleDAO;
import vn.ninhkieupharma.pharmacy.common.model.ModuleDefinition;
import vn.ninhkieupharma.pharmacy.common.service.Localization;
import vn.ninhkieupharma.pharmacy.common.service.AbstractFeatureModuleService;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class AttendanceService extends AbstractFeatureModuleService {
<<<<<<< HEAD
    private static final DateTimeFormatter ISO_DATE_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter VIETNAMESE_DATE_TIME =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter VIETNAMESE_TIME_DATE =
            DateTimeFormatter.ofPattern("H:mm dd/MM/yyyy");
=======
>>>>>>> c910dd9e5d0954962f081bf169ddcba5f6adf653
    private final FeatureModuleDAO repository;

    public AttendanceService(FeatureModuleDAO repository) {
        super(repository, Set.of("UC-04", "UC-05"));
        this.repository = repository;
    }

    @Override
    public List<ModuleRow> findAll(ModuleDefinition module) {
        if (!module.id().equals("UC-05") || repository.supportsNativeReports()) {
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
    }

    private void validateTime(String value, boolean allowMissing) {
        if (value == null) {
            throw new IllegalArgumentException(Localization.text("Time value cannot be null."));
        }
        if (allowMissing && (value.isBlank() || value.equals("—"))) {
            return;
        }
        try {
            parseAttendanceTime(value);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException(
<<<<<<< HEAD
                    Localization.text("Time must use H:mm, HH:mm dd/MM/yyyy, dd/MM/yyyy HH:mm, "
                            + "or YYYY-MM-DD HH:mm format: ")
                            + value,
=======
                    Localization.text("Time must use H:mm or YYYY-MM-DD HH:mm format: ") + value,
>>>>>>> c910dd9e5d0954962f081bf169ddcba5f6adf653
                    exception);
        }
    }

    private LocalTime parseAttendanceTime(String value) {
        try {
            return LocalTime.parse(value, DateTimeFormatter.ofPattern("H:mm"));
        } catch (DateTimeParseException timeException) {
            try {
                return LocalDateTime.parse(value, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                        .toLocalTime();
            } catch (DateTimeParseException dateTimeException) {
                dateTimeException.addSuppressed(timeException);
                throw dateTimeException;
            }
        }
    }

<<<<<<< HEAD
    private LocalDateTime parseAttendanceDateTime(String value) {
        try {
            return LocalDateTime.parse(value, ISO_DATE_TIME);
        } catch (DateTimeParseException isoException) {
            try {
                return LocalDateTime.parse(value, VIETNAMESE_DATE_TIME);
            } catch (DateTimeParseException vietnameseException) {
                try {
                    return LocalDateTime.parse(value, VIETNAMESE_TIME_DATE);
                } catch (DateTimeParseException timeFirstException) {
                    timeFirstException.addSuppressed(isoException);
                    timeFirstException.addSuppressed(vietnameseException);
                    throw timeFirstException;
                }
            }
        }
    }

=======
>>>>>>> c910dd9e5d0954962f081bf169ddcba5f6adf653
    private double workedHours(ModuleRow row) {
        List<String> values = row.values();
        if (values.get(2).isBlank() || values.get(3).isBlank() || values.get(3).equals("—")) {
            return 0;
        }
        try {
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            try {
                LocalDateTime start = LocalDateTime.parse(values.get(2), dateTimeFormatter);
                LocalDateTime end = LocalDateTime.parse(values.get(3), dateTimeFormatter);
                return Duration.between(start, end).toMinutes() / 60.0;
            } catch (DateTimeParseException ignored) {
                LocalTime start = parseAttendanceTime(values.get(2));
                LocalTime end = parseAttendanceTime(values.get(3));
                return Duration.between(start, end).toMinutes() / 60.0;
            }
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException(
<<<<<<< HEAD
                    Localization.text("Attendance times must use H:mm, HH:mm dd/MM/yyyy, "
                            + "dd/MM/yyyy HH:mm, or YYYY-MM-DD HH:mm format: ")
=======
                    Localization.text("Attendance times must use H:mm or YYYY-MM-DD HH:mm format: ")
>>>>>>> c910dd9e5d0954962f081bf169ddcba5f6adf653
                            + values.get(2) + "–" + values.get(3),
                    exception);
        }
    }
}
