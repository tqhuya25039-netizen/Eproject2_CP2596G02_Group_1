package vn.ninhkieupharma.pharmacy.domain.sales;

import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;

public record CustomerBirthDate(LocalDate fullDate, Integer partialYear, Integer partialMonth) {
    private static final ZoneId STORE_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final String INVALID_FORMAT = "Ngày sinh phải theo định dạng YYYY, YYYY-MM hoặc YYYY-MM-DD.";

    public static CustomerBirthDate parse(String value) {
        return parse(value, LocalDate.now(STORE_ZONE));
    }

    static CustomerBirthDate parse(String value, LocalDate today) {
        String text = value.strip();
        if (text.isEmpty()) {
            return new CustomerBirthDate(null, null, null);
        }

        try {
            return switch (text.length()) {
                case 4 -> {
                    Year year = Year.parse(text);
                    validateDate(year.atDay(1), today);
                    yield new CustomerBirthDate(null, year.getValue(), null);
                }
                case 7 -> {
                    YearMonth month = YearMonth.parse(text);
                    validateDate(month.atDay(1), today);
                    yield new CustomerBirthDate(null, month.getYear(), month.getMonthValue());
                }
                case 10 -> {
                    LocalDate date = LocalDate.parse(text);
                    validateDate(date, today);
                    yield new CustomerBirthDate(date, null, null);
                }
                default -> throw new IllegalArgumentException(INVALID_FORMAT);
            };
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException(INVALID_FORMAT, exception);
        }
    }

    private static void validateDate(LocalDate earliestDate, LocalDate today) {
        if (earliestDate.getYear() < 1000 || earliestDate.isAfter(today)) {
            throw new IllegalArgumentException("Ngày sinh phải hợp lệ và không ở tương lai.");
        }
    }

    public String format() {
        if (fullDate != null) {
            return fullDate.toString();
        }
        if (partialYear == null) {
            return "";
        }
        return partialMonth == null
                ? partialYear.toString()
                : YearMonth.of(partialYear, partialMonth).toString();
    }
}
