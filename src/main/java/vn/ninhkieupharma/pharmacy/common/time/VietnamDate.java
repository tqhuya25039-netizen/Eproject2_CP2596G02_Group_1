package vn.ninhkieupharma.pharmacy.common.time;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class VietnamDate {
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter DISPLAY_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private VietnamDate() {
    }

    public static LocalDate today() {
        return LocalDate.now(ZONE);
    }

    public static LocalDate latestSchedulingDate() {
        return today().plusYears(1);
    }

    public static boolean isSchedulingDateAllowed(LocalDate date) {
        LocalDate today = today();
        return date != null && !date.isBefore(today) && !date.isAfter(today.plusYears(1));
    }

    public static void requireSchedulingDate(LocalDate date, String field) {
        LocalDate today = today();
        LocalDate latestDate = today.plusYears(1);
        if (date == null || date.isBefore(today) || date.isAfter(latestDate)) {
            throw new IllegalArgumentException(
                    field + " phải từ " + today.format(DISPLAY_FORMAT)
                            + " đến " + latestDate.format(DISPLAY_FORMAT) + ".");
        }
    }
}
