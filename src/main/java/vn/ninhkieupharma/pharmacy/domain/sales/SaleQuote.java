package vn.ninhkieupharma.pharmacy.domain.sales;

import java.util.List;

public record SaleQuote(SaleReceipt amounts, List<SaleAllergyWarning> warnings, List<SaleStockPreview.Pick> picks) {
    public SaleQuote {
        warnings = List.copyOf(warnings);
        picks = List.copyOf(picks);
    }

    public SaleQuote(SaleReceipt amounts, List<SaleAllergyWarning> warnings) {
        this(amounts, warnings, List.of());
    }
}
