package vn.ninhkieupharma.pharmacy.features.attendance.model;

import java.util.UUID;

public record Attendance(UUID id, String employee, String shift, String clockIn, String clockOut, String status) {
}
