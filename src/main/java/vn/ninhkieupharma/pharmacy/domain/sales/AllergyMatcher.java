package vn.ninhkieupharma.pharmacy.domain.sales;

import java.text.Normalizer;
import java.util.Locale;

public final class AllergyMatcher {
    private AllergyMatcher() {
    }

    public static boolean matches(String allergen, String ingredient) {
        return canonicalize(allergen).equals(canonicalize(ingredient));
    }

    public static String canonicalize(String value) {
        return Normalizer.normalize(value.replace('đ', 'd').replace('Đ', 'D'), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]", "");
    }
}
