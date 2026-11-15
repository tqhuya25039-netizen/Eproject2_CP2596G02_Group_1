package vn.ninhkieupharma.pharmacy.domain.hr;

import java.util.UUID;

public record EmployeeAccount(UUID id, String employeeCode, String fullName, String phone, String role, String status) {
}
