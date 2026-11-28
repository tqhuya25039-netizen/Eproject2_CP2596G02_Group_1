package vn.ninhkieupharma.pharmacy.domain.sales;

public enum DoseUnit {
    TABLET("Viên"),
    CAPSULE("Viên nang"),
    PACKET("Gói"),
    ML("mL"),
    DROP("Giọt"),
    DOSE("Liều");

    private final String label;

    DoseUnit(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
