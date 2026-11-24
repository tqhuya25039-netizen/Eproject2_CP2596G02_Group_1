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
        long requestedPoints,
        List<SaleAllergyWarning> acknowledgedAllergyWarnings) {
    public static final long REDEEM_MAXIMUM = Long.MAX_VALUE;
    public static final long MAX_POINT_BALANCE = 4_294_967_295L;

    public SaleOrderRequest {
        customerId = customerId == null ? Optional.empty() : customerId;
        lines = List.copyOf(lines);
        acknowledgedAllergyWarnings = acknowledgedAllergyWarnings == null
                ? List.of()
                : List.copyOf(acknowledgedAllergyWarnings);
    }

    public SaleOrderRequest(
            Optional<UUID> customerId, List<SaleLineRequest> lines, String prescriptionCode,
            LocalDate prescriptionDate, String prescribingDoctor, String clinic, String diagnosis,
            String note, boolean redeemPoints, List<SaleAllergyWarning> acknowledgedAllergyWarnings) {
        this(customerId, lines, prescriptionCode, prescriptionDate, prescribingDoctor, clinic, diagnosis,
                note, redeemPoints ? REDEEM_MAXIMUM : 0, acknowledgedAllergyWarnings);
    }

    public boolean redeemPoints() {
        return requestedPoints > 0;
    }
}
