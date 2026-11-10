package vn.ninhkieupharma.pharmacy.auth.service;

import org.junit.jupiter.api.Test;
import vn.ninhkieupharma.pharmacy.auth.dao.UserDAO;
import vn.ninhkieupharma.pharmacy.auth.model.User;
import vn.ninhkieupharma.pharmacy.auth.model.UserRole;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthenticationServiceTest {
    @Test
    void authenticatesExistingRelationalUserWithBcryptHash() {
        String password = UUID.randomUUID().toString();
        User sampleAdmin = new User(1L, "admin_demo",
                PasswordHasher.hash(password), UserRole.ADMIN, true);
        AuthenticationService service = new AuthenticationService(
                username -> "admin_demo".equals(username) ? Optional.of(sampleAdmin) : Optional.empty());

        assertEquals(sampleAdmin, service.authenticate(" admin_demo ", password).orElseThrow());
        assertTrue(service.authenticate("admin_demo", "wrong password").isEmpty());
        assertTrue(service.authenticate("unknown", password).isEmpty());
    }

    @Test
    void doesNotAuthenticateInactiveUsers() {
        User inactive = new User(2L, "staff_demo",
                PasswordHasher.hash("Staff@12345"), UserRole.STAFF, false);
        AuthenticationService service = new AuthenticationService(
                username -> "staff_demo".equals(username) ? Optional.of(inactive) : Optional.empty());

        assertTrue(service.authenticate("staff_demo", "Staff@12345").isEmpty());
    }
}
