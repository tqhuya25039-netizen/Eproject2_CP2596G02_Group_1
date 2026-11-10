package vn.ninhkieupharma.pharmacy.auth.dao;

import org.junit.jupiter.api.Test;
import vn.ninhkieupharma.pharmacy.auth.service.PasswordHasher;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserDAOImplTest {
    @Test
    void sampleAccountHashIsBcryptAndNeverStoresThePlaintextPassword() {
        String password = UUID.randomUUID().toString();
        String hash = UserDAOImpl.samplePasswordHash(password);

        assertTrue(hash.startsWith("$2a$"));
        assertTrue(PasswordHasher.matches(password, hash));
        assertFalse(hash.equals(password));
        assertFalse(PasswordHasher.matches("wrong", hash));
    }
}
