package vn.ninhkieupharma.pharmacy.domain.sales;

public enum SaleLineKind {
    PRESCRIBED("Thuốc kê đơn"),
    ADD_ON("Khách mua thêm");

    private final String label;

    SaleLineKind(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
