package vn.ninhkieupharma.pharmacy.features.customerloyalty.dao;

import vn.ninhkieupharma.pharmacy.features.customerloyalty.model.Customer;

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
