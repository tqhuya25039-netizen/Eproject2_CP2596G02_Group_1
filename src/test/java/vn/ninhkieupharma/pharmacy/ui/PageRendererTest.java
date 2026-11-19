package vn.ninhkieupharma.pharmacy.ui;

import org.junit.jupiter.api.Test;
import vn.ninhkieupharma.pharmacy.common.auth.UserRole;
import vn.ninhkieupharma.pharmacy.common.model.ModuleRow;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PageRendererTest {
    @Test
    void customerSuggestionsNarrowAsPhoneDigitsAreTyped() {
        ModuleRow customer = new ModuleRow(
                UUID.randomUUID(),
                List.of("Nguyễn Thị Mai", "0912345678", "", "", "125"));

        assertTrue(PageRenderer.matchesSalesCustomerQuery(customer, "0"));
        assertTrue(PageRenderer.matchesSalesCustomerQuery(customer, "09"));
        assertFalse(PageRenderer.matchesSalesCustomerQuery(customer, "08"));
    }

    @Test
    void customerSuggestionsMatchNamesWithoutCaseSensitivity() {
        ModuleRow customer = new ModuleRow(
                UUID.randomUUID(),
                List.of("Nguyễn Thị Mai", "0912345678", "", "", "125"));

        assertTrue(PageRenderer.matchesSalesCustomerQuery(customer, "nguyễn"));
    }

    @Test
    void catalogMutationsAreAdminOnly() {
        for (String moduleId : List.of("UC-07", "UC-08", "UC-09")) {
            assertTrue(PageRenderer.canWriteModule(moduleId, UserRole.ADMIN));
            assertFalse(PageRenderer.canWriteModule(moduleId, UserRole.STAFF));
        }
    }
}
