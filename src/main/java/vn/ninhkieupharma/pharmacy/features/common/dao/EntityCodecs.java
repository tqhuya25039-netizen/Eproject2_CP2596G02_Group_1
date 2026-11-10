package vn.ninhkieupharma.pharmacy.features.common.dao;

import java.util.List;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;

public final class EntityCodecs {
    private EntityCodecs() {
    }

    public static <T> EntityCodec<T> of(
            Function<T, UUID> id,
            Function<T, List<String>> values,
            BiFunction<UUID, List<String>, T> factory) {
        return new EntityCodec<>() {
            @Override
            public UUID id(T entity) {
                return id.apply(entity);
            }

            @Override
            public List<String> values(T entity) {
                return values.apply(entity);
            }

            @Override
            public T fromValues(UUID entityId, List<String> entityValues) {
                return factory.apply(entityId, entityValues);
            }
        };
    }
}
