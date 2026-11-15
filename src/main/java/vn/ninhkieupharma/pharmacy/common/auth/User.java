package vn.ninhkieupharma.pharmacy.common.auth;

public record User(long id, String username, String passwordHash, UserRole role, boolean active) {
}
