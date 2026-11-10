package vn.ninhkieupharma.pharmacy.features.common.dao;

import vn.ninhkieupharma.pharmacy.features.common.model.ModuleRow;
import vn.ninhkieupharma.pharmacy.features.customerloyalty.dao.CustomerDAO;
import vn.ninhkieupharma.pharmacy.features.customerloyalty.model.Customer;
import vn.ninhkieupharma.pharmacy.features.common.service.Localization;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class CustomerModuleDAOAdapter implements FeatureModuleDAO {
    private static final String MODULE_ID = "UC-16";
    private final CustomerDAO customerDAO;

    public CustomerModuleDAOAdapter(CustomerDAO customerDAO) {
        this.customerDAO = customerDAO;
    }

    @Override
    public List<ModuleRow> findAll(String moduleId) {
        requireCustomerModule(moduleId);
        return customerDAO.findAll().stream().map(this::toModuleRow).toList();
    }

    @Override
    public void replaceAll(String moduleId, List<List<String>> values) {
        requireCustomerModule(moduleId);
        throw new UnsupportedOperationException("Bulk replacement is not supported for customer records.");
    }

    @Override
    public ModuleRow create(String moduleId, List<String> values) {
        requireCustomerModule(moduleId);
        Customer customer = toCustomer(UUID.randomUUID(), values);
        return toModuleRow(customerDAO.create(customer));
    }

    @Override
    public ModuleRow update(String moduleId, UUID rowId, List<String> values) {
        requireCustomerModule(moduleId);
        Customer customer = toCustomer(rowId, values);
        return toModuleRow(customerDAO.update(customer));
    }

    @Override
    public void delete(String moduleId, UUID rowId) {
        requireCustomerModule(moduleId);
        customerDAO.delete(rowId);
    }

    private ModuleRow toModuleRow(Customer customer) {
        return new ModuleRow(customer.id(), List.of(
                customer.customerCode(),
                customer.fullName(),
                customer.phone(),
                customer.allergies(),
                NumberFormat.getIntegerInstance(Localization.isEnglish()
                                ? Locale.US : Locale.forLanguageTag("vi-VN"))
                        .format(customer.loyaltyPoints()),
                customer.status()));
    }

    private Customer toCustomer(UUID id, List<String> values) {
        if (values.size() != 6) {
            throw new IllegalArgumentException(Localization.text("Khách hàng cần có 6 giá trị."));
        }
        int loyaltyPoints;
        try {
            loyaltyPoints = Integer.parseInt(values.get(4).replace(".", "").replace(",", "").trim());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(Localization.text("Điểm tích lũy phải là số nguyên."),
                    exception);
        }
        if (loyaltyPoints < 0) {
            throw new IllegalArgumentException(Localization.text("Điểm tích lũy không thể là số âm."));
        }
        return new Customer(id, values.get(0), values.get(1), values.get(2),
                values.get(3), loyaltyPoints, values.get(5));
    }

    private void requireCustomerModule(String moduleId) {
        if (!MODULE_ID.equals(moduleId)) {
            throw new IllegalArgumentException("Customer DAO adapter only supports " + MODULE_ID + ".");
        }
    }
}
