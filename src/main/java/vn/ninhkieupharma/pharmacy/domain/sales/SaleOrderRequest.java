package vn.ninhkieupharma.pharmacy.domain.sales;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public record SaleOrderRequest(
        Optional<UUID> customerId,
        List<SaleLineRequest> lines,
        String prescriptionCode,
        LocalDate prescriptionDate,
        String prescribingDoctor,
        String clinic,
        String diagnosis,
        String note,
        boolean redeemPoints,
        List<SaleAllergyWarning> acknowledgedAllergyWarnings) {
    public SaleOrderRequest {
        customerId = customerId == null ? Optional.empty() : customerId;
        lines = List.copyOf(lines);
        acknowledgedAllergyWarnings = acknowledgedAllergyWarnings == null
                ? List.of()
                : List.copyOf(acknowledgedAllergyWarnings);
    }
}
