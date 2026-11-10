package vn.ninhkieupharma.pharmacy.features.employeeaccounts.model;

import java.util.UUID;

public record EmployeeAccount(UUID id, String employeeCode, String fullName, String phone, String role, String status) {
}
