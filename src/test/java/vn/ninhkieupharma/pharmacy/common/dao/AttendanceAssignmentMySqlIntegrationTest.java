package vn.ninhkieupharma.pharmacy.common.dao;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import vn.ninhkieupharma.pharmacy.common.auth.User;
import vn.ninhkieupharma.pharmacy.common.auth.UserRole;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledIfEnvironmentVariable(named = MySqlTestSchema.ENABLED_ENV, matches = "true")
class AttendanceAssignmentMySqlIntegrationTest {
    @Test
    void storesAttendanceInTheAssignmentAndClearsItWithoutDeletingTheShift() throws Exception {
        try (MySqlTestSchema schema = MySqlTestSchema.withCurrentSchema()) {
            schema.runInChild(AttendanceScenario.class);
        }
    }

    public static final class AttendanceScenario {
        public static void main(String[] args) {
            String database = System.getenv("PHARMACY_DB_NAME");
            if (database == null || !database.matches("pharmacy_test_[a-f0-9]{32}")) {
                throw new IllegalStateException("Attendance tests require a disposable database.");
            }
            var dao = new RelationalFeatureModuleDAO(new User(2, "admin_demo", "", UserRole.ADMIN, true));
            var shift = dao.findAll("UC-03").stream()
                    .filter(row -> row.id().getLeastSignificantBits() == 1023)
                    .findFirst().orElseThrow();
            String employee = shift.values().get(2);
            String shiftName = shift.values().get(1);
            BigDecimal hoursBeforeAttendance = workHours(dao, employee);

            assertTrue(dao.findAll("UC-04").stream().anyMatch(row -> row.id().getLeastSignificantBits() == 1013
                    && row.values().get(2).isEmpty() && !row.values().get(4).isEmpty()));
            var checkIn = List.of(employee, shiftName, "2026-11-28 08:00", "", "Check-in note");
            assertEquals(shift.id(), dao.create("UC-04", checkIn).id());
            assertThrows(IllegalArgumentException.class, () -> dao.create("UC-04", checkIn));
            assertThrows(IllegalArgumentException.class, () -> dao.delete("UC-03", shift.id()));

            var completedAttendance = List.of(employee, shiftName, "2026-11-28 08:00", "2026-11-28 11:30",
                    "Check-out note");
            dao.update("UC-04", shift.id(), completedAttendance);
            assertEquals(completedAttendance, dao.findAll("UC-04").stream()
                    .filter(row -> row.id().equals(shift.id())).findFirst().orElseThrow().values());
            assertEquals(0, hoursBeforeAttendance.add(new BigDecimal("3.50")).compareTo(workHours(dao, employee)));
            assertEquals("08:00 - 11:30", dao.findAll("UC-06").stream()
                    .filter(row -> row.id().equals(shift.id())).findFirst().orElseThrow().values().get(3));

            dao.delete("UC-04", shift.id());
            assertFalse(dao.findAll("UC-04").stream().anyMatch(row -> row.id().equals(shift.id())));
            var remainingShift = dao.findAll("UC-03").stream()
                    .filter(row -> row.id().equals(shift.id())).findFirst().orElseThrow();
            assertEquals("COMPLETED", remainingShift.values().get(3));
            assertEquals(shift.values().get(4), remainingShift.values().get(4));
            assertEquals(0, hoursBeforeAttendance.compareTo(workHours(dao, employee)));
        }

        private static BigDecimal workHours(RelationalFeatureModuleDAO dao, String employee) {
            return new BigDecimal(dao.findAll("UC-05").stream().filter(row -> row.values().get(0).equals(employee))
                    .findFirst().orElseThrow().values().get(3));
        }
    }
}
