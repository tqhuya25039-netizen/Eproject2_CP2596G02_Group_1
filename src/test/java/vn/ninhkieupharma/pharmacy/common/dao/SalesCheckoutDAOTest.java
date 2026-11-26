package vn.ninhkieupharma.pharmacy.common.dao;

import org.junit.jupiter.api.Test;
import vn.ninhkieupharma.pharmacy.common.service.Localization;
import vn.ninhkieupharma.pharmacy.domain.sales.DoseInstructions;
import vn.ninhkieupharma.pharmacy.domain.sales.DoseUnit;
import vn.ninhkieupharma.pharmacy.domain.sales.SaleLineKind;
import vn.ninhkieupharma.pharmacy.domain.sales.SaleLineRequest;
import vn.ninhkieupharma.pharmacy.domain.sales.SaleOrderRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void requiresACustomerProfileBeforeSavingOrCompletingASale() {
        SaleLineRequest line = new SaleLineRequest(
                new UUID(0x4e4b504841524d41L, 1),
                BigDecimal.ONE,
                "",
                1);
        SaleOrderRequest request = new SaleOrderRequest(
                Optional.empty(), List.of(line), "", null, "", "", "", "", false, List.of());

        IllegalArgumentException completeError = assertThrows(
                IllegalArgumentException.class, () -> dao.completeSale(1, request));
        IllegalArgumentException pendingError = assertThrows(
                IllegalArgumentException.class, () -> dao.createPendingSale(1, request));

        assertTrue(completeError.getMessage().contains("hồ sơ khách hàng"));
        assertTrue(pendingError.getMessage().contains("hồ sơ khách hàng"));
    }

    @Test
    void rejectsIncompleteOrOutOfRangeDoseAmountsBeforeConnecting() {
        var checkout = new SalesCheckoutDAO(() -> {
            throw new AssertionError("The dose must be validated before opening the database.");
        });
        UUID productId = new UUID(0x4e4b504841524d41L, 1);
        List<DoseInstructions> invalidDoses = List.of(
                new DoseInstructions(BigDecimal.ONE, null, null, Set.of()),
                new DoseInstructions(null, DoseUnit.TABLET, null, Set.of()),
                new DoseInstructions(BigDecimal.ZERO, DoseUnit.TABLET, null, Set.of()),
                new DoseInstructions(new BigDecimal("0.0123"), DoseUnit.TABLET, null, Set.of()),
                new DoseInstructions(new BigDecimal("1000000"), DoseUnit.TABLET, null, Set.of()));

        for (DoseInstructions dose : invalidDoses) {
            SaleLineRequest line = new SaleLineRequest(
                    productId, BigDecimal.ONE, "", 1, SaleLineKind.ADD_ON, dose);
            var failure = assertThrows(IllegalArgumentException.class, () -> checkout.previewSaleStock(List.of(line)));
            assertEquals(Localization.text(DoseInstructions.AMOUNT_ERROR), failure.getMessage());
        }
    }
}
