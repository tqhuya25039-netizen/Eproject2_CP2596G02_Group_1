package vn.ninhkieupharma.pharmacy.common.dao;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import vn.ninhkieupharma.pharmacy.common.auth.User;
import vn.ninhkieupharma.pharmacy.common.auth.UserRole;
import vn.ninhkieupharma.pharmacy.common.model.ModuleRow;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledIfEnvironmentVariable(named = MySqlTestSchema.ENABLED_ENV, matches = "true")
class StoreScheduleAdministrationMySqlIntegrationTest {
    @Test
    void administratorCanCreateChangeStatusAndDeleteAssignments() throws Exception {
        try (MySqlTestSchema schema = MySqlTestSchema.withCurrentSchema()) {
            schema.runInChild(StoreScheduleAdministrationScenario.class);
        }
    }

    public static final class StoreScheduleAdministrationScenario {
        public static void main(String[] args) {
            RelationalFeatureModuleDAO administratorDAO = new RelationalFeatureModuleDAO(
                    new User(1, "admin_demo", "", UserRole.ADMIN, true));

            ModuleRow created = administratorDAO.create("UC-03",
                    List.of("2026-11-03", "Ca sang", "Nguyen Thi An", "PLANNED", "Admin-created assignment"));
            assertEquals("PLANNED", created.values().get(3));

            List<String> updatedValues = new ArrayList<>(created.values());
            updatedValues.set(3, "COMPLETED");
            ModuleRow updated = administratorDAO.update("UC-03", created.id(), updatedValues);
            assertEquals("COMPLETED", updated.values().get(3));
            assertTrue(administratorDAO.findAll("UC-03").stream()
                    .anyMatch(row -> row.id().equals(created.id())
                            && row.values().get(3).equals("COMPLETED")));

            administratorDAO.delete("UC-03", created.id());
            assertFalse(administratorDAO.findAll("UC-03").stream()
                    .anyMatch(row -> row.id().equals(created.id())));
        }
    }
}
