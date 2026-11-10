package vn.ninhkieupharma.pharmacy.features.common.dao;

import java.util.List;
import java.util.UUID;

public interface EntityCodec<T> {
    UUID id(T entity);

    List<String> values(T entity);

    T fromValues(UUID id, List<String> values);
}
