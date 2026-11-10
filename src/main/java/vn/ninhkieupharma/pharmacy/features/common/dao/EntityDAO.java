package vn.ninhkieupharma.pharmacy.features.common.dao;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EntityDAO<T> {
    List<T> findAll();

    Optional<T> findById(UUID id);

    T create(T entity);

    T update(T entity);

    void delete(UUID id);
}
