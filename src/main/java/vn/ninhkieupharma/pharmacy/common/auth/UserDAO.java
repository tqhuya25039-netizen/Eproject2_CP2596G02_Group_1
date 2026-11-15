package vn.ninhkieupharma.pharmacy.common.auth;

import vn.ninhkieupharma.pharmacy.common.auth.User;

public interface UserDAO {
    java.util.Optional<User> findByUsername(String username);
}
