package vn.ninhkieupharma.pharmacy.common.time;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VietnamDateTest {
    @Test
    void schedulingRangeIncludesTodayAndOneYearAheadOnly() {
        var today = VietnamDate.today();

        assertDoesNotThrow(() -> VietnamDate.requireSchedulingDate(today, "Ngày"));
        assertDoesNotThrow(() -> VietnamDate.requireSchedulingDate(today.plusYears(1), "Ngày"));
        assertThrows(IllegalArgumentException.class,
                () -> VietnamDate.requireSchedulingDate(today.minusDays(1), "Ngày"));
        assertThrows(IllegalArgumentException.class,
                () -> VietnamDate.requireSchedulingDate(today.plusYears(1).plusDays(1), "Ngày"));
    }
}
