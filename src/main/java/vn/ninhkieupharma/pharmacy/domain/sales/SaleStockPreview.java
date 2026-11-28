package vn.ninhkieupharma.pharmacy.domain.sales;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record SaleStockPreview(List<Pick> picks, List<SaleStockShortage> shortages) {
    public SaleStockPreview {
        picks = List.copyOf(picks);
        shortages = List.copyOf(shortages);
    }

    public record Pick(long batchId, String productName, String unit, String batchCode,
                       String locationCode, String locationName, LocalDate expiryDate, BigDecimal quantity) {
    }
}
