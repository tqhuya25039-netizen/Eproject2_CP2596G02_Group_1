package vn.ninhkieupharma.pharmacy.domain.sales;

import vn.ninhkieupharma.pharmacy.domain.sales.Customer;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustomerDAO {
    List<Customer> findAll();

    Optional<Customer> findById(UUID id);

    Customer create(Customer customer);

    Customer update(Customer customer);

    void delete(UUID id);
}
