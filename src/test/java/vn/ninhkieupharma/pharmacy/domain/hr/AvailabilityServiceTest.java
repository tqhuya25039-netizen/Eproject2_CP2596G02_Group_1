package vn.ninhkieupharma.pharmacy.domain.hr;

import org.junit.jupiter.api.Test;
import vn.ninhkieupharma.pharmacy.common.dao.FeatureModuleDAO;
import vn.ninhkieupharma.pharmacy.common.model.ModuleRow;
import vn.ninhkieupharma.pharmacy.common.repository.InMemoryModuleRepository;
import vn.ninhkieupharma.pharmacy.common.time.VietnamDate;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AvailabilityServiceTest {
    @Test
    void rejectsPastAndOutOfRangeDatesBeforeCallingRepository() {
        AvailabilityService service = new AvailabilityService(new InMemoryModuleRepository());
        LocalDate today = VietnamDate.today();

        assertThrows(IllegalArgumentException.class, () -> service.createWeeklyAvailability(
                "Nguyen Thi An", today.minusDays(1),
                List.of(new AvailabilitySlot(today, LocalTime.of(8, 0), LocalTime.NOON, ""))));
        assertThrows(IllegalArgumentException.class, () -> service.createWeeklyAvailability(
                "Nguyen Thi An", today,
                List.of(new AvailabilitySlot(today.plusYears(1).plusDays(1),
                        LocalTime.of(8, 0), LocalTime.NOON, ""))));
    }

    @Test
    void registersSelectedDaysUsingTheMondayOfTheChosenWeek() {
        FeatureModuleDAO delegate = new InMemoryModuleRepository();
        AtomicReference<LocalDate> receivedWeekStart = new AtomicReference<>();
        AtomicReference<List<AvailabilitySlot>> receivedSlots = new AtomicReference<>();
        FeatureModuleDAO repository = new FeatureModuleDAO() {
            @Override
            public List<ModuleRow> findAll(String moduleId) {
                return delegate.findAll(moduleId);
            }

            @Override
            public void replaceAll(String moduleId, List<List<String>> values) {
                delegate.replaceAll(moduleId, values);
            }

            @Override
            public ModuleRow create(String moduleId, List<String> values) {
                return delegate.create(moduleId, values);
            }

            @Override
            public ModuleRow update(String moduleId, UUID rowId, List<String> values) {
                return delegate.update(moduleId, rowId, values);
            }

            @Override
            public void delete(String moduleId, UUID rowId) {
                delegate.delete(moduleId, rowId);
            }

            @Override
            public int createWeeklyAvailability(
                    String employeeName, LocalDate weekStart, List<AvailabilitySlot> slots) {
                assertEquals("Nguyen Thi An", employeeName);
                receivedWeekStart.set(weekStart);
                receivedSlots.set(slots);
                return slots.size();
            }
        };
        LocalDate today = VietnamDate.today();
        LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        List<AvailabilitySlot> slots = List.of(
                new AvailabilitySlot(today, LocalTime.of(8, 0), LocalTime.NOON, ""));

        int created = new AvailabilityService(repository).createWeeklyAvailability(
                "  Nguyen Thi An  ", today, slots);

        assertEquals(1, created);
        assertEquals(monday, receivedWeekStart.get());
        assertEquals(slots, receivedSlots.get());
    }
}
