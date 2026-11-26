package vn.ninhkieupharma.pharmacy.domain.hr;

import vn.ninhkieupharma.pharmacy.common.dao.FeatureModuleDAO;
import vn.ninhkieupharma.pharmacy.common.service.SingleModuleService;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

public final class AvailabilityService extends SingleModuleService {
    private final FeatureModuleDAO repository;

    public AvailabilityService(FeatureModuleDAO repository) {
        super(repository, "UC-02");
        this.repository = repository;
    }

    public List<String> findActiveEmployeeNames() {
        return repository.findActiveEmployeeNames();
    }

    public int createWeeklyAvailability(
            String employeeName, LocalDate dateInWeek, List<AvailabilitySlot> slots) {
        if (employeeName == null || employeeName.isBlank()) {
            throw new IllegalArgumentException("Vui lòng chọn nhân viên.");
        }
        if (dateInWeek == null) {
            throw new IllegalArgumentException("Vui lòng chọn một ngày trong tuần.");
        }
        if (slots == null || slots.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn ít nhất một ngày khả dụng.");
        }
        LocalDate weekStart = dateInWeek.with(
                TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return repository.createWeeklyAvailability(employeeName.trim(), weekStart, List.copyOf(slots));
    }
}
