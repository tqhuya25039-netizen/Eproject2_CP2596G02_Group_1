package vn.ninhkieupharma.pharmacy.domain.sales;

import org.junit.jupiter.api.Test;
import vn.ninhkieupharma.pharmacy.common.repository.InMemoryModuleRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SalesReportTest {
    @Test
    void reportsRespectPeriodBoundariesAndExcludeUnpaidOrders() {
        var repository = new InMemoryModuleRepository();
        repository.replaceAll("UC-17", List.of(
                sale("31/12/2023 23:59", "30", "COMPLETED"),
                sale("01/01/2024 00:00", "100", "COMPLETED"),
                sale("29/02/2024 13:45", "200", "Hoàn tất"),
                sale("29/02/2024 15:00", "900", "PENDING"),
                sale("02/03/2024 17:00", "900", "VOIDED"),
                sale("31/03/2024 23:59", "300", "COMPLETED"),
                sale("01/04/2024 00:00", "400", "COMPLETED"),
                sale("31/12/2024 23:59", "500", "COMPLETED"),
                sale("01/01/2025 00:00", "600", "COMPLETED")));
        var service = new SalesService(repository);

        SalesReport february = service.salesReport(SalesReport.Period.MONTH, 2024, 2);
        assertEquals(List.of(LocalDate.of(2024, 2, 29)),
                february.rows().stream().map(SalesReport.Row::start).toList());
        assertEquals(1, february.totals().orderCount());
        assertEquals(new BigDecimal("200"), february.totals().revenue());

        SalesReport firstQuarter = service.salesReport(SalesReport.Period.QUARTER, 2024, 1);
        assertEquals(List.of(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 2, 1), LocalDate.of(2024, 3, 1)),
                firstQuarter.rows().stream().map(SalesReport.Row::start).toList());
        assertEquals(LocalDate.of(2024, 4, 1), firstQuarter.endExclusive());
        assertEquals(3, firstQuarter.totals().orderCount());
        assertEquals(new BigDecimal("600"), firstQuarter.totals().revenue());
        assertEquals(new BigDecimal("200.00"), firstQuarter.totals().averageOrderValue());

        SalesReport year = service.salesReport(SalesReport.Period.YEAR, 2024, 1);
        assertEquals(5, year.totals().orderCount());
        assertEquals(new BigDecimal("1500"), year.totals().revenue());
        assertEquals(7, service.completedSalesTotals().orderCount());
        assertEquals(new BigDecimal("2130"), service.completedSalesTotals().revenue());
    }

    private List<String> sale(String date, String total, String status) {
        return List.of("TEST", date, "Khách", "Nhân viên", total, status);
    }
}
