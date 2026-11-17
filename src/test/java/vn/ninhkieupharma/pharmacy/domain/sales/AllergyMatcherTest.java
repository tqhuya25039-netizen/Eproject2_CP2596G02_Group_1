package vn.ninhkieupharma.pharmacy.domain.sales;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AllergyMatcherTest {
    @Test
    void matchesIngredientNamesIgnoringCaseSpacingAndVietnameseDiacritics() {
        assertTrue(AllergyMatcher.matches("Đậu phộng", "dau-phong"));
        assertTrue(AllergyMatcher.matches("Penicillin", " penicillin "));
        assertFalse(AllergyMatcher.matches("Penicillin", "Amoxicillin"));
    }
}
