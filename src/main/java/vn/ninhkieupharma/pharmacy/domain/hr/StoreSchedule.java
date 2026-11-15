package vn.ninhkieupharma.pharmacy.domain.hr;

import java.util.UUID;

public record StoreSchedule(UUID id, String date, String shift, String employee, String status, String notes) {
}
