package vn.ninhkieupharma.pharmacy.domain.sales;

import vn.ninhkieupharma.pharmacy.common.service.Localization;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.List;

public record SalesReport(Period period, Grouping grouping, LocalDate start, LocalDate endExclusive,
                          Rows data) {
    public SalesReport(Period period, LocalDate start, List<Row> rows) {
        this(period, period.grouping(), start, period.endExclusive(start),
                new Rows(rows, List.of(), List.of(), List.of()));
    }

    public List<Row> rows() {
        return data.timeSeries();
    }

    public List<Breakdown> employees() {
        return data.employees();
    }

    public List<Breakdown> products() {
        return data.products();
    }

    public List<Breakdown> categories() {
        return data.categories();
    }

    public Totals totals() {
        return rows().stream().map(Row::totals).reduce(Totals.ZERO, Totals::plus);
    }

    public record Filter(LocalDate fromInclusive, LocalDate toInclusive,
                         Long employeeId, Long productId, Long categoryId) {
        public Filter {
            if (fromInclusive == null || toInclusive == null) {
                throw new IllegalArgumentException(Localization.text("Hãy chọn ngày bắt đầu và ngày kết thúc."));
            }
            if (fromInclusive.isAfter(toInclusive)) {
                throw new IllegalArgumentException(Localization.text("Ngày kết thúc phải từ ngày bắt đầu trở đi."));
            }
            if (fromInclusive.getYear() < 1000 || toInclusive.getYear() > 9998) {
                throw new IllegalArgumentException(Localization.text("Hãy nhập năm từ 1000 đến 9998."));
            }
            if (invalidId(employeeId) || invalidId(productId) || invalidId(categoryId)) {
                throw new IllegalArgumentException(Localization.text("Bộ lọc báo cáo không hợp lệ."));
            }
        }

        public LocalDate endExclusive() {
            return toInclusive.plusDays(1);
        }

        Grouping defaultGrouping() {
            if (ChronoUnit.DAYS.between(fromInclusive, endExclusive()) <= 31) {
                return Grouping.DAILY;
            }
            long months = ChronoUnit.MONTHS.between(YearMonth.from(fromInclusive), YearMonth.from(toInclusive));
            return months < 36 ? Grouping.MONTHLY : Grouping.YEARLY;
        }

        private static boolean invalidId(Long id) {
            return id != null && id < 1;
        }
    }

    public enum Grouping {
        DAILY,
        MONTHLY,
        YEARLY;

        public LocalDate bucketStart(LocalDate date) {
            return switch (this) {
                case DAILY -> date;
                case MONTHLY -> date.withDayOfMonth(1);
                case YEARLY -> date.withDayOfYear(1);
            };
        }

        public LocalDate nextBucket(LocalDate date) {
            return switch (this) {
                case DAILY -> date.plusDays(1);
                case MONTHLY -> date.plusMonths(1);
                case YEARLY -> date.plusYears(1);
            };
        }
    }

    public record Option(long id, String label) {
    }

    public record FilterOptions(List<Option> employees, List<Option> products, List<Option> categories) {
        public FilterOptions {
            employees = List.copyOf(employees);
            products = List.copyOf(products);
            categories = List.copyOf(categories);
        }
    }

    public record Breakdown(Option option, Totals totals) {
    }

    public record Rows(List<Row> timeSeries, List<Breakdown> employees, List<Breakdown> products,
                       List<Breakdown> categories) {
        public Rows {
            timeSeries = List.copyOf(timeSeries);
            employees = List.copyOf(employees);
            products = List.copyOf(products);
            categories = List.copyOf(categories);
        }
    }

    public enum Period {
        MONTH,
        QUARTER,
        YEAR;

        public LocalDate start(int year, int selection) {
            if (year < 1000 || year > 9998) {
                throw new IllegalArgumentException(Localization.text("Hãy nhập năm từ 1000 đến 9998."));
            }
            return switch (this) {
                case MONTH -> LocalDate.of(year, selection, 1);
                case QUARTER -> LocalDate.of(year, (selection - 1) * 3 + 1, 1);
                case YEAR -> LocalDate.of(year, 1, 1);
            };
        }

        public LocalDate endExclusive(LocalDate start) {
            return switch (this) {
                case MONTH -> start.plusMonths(1);
                case QUARTER -> start.plusMonths(3);
                case YEAR -> start.plusYears(1);
            };
        }

        Grouping grouping() {
            return this == MONTH ? Grouping.DAILY : Grouping.MONTHLY;
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
