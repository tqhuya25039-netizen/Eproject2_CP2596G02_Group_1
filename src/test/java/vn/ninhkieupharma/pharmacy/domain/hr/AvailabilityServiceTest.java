package vn.ninhkieupharma.pharmacy.domain.hr;

import org.junit.jupiter.api.Test;
import vn.ninhkieupharma.pharmacy.common.dao.FeatureModuleDAO;
import vn.ninhkieupharma.pharmacy.common.model.ModuleRow;
import vn.ninhkieupharma.pharmacy.common.repository.InMemoryModuleRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AvailabilityServiceTest {
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
        LocalDate monday = LocalDate.of(2026, 11, 23);
        List<AvailabilitySlot> slots = List.of(
                new AvailabilitySlot(monday, LocalTime.of(8, 0), LocalTime.NOON, ""),
                new AvailabilitySlot(monday.plusDays(2), LocalTime.NOON, LocalTime.of(18, 0), ""));

        int created = new AvailabilityService(repository).createWeeklyAvailability(
                "  Nguyen Thi An  ", monday.plusDays(3), slots);

        assertEquals(2, created);
        assertEquals(monday, receivedWeekStart.get());
        assertEquals(slots, receivedSlots.get());
    }
}
