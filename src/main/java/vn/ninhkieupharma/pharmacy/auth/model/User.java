package vn.ninhkieupharma.pharmacy.auth.model;

public record User(long id, String username, String passwordHash, UserRole role, boolean active) {
}
