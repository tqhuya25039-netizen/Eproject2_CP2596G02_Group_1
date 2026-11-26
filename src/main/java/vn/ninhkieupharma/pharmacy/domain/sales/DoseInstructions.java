package vn.ninhkieupharma.pharmacy.domain.sales;

import java.math.BigDecimal;
import java.util.Set;

public record DoseInstructions(BigDecimal amount, DoseUnit unit, DoseTiming timing, Set<DoseSession> sessions) {
    public static final DoseInstructions EMPTY = new DoseInstructions(null, null, null, Set.of());
    public static final String AMOUNT_ERROR =
            "Liều mỗi lần cần có số dương và đơn vị, tối đa 6 chữ số nguyên và 3 chữ số thập phân.";
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("999999.999");

    public DoseInstructions {
        amount = amount == null ? null : amount.stripTrailingZeros();
        sessions = sessions == null ? Set.of() : Set.copyOf(sessions);
    }

    public boolean hasValidAmount() {
        if (amount == null) {
            return unit == null;
        }
        return unit != null && amount.signum() > 0 && amount.scale() <= 3 && amount.compareTo(MAX_AMOUNT) <= 0;
    }
}
