package vn.ninhkieupharma.pharmacy.domain.hr;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;

public record AvailabilitySlot(
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        String note) {
    public AvailabilitySlot {
        Objects.requireNonNull(date);
        Objects.requireNonNull(startTime);
        Objects.requireNonNull(endTime);
        note = Objects.requireNonNull(note);
        if (!startTime.isBefore(endTime)) {
            throw new IllegalArgumentException("Giờ bắt đầu phải sớm hơn giờ kết thúc.");
        }
    }
}
