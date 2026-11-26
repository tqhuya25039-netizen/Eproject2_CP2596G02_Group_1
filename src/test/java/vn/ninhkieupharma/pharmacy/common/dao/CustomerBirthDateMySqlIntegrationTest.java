package vn.ninhkieupharma.pharmacy.common.dao;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import vn.ninhkieupharma.pharmacy.common.auth.User;
import vn.ninhkieupharma.pharmacy.common.auth.UserRole;
import vn.ninhkieupharma.pharmacy.common.model.ModuleRow;
import vn.ninhkieupharma.pharmacy.common.service.FeatureServices;
import vn.ninhkieupharma.pharmacy.common.service.ModuleCatalog;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@EnabledIfEnvironmentVariable(named = MySqlTestSchema.ENABLED_ENV, matches = "true")
class CustomerBirthDateMySqlIntegrationTest {
    @Test
    void preservesBirthDatePrecisionAcrossCustomerAndCheckoutScreens() throws Exception {
        try (MySqlTestSchema schema = MySqlTestSchema.withCurrentSchema()) {
            schema.runInChild(BirthDateScenario.class);
        }
    }

    public static class BirthDateScenario {
        public static void main(String[] args) {
            var staff = new RelationalFeatureModuleDAO(new User(1, "staff_demo", "", UserRole.STAFF, true));
            var admin = new RelationalFeatureModuleDAO(new User(2, "admin_demo", "", UserRole.ADMIN, true));
            var existing = staff.findCustomersAvailableForSale().stream()
                    .filter(row -> row.values().get(1).equals("0910000001"))
                    .findFirst().orElseThrow();
            assertDates(admin, staff, existing.id(), "1996-03-18");

            var created = staff.createCustomerFromSale(List.of("Customer", "0935555555", "", "", "1990"));
            assertEquals("1990", created.values().get(5));
            assertDates(admin, staff, created.id(), "1990");

            var updated = staff.updateCustomerFromSale(created.id(),
                    List.of("Customer", "0935555555", "", "", "1990-03"));
            assertEquals("1990-03", updated.values().get(5));
            assertDates(admin, staff, created.id(), "1990-03");

            String customerCode = rowFor(admin.findAll("UC-16"), created.id()).values().get(0);
            var profile = staff.updateCustomerProfile(created.id(),
                    List.of(customerCode, "Customer", "0935555555", "", "", "0", "ACTIVE", "", "1990-03-25"));
            assertEquals("1990-03-25", profile.values().get(8));
            assertDates(admin, staff, created.id(), "1990-03-25");

            var customerModule = ModuleCatalog.MODULES.stream()
                    .filter(module -> module.id().equals("UC-16")).findFirst().orElseThrow();
            var customers = new FeatureServices(admin).forModule(customerModule);
            customers.update(customerModule, created.id(),
                    List.of(customerCode, "Customer", "0935555555", "", "", "0", "ACTIVE", "", ""));
            assertDates(admin, staff, created.id(), "");

            var customer = customers.create(customerModule,
                    List.of("", "Profile customer", "0935555556", "", "", "0", "ACTIVE", "", "1991"));
            assertDates(admin, staff, customer.id(), "1991");
            customers.update(customerModule, customer.id(),
                    List.of(customer.values().getFirst(), "Profile customer", "0935555556", "", "",
                            "0", "BLOCKED", "Integration restriction", "1991"));
            assertEquals("BLOCKED", rowFor(customers.findAll(customerModule), customer.id()).values().get(6));
        }

        private static void assertDates(RelationalFeatureModuleDAO admin, RelationalFeatureModuleDAO staff,
                                        UUID customerId, String expected) {
            assertEquals(expected, rowFor(admin.findAll("UC-16"), customerId).values().get(8));
            assertEquals(expected, rowFor(staff.findCustomersAvailableForSale(), customerId).values().get(5));
        }

        private static ModuleRow rowFor(List<ModuleRow> rows, UUID customerId) {
            return rows.stream().filter(row -> row.id().equals(customerId)).findFirst().orElseThrow();
        }
    }
}
