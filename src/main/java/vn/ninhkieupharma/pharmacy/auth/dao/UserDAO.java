package vn.ninhkieupharma.pharmacy.auth.dao;

import vn.ninhkieupharma.pharmacy.auth.model.User;

public interface UserDAO {
    java.util.Optional<User> findByUsername(String username);
}
