package vn.ninhkieupharma.pharmacy.domain.hr;

import vn.ninhkieupharma.pharmacy.common.dao.FeatureModuleDAO;
import vn.ninhkieupharma.pharmacy.common.service.SingleModuleService;
import vn.ninhkieupharma.pharmacy.common.time.VietnamDate;

public final class AvailabilityService extends SingleModuleService {
    public AvailabilityService(FeatureModuleDAO repository) {
        super(repository, "UC-02");
<<<<<<< HEAD
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
        VietnamDate.requireSchedulingDate(dateInWeek, "Ngày đã chọn");
        if (slots == null || slots.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn ít nhất một ngày khả dụng.");
        }
        for (AvailabilitySlot slot : slots) {
            if (slot == null) {
                throw new IllegalArgumentException("Ngày khả dụng không hợp lệ.");
            }
            VietnamDate.requireSchedulingDate(slot.date(), "Ngày khả dụng");
        }
        LocalDate weekStart = dateInWeek.with(
                TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return repository.createWeeklyAvailability(employeeName.trim(), weekStart, List.copyOf(slots));
=======
>>>>>>> c910dd9e5d0954962f081bf169ddcba5f6adf653
    }
}
