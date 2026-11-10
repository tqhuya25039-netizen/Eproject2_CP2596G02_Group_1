package vn.ninhkieupharma.pharmacy.auth.model;

import vn.ninhkieupharma.pharmacy.features.common.service.Localization;

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
