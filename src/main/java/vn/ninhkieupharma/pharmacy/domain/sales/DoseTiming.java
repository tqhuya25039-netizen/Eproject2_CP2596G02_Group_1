package vn.ninhkieupharma.pharmacy.domain.sales;

public enum DoseTiming {
    BEFORE_MEAL("Trước ăn"),
    WITH_MEAL("Trong bữa ăn"),
    AFTER_MEAL("Sau ăn"),
    BEFORE_BED("Trước khi ngủ");

    private final String label;

    DoseTiming(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
