package vn.ninhkieupharma.pharmacy.common.dao;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import vn.ninhkieupharma.pharmacy.common.auth.User;
import vn.ninhkieupharma.pharmacy.common.auth.UserRole;
import vn.ninhkieupharma.pharmacy.common.model.ModuleRow;

import java.sql.Connection;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@EnabledIfEnvironmentVariable(named = MySqlTestSchema.ENABLED_ENV, matches = "true")
class AttendanceOwnershipMySqlIntegrationTest {
    @Test
    void staffCanOnlyCreateAndEditTheirOwnAttendance() throws Exception {
        try (MySqlTestSchema schema = MySqlTestSchema.withCurrentSchema()) {
            schema.runInChild(AttendanceOwnershipScenario.class);
        }
    }

    public static final class AttendanceOwnershipScenario {
        public static void main(String[] args) throws Exception {
            addOtherEmployeeAndAssignments();

            RelationalFeatureModuleDAO staffDAO = new RelationalFeatureModuleDAO(
                    new User(2, "staff_demo", "", UserRole.STAFF, true));
            RelationalFeatureModuleDAO administratorDAO = new RelationalFeatureModuleDAO(
                    new User(1, "admin_demo", "", UserRole.ADMIN, true));

            assertEquals("Nguyen Thi An", staffDAO.findCurrentEmployeeName());
            ModuleRow ownAttendance = staffDAO.create("UC-04",
                    List.of("Nguyen Thi An", "Ca chieu", "2026-11-03 13:00", "", "Self check-in"));
            ModuleRow otherAttendance = administratorDAO.findAll("UC-04").stream()
                    .filter(row -> row.values().get(0).equals("Nguyen Thi Binh"))
                    .findFirst()
                    .orElseThrow();

            assertEquals(2, staffDAO.findAll("UC-04").size());
            assertEquals(3, administratorDAO.findAll("UC-04").size());
            assertThrows(SecurityException.class, () -> staffDAO.create("UC-04",
                    List.of("Nguyen Thi Binh", "Ca sang", "2026-11-03 08:00", "", "")));
            assertThrows(SecurityException.class, () -> staffDAO.update(
                    "UC-04", otherAttendance.id(),
                    List.of("Nguyen Thi Binh", "Ca sang", "2026-11-03 08:01", "", "")));
            assertThrows(SecurityException.class, () -> staffDAO.delete("UC-04", otherAttendance.id()));

            staffDAO.update("UC-04", ownAttendance.id(),
                    List.of("Nguyen Thi An", "Ca chieu", "2026-11-03 13:01", "", "Self update"));
        }

        private static void addOtherEmployeeAndAssignments() throws Exception {
            try (Connection connection = vn.ninhkieupharma.pharmacy.config.DatabaseConnection.getConnection();
                 Statement statement = connection.createStatement()) {
                statement.executeUpdate("""
                        INSERT INTO users (user_id, role_id, username, password_hash, is_active)
                        VALUES (3, 2, 'staff_other', 'integration-test', 1)
                        """);
                statement.executeUpdate("""
                        INSERT INTO employees
                            (employee_id, user_id, employee_code, full_name, phone, email, hire_date, employment_status)
                        VALUES
                            (2, 3, 'NV0002', 'Nguyen Thi Binh', '0900000002',
                             'binh.demo@example.com', '2025-01-15', 'ACTIVE')
                        """);
                statement.executeUpdate("""
                        INSERT INTO schedule_assignments
                            (assignment_id, schedule_id, employee_id, shift_id, work_date, status)
                        VALUES
                            (2, 1, 2, 1, '2026-11-03', 'PLANNED'),
                            (3, 1, 1, 2, '2026-11-03', 'PLANNED')
                        """);
                statement.executeUpdate("""
                        INSERT INTO attendance_records (assignment_id, check_in_at, check_out_at)
                        VALUES (2, '2026-11-03 08:00:00', '2026-11-03 12:00:00')
                        """);
            }
        }
    }
}
