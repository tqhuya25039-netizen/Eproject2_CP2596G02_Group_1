package vn.ninhkieupharma.pharmacy.domain.sales;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SaleProductSearchTest {
    @Test
    void ranksExactBarcodeThenCodeThenProductName() {
        SaleProduct sameName = product("SP10", "8938500010011", "", "Menthol");
        SaleProduct sameCode = product("8938500010011", "Saline", "", "Sodium chloride");
        SaleProduct scanned = product("SP20", "Nước muối", "8938500010011", "Sodium chloride");

        var search = new SaleProductSearch(List.of(sameName, sameCode, scanned));

        assertEquals(List.of(scanned, sameCode, sameName), search.find("8938500010011"));
        assertEquals(scanned, search.find("8938 5000-10011").getFirst());
        assertEquals(List.of(scanned, sameCode), search.find("8938500"));
    }

    @Test
    void matchesVietnameseDiacriticsAndWordsInEitherOrder() {
        SaleProduct painRelief = product("SP10", "Thuốc đau đầu 500mg", null, "Paracetamol");
        SaleProduct stronger = product("SP11", "Thuốc đau đầu 650 mg", "", "Paracetamol");
        var search = new SaleProductSearch(List.of(stronger, painRelief));

        assertEquals(List.of(painRelief), search.find("500 Dau đầu"));
        assertEquals(List.of(painRelief), search.find("paracet 500 mg"));
        assertEquals(List.of(painRelief), search.find("SP-10"));
        assertTrue(search.find("dau dau 50 mg").isEmpty());
        assertTrue(search.find("paracetamol ibuprofen").isEmpty());
        assertTrue(search.find("---").isEmpty());
    }

    @Test
    void ranksMatchesInTheNameBeforeIngredientOnlyMatches() {
        SaleProduct brand = product("SP01", "Brand 500 mg", "", "Paracetamol");
        SaleProduct named = product("SP02", "Paracetamol 500 mg", "", "Paracetamol");

        assertEquals(List.of(named, brand), SaleProductSearch.find(List.of(brand, named), "paracetamol 500"));
    }

    @Test
    void prefersTheLiteralCodeWhenIdentifiersOnlyDifferByPunctuation() {
        SaleProduct withHyphen = product("SP-01", "A product", "", "Saline");
        SaleProduct withoutHyphen = product("SP01", "Z product", "", "Saline");

        assertEquals(List.of(withoutHyphen, withHyphen),
                SaleProductSearch.find(List.of(withHyphen, withoutHyphen), "sp01"));
    }

    @Test
    void blankSearchShowsTheCatalogWithoutReorderingIt() {
        SaleProduct plain = new SaleProduct(UUID.randomUUID(), "SP01", "Sample", "box",
                false, BigDecimal.ONE, BigDecimal.TEN, List.of());
        List<SaleProduct> catalog = List.of(plain, product("SP02", "Another product", "", "Saline"));

        assertEquals("", plain.barcode());
        assertEquals(catalog, SaleProductSearch.find(catalog, "  "));
    }

    private static SaleProduct product(String code, String name, String barcode, String ingredient) {
        return new SaleProduct(UUID.randomUUID(), code, name, "box", false,
                BigDecimal.ONE, BigDecimal.TEN, List.of(ingredient), barcode);
    }
}
