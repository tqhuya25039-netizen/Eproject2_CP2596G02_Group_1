package vn.ninhkieupharma.pharmacy.domain.sales;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

public record SalesReport(Period period, LocalDate start, List<Row> rows) {
    public SalesReport {
        rows = List.copyOf(rows);
    }

    public LocalDate endExclusive() {
        return period.endExclusive(start);
    }

    public Totals totals() {
        return rows.stream().map(Row::totals).reduce(Totals.ZERO, Totals::plus);
    }

    public enum Period {
        MONTH,
        QUARTER,
        YEAR;

        LocalDate start(int year, int selection) {
            if (year < 1000 || year > 9998) {
                throw new IllegalArgumentException("Hãy nhập năm từ 1000 đến 9998.");
            }
            return switch (this) {
                case MONTH -> LocalDate.of(year, selection, 1);
                case QUARTER -> LocalDate.of(year, (selection - 1) * 3 + 1, 1);
                case YEAR -> LocalDate.of(year, 1, 1);
            };
        }

        LocalDate endExclusive(LocalDate start) {
            return switch (this) {
                case MONTH -> start.plusMonths(1);
                case QUARTER -> start.plusMonths(3);
                case YEAR -> start.plusYears(1);
            };
        }

        LocalDate groupStart(LocalDate date) {
            return this == MONTH ? date : date.withDayOfMonth(1);
        }
    }

    public record Row(LocalDate start, Totals totals) {
    }

    public record Totals(long orderCount, BigDecimal revenue, BigDecimal discount) {
        public static final Totals ZERO = new Totals(0, BigDecimal.ZERO, BigDecimal.ZERO);

        public Totals plus(Totals other) {
            return new Totals(orderCount + other.orderCount,
                    revenue.add(other.revenue), discount.add(other.discount));
        }

        public BigDecimal averageOrderValue() {
            return orderCount == 0
                    ? BigDecimal.ZERO
                    : revenue.divide(BigDecimal.valueOf(orderCount), 2, RoundingMode.HALF_UP);
        }
    }
}
