package vn.ninhkieupharma.pharmacy.features.storeschedule.model;

import java.util.UUID;

public record StoreSchedule(UUID id, String date, String shift, String employee, String status, String notes) {
}
