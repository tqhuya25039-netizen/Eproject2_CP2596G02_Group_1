package vn.ninhkieupharma.pharmacy.domain.hr;

import java.util.UUID;

public record Availability(UUID id, String employee, String date, String startTime, String endTime, String notes) {
}
