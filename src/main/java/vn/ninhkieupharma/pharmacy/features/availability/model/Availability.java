package vn.ninhkieupharma.pharmacy.features.availability.model;

import java.util.UUID;

public record Availability(UUID id, String employee, String date, String startTime, String endTime, String notes) {
}
