package vn.ninhkieupharma.pharmacy.common.auth;

import vn.ninhkieupharma.pharmacy.common.service.Localization;

public enum UserRole {
    ADMIN("Quản trị viên"),
    STAFF("Nhân viên");

    private final String displayName;

    UserRole(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return Localization.text(displayName);
    }

    @Override
    public String toString() {
        return displayName();
    }
}
