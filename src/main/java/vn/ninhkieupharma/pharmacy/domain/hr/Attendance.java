package vn.ninhkieupharma.pharmacy.domain.hr;

import java.util.UUID;

public record Attendance(UUID id, String employee, String shift, String clockIn, String clockOut, String status) {
}
