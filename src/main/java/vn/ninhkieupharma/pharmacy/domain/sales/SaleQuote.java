package vn.ninhkieupharma.pharmacy.domain.sales;

import java.util.List;

public record SaleQuote(SaleReceipt amounts, List<SaleAllergyWarning> warnings) {
    public SaleQuote {
        warnings = List.copyOf(warnings);
    }
}
