package vn.ninhkieupharma.pharmacy.auth.service;

import vn.ninhkieupharma.pharmacy.auth.dao.UserDAO;
import vn.ninhkieupharma.pharmacy.auth.model.User;

import java.util.Objects;
import java.util.Optional;

public final class AuthenticationService {
    private final UserDAO users;

    public AuthenticationService(UserDAO users) {
        this.users = Objects.requireNonNull(users);
    }

    public Optional<User> authenticate(String username, String password) {
        if (username == null || password == null) {
            return Optional.empty();
        }
        return users.findByUsername(username.trim())
                .filter(User::active)
                .filter(user -> PasswordHasher.matches(password, user.passwordHash()));
    }
}
