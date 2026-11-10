package vn.ninhkieupharma.pharmacy.auth.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordHasherTest {
    @Test
    void hashesAndVerifiesPasswordsWithoutStoringPlaintext() {
        String hash = PasswordHasher.hash("correct horse battery");

        assertNotEquals("correct horse battery", hash);
        assertTrue(hash.startsWith("$2"));
        assertTrue(PasswordHasher.matches("correct horse battery", hash));
        assertFalse(PasswordHasher.matches("incorrect password", hash));
        assertFalse(PasswordHasher.matches(null, hash));
    }
}
