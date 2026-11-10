package vn.ninhkieupharma.pharmacy.features.common.dao;

import org.junit.jupiter.api.Test;
import vn.ninhkieupharma.pharmacy.features.common.model.ModuleRow;
import vn.ninhkieupharma.pharmacy.features.customerloyalty.dao.CustomerDAO;
import vn.ninhkieupharma.pharmacy.features.customerloyalty.model.Customer;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CustomerModuleDAOAdapterTest {
    @Test
    void mapsTheExistingCustomerTableShapeToTypedCrud() {
        InMemoryCustomerDAO typedDAO = new InMemoryCustomerDAO();
        CustomerModuleDAOAdapter adapter = new CustomerModuleDAOAdapter(typedDAO);

        ModuleRow created = adapter.create("UC-16",
                List.of("KH9001", "Test Customer", "0900", "Penicillin", "1.250", "Member"));
        assertEquals(List.of("KH9001", "Test Customer", "0900", "Penicillin", "1.250", "Member"),
                adapter.findAll("UC-16").get(0).values());
        assertEquals(1250, typedDAO.findById(created.id()).orElseThrow().loyaltyPoints());

        ModuleRow updated = adapter.update("UC-16", created.id(),
                List.of("KH9001", "Updated Customer", "0901", "None", "2.000", "Active"));
        assertEquals(created.id(), updated.id());
        assertEquals("Updated Customer", typedDAO.findById(created.id()).orElseThrow().fullName());

        adapter.delete("UC-16", created.id());
        assertEquals(List.of(), adapter.findAll("UC-16"));
    }

    @Test
    void rejectsInvalidPointsAndModulesOutsideCustomerUseCase() {
        CustomerModuleDAOAdapter adapter =
                new CustomerModuleDAOAdapter(new InMemoryCustomerDAO());

        assertThrows(IllegalArgumentException.class, () -> adapter.create("UC-16",
                List.of("KH9002", "Test Customer", "0900", "None", "many", "Member")));
        assertThrows(IllegalArgumentException.class, () -> adapter.findAll("UC-17"));
    }

    private static final class InMemoryCustomerDAO implements CustomerDAO {
        private final List<Customer> customers = new ArrayList<>();

        @Override
        public List<Customer> findAll() {
            return List.copyOf(customers);
        }

        @Override
        public Optional<Customer> findById(UUID id) {
            return customers.stream().filter(customer -> customer.id().equals(id)).findFirst();
        }

        @Override
        public Customer create(Customer customer) {
            customers.add(customer);
            return customer;
        }

        @Override
        public Customer update(Customer updatedCustomer) {
            customers.removeIf(customer -> customer.id().equals(updatedCustomer.id()));
            customers.add(updatedCustomer);
            return updatedCustomer;
        }

        @Override
        public void delete(UUID id) {
            customers.removeIf(customer -> customer.id().equals(id));
        }
    }
}
