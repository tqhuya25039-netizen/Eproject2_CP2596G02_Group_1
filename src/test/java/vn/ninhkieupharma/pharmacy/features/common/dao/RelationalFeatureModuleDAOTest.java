package vn.ninhkieupharma.pharmacy.features.common.dao;

import org.junit.jupiter.api.Test;
import vn.ninhkieupharma.pharmacy.features.common.service.ModuleCatalog;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RelationalFeatureModuleDAOTest {
    private final RelationalFeatureModuleDAO dao = new RelationalFeatureModuleDAO();

    @Test
    void categoryFormMatchesTheNormalizedSchemaFields() {
        var category = ModuleCatalog.MODULES.stream()
                .filter(module -> module.id().equals("UC-07"))
                .findFirst()
                .orElseThrow();

        assertEquals(3, category.columns().size());
        assertThrows(IllegalArgumentException.class,
                () -> dao.create("UC-07", List.of("category code", "category name", "description", "ACTIVE")));
    }

    @Test
    void complexWorkflowsRemainReadOnlyInsteadOfWritingPartialRecords() {
        for (String moduleId : List.of("UC-03", "UC-04", "UC-12", "UC-14", "UC-17")) {
            UnsupportedOperationException error = assertThrows(UnsupportedOperationException.class,
                    () -> dao.create(moduleId, List.of()));
            assertTrue(error.getMessage().contains(moduleId));
        }
    }

    @Test
    void employeeAccountCreationDoesNotAllowElevatedRoles() {
        assertThrows(IllegalArgumentException.class, () -> dao.create("UC-01",
                List.of("NV-TEST", "Test Employee", "", "", "test_admin", "ADMIN", "Test@12345")));
    }

    @Test
    void employeeAccountCreationValidatesPasswordBeforeConnectingToMysql() {
        assertThrows(IllegalArgumentException.class, () -> dao.create("UC-01",
                List.of("NV-TEST", "Test Employee", "", "", "test_staff", "STAFF", "x".repeat(73))));
    }

    @Test
    void bulkReplacementIsNeverAllowedForExistingBusinessData() {
        assertThrows(UnsupportedOperationException.class,
                () -> dao.replaceAll("UC-07", List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> dao.delete("UC-07", UUID.randomUUID()));
    }
}
