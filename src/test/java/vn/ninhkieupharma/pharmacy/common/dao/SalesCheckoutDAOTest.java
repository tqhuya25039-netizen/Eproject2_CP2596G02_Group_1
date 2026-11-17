package vn.ninhkieupharma.pharmacy.common.dao;

import org.junit.jupiter.api.Test;
import vn.ninhkieupharma.pharmacy.domain.sales.SaleLineRequest;
import vn.ninhkieupharma.pharmacy.domain.sales.SaleOrderRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;

class SalesCheckoutDAOTest {
    private final SalesCheckoutDAO dao = new SalesCheckoutDAO();

    @Test
    void refusesAnEmptySaleBeforeOpeningADatabaseConnection() {
        SaleOrderRequest request = new SaleOrderRequest(
                Optional.empty(), List.of(), "", null, "", "", "", "", false, List.of());

        assertThrows(IllegalArgumentException.class, () -> dao.completeSale(1, request));
    }

    @Test
    void refusesTreatmentDurationsOutsideTheSupportedRangeBeforeOpeningADatabaseConnection() {
        SaleLineRequest line = new SaleLineRequest(
                new UUID(0x4e4b504841524d41L, 1),
                BigDecimal.ONE,
                "Once daily",
                0);
        SaleOrderRequest request = new SaleOrderRequest(
                Optional.empty(), List.of(line), "", null, "", "", "", "", false, List.of());

        assertThrows(IllegalArgumentException.class, () -> dao.completeSale(1, request));
    }
}
