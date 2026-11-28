package vn.ninhkieupharma.pharmacy.domain.sales;

public enum DoseSession {
    MORNING("Sáng", 1),
    NOON("Trưa", 2),
    AFTERNOON("Chiều", 4),
    EVENING("Tối", 8);

    private final String label;
    private final int mask;

    DoseSession(String label, int mask) {
        this.label = label;
        this.mask = mask;
    }

    public String label() {
        return label;
    }

    public int mask() {
        return mask;
    }
}
