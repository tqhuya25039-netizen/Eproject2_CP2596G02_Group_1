package vn.ninhkieupharma.pharmacy.common.dao;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import vn.ninhkieupharma.pharmacy.common.auth.User;
import vn.ninhkieupharma.pharmacy.common.auth.UserRole;
import vn.ninhkieupharma.pharmacy.common.time.VietnamDate;
import vn.ninhkieupharma.pharmacy.domain.hr.AvailabilityService;
import vn.ninhkieupharma.pharmacy.domain.hr.AvailabilitySlot;

import java.sql.ResultSet;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@EnabledIfEnvironmentVariable(named = MySqlTestSchema.ENABLED_ENV, matches = "true")
class AvailabilityMySqlIntegrationTest {
    @Test
    void savesOnlyAvailabilityAndRollsBackTheWholeWeekOnAnOverlap() throws Exception {
        try (MySqlTestSchema schema = MySqlTestSchema.withCurrentSchema()) {
            schema.runInChild(WeeklyAvailabilityScenario.class);
        }
    }

    public static final class WeeklyAvailabilityScenario {
        public static void main(String[] args) throws Exception {
            RelationalFeatureModuleDAO repository = new RelationalFeatureModuleDAO(
                    new User(1, "admin_demo", "", UserRole.ADMIN, true));
            AvailabilityService service = new AvailabilityService(repository);
            LocalDate weekStart = VietnamDate.today().plusDays(7)
                    .with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
            List<AvailabilitySlot> slots = List.of(
                    new AvailabilitySlot(weekStart, LocalTime.of(8, 0), LocalTime.NOON, "Week test"),
                    new AvailabilitySlot(weekStart.plusDays(1), LocalTime.of(13, 0),
                            LocalTime.of(17, 0), "Week test"));

            assertEquals(2, service.createWeeklyAvailability(
                    "Nguyen Thi An", weekStart.plusDays(3), slots));
            assertThrows(IllegalArgumentException.class, () -> service.createWeeklyAvailability(
                    "Nguyen Thi An", weekStart, List.of(
                            new AvailabilitySlot(weekStart.plusDays(2), LocalTime.of(8, 0),
                                    LocalTime.NOON, "Should roll back"),
                            new AvailabilitySlot(weekStart, LocalTime.of(9, 0),
                                    LocalTime.of(10, 0), "Conflicts"))));

            try (var connection = vn.ninhkieupharma.pharmacy.config.DatabaseConnection.getConnection();
                 var statement = connection.prepareStatement("""
                         SELECT
                             (SELECT COUNT(*) FROM employee_availabilities
                              WHERE employee_id = 1 AND available_date BETWEEN ? AND ?) AS availability_count,
                             (SELECT COUNT(*) FROM work_schedules WHERE week_start_date = ?) AS schedule_count,
                             (SELECT COUNT(*) FROM schedule_assignments
                              WHERE work_date BETWEEN ? AND ?) AS assignment_count
                         """)) {
                statement.setString(1, weekStart.toString());
                statement.setString(2, weekStart.plusDays(6).toString());
                statement.setString(3, weekStart.toString());
                statement.setString(4, weekStart.toString());
                statement.setString(5, weekStart.plusDays(6).toString());
                try (ResultSet rows = statement.executeQuery()) {
                    if (!rows.next()) {
                        throw new AssertionError("Expected weekly availability counts.");
                    }
                    assertEquals(2, rows.getInt("availability_count"));
                    assertEquals(0, rows.getInt("schedule_count"));
                    assertEquals(0, rows.getInt("assignment_count"));
                }
            }
        }
    }
}
