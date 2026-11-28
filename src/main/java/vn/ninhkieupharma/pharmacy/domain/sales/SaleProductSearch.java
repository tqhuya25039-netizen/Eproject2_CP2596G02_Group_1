package vn.ninhkieupharma.pharmacy.domain.sales;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public final class SaleProductSearch {
    private static final Pattern MARKS = Pattern.compile("\\p{M}+");
    private static final Pattern SEPARATORS = Pattern.compile("[^\\p{L}\\p{N}]+");
    private static final Pattern STRENGTH_BOUNDARIES = Pattern.compile(
            "(?<=\\p{L})(?=\\p{N})|(?<=\\p{N})(?=\\p{L})");
    private static final int NO_MATCH = -1;

    private final List<SaleProduct> products;
    private final List<Entry> entries;

    public SaleProductSearch(List<SaleProduct> products) {
        this.products = List.copyOf(products);
        this.entries = this.products.stream().map(SaleProductSearch::index).toList();
    }

    public static List<SaleProduct> find(List<SaleProduct> products, String query) {
        return new SaleProductSearch(products).find(query);
    }

    public List<SaleProduct> find(String query) {
        if (query == null || query.isBlank()) {
            return products;
        }
        String phrase = normalize(query);
        if (phrase.isEmpty()) {
            return List.of();
        }
        String identifier = phrase.replace(" ", "");
        List<String> terms = words(phrase);
        String rawIdentifier = query.strip();
        return entries.stream()
                .map(entry -> new Match(entry, rank(entry, phrase, rawIdentifier, identifier, terms)))
                .filter(match -> match.rank() != NO_MATCH)
                .sorted(Comparator.comparingInt(Match::rank)
                        .thenComparing(match -> match.entry().name())
                        .thenComparing(match -> match.entry().code()))
                .map(match -> match.entry().product())
                .toList();
    }

    private static Entry index(SaleProduct product) {
        String name = normalize(product.name());
        String code = normalize(product.code());
        List<String> nameWords = words(name);
        List<String> searchableWords = new ArrayList<>(nameWords);
        searchableWords.addAll(words(code));
        searchableWords.add(code.replace(" ", ""));
        for (String ingredient : product.ingredients()) {
            searchableWords.addAll(words(normalize(ingredient)));
        }
        return new Entry(product, normalize(product.barcode()).replace(" ", ""), code.replace(" ", ""),
                name, nameWords, List.copyOf(searchableWords));
    }

    private static int rank(Entry entry, String phrase, String rawIdentifier, String identifier, List<String> terms) {
        if (entry.barcode().equals(identifier)) {
            return entry.product().barcode().equalsIgnoreCase(rawIdentifier) ? 0 : 1;
        }
        if (entry.code().equals(identifier)) {
            return entry.product().code().equalsIgnoreCase(rawIdentifier) ? 2 : 3;
        }
        if (entry.name().equals(phrase)) {
            return 4;
        }
        boolean nameStartsWithTerms = matches(terms, entry.nameWords(), true);
        if (nameStartsWithTerms && entry.name().startsWith(phrase)) {
            return 5;
        }
        if (entry.code().startsWith(identifier) || entry.barcode().startsWith(identifier)) {
            return 6;
        }
        if (nameStartsWithTerms) {
            return 7;
        }
        if (matches(terms, entry.searchableWords(), true)) {
            return 8;
        }
        if (matches(terms, entry.nameWords(), false)) {
            return 9;
        }
        return matches(terms, entry.searchableWords(), false) ? 10 : NO_MATCH;
    }

    private static boolean matches(List<String> terms, List<String> candidates, boolean prefix) {
        return terms.stream().allMatch(term -> {
            // A strength such as 50 must not match 500. Identifiers have their own prefix checks.
            if (term.codePoints().allMatch(Character::isDigit)) {
                return candidates.contains(term);
            }
            return candidates.stream().anyMatch(word -> prefix ? word.startsWith(term) : word.contains(term));
        });
    }

    private static List<String> words(String phrase) {
        return phrase.isEmpty() ? List.of() : List.of(phrase.split(" "));
    }

    private static String normalize(String value) {
        String decomposed = Normalizer.normalize(value.toLowerCase(Locale.ROOT).replace('đ', 'd'),
                Normalizer.Form.NFD);
        String folded = MARKS.matcher(decomposed).replaceAll("");
        String separated = SEPARATORS.matcher(folded).replaceAll(" ");
        return STRENGTH_BOUNDARIES.matcher(separated).replaceAll(" ").strip();
    }

    private record Entry(
            SaleProduct product, String barcode, String code, String name,
            List<String> nameWords, List<String> searchableWords) {
    }

    private record Match(Entry entry, int rank) {
    }
}
