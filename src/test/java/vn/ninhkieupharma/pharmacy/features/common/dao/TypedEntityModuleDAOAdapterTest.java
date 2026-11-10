package vn.ninhkieupharma.pharmacy.features.common.dao;

import org.junit.jupiter.api.Test;
import vn.ninhkieupharma.pharmacy.features.common.model.ModuleRow;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TypedEntityModuleDAOAdapterTest {
    @Test
    void adaptsTypedCrudWithoutRequiringSqlServer() {
        EntityCodec<TestEntity> codec = EntityCodecs.of(
                TestEntity::id,
                entity -> List.of(entity.code(), entity.name()),
                (id, values) -> new TestEntity(id, values.get(0), values.get(1)));
        InMemoryEntityDAO<TestEntity> dao = new InMemoryEntityDAO<>();
        FeatureModuleDAO adapter = new TypedEntityModuleDAOAdapter<>("UC-90", dao, codec);

        ModuleRow created = adapter.create("UC-90", List.of("T-1", "Typed row"));
        assertEquals(List.of("T-1", "Typed row"), adapter.findAll("UC-90").get(0).values());
        adapter.update("UC-90", created.id(), List.of("T-1", "Updated"));
        assertEquals("Updated", dao.findById(created.id()).orElseThrow().name());
        adapter.delete("UC-90", created.id());
        assertEquals(List.of(), adapter.findAll("UC-90"));
        assertThrows(IllegalArgumentException.class, () -> adapter.findAll("UC-91"));
    }

    private record TestEntity(UUID id, String code, String name) {
    }

    private static final class InMemoryEntityDAO<T extends TestEntity> implements EntityDAO<T> {
        private final List<T> entities = new ArrayList<>();

        @Override
        public List<T> findAll() {
            return List.copyOf(entities);
        }

        @Override
        public Optional<T> findById(UUID id) {
            return entities.stream().filter(entity -> entity.id().equals(id)).findFirst();
        }

        @Override
        public T create(T entity) {
            entities.add(entity);
            return entity;
        }

        @Override
        public T update(T entity) {
            entities.removeIf(existing -> existing.id().equals(entity.id()));
            entities.add(entity);
            return entity;
        }

        @Override
        public void delete(UUID id) {
            entities.removeIf(entity -> entity.id().equals(id));
        }
    }
}
