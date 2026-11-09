package vn.ninhkieupharma.pharmacy.model;

public enum UserRole {
    ADMIN("Quản trị viên"),
    STAFF("Nhân viên");

    private final String displayName;

    UserRole(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
