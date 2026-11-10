package vn.ninhkieupharma.pharmacy.features.common.dao;

import vn.ninhkieupharma.pharmacy.features.common.model.ModuleRow;

import java.util.List;
import java.util.UUID;

public final class TypedEntityModuleDAOAdapter<T> implements FeatureModuleDAO {
    private final String moduleId;
    private final EntityDAO<T> entityDAO;
    private final EntityCodec<T> codec;

    public TypedEntityModuleDAOAdapter(String moduleId, EntityDAO<T> entityDAO, EntityCodec<T> codec) {
        this.moduleId = moduleId;
        this.entityDAO = entityDAO;
        this.codec = codec;
    }

    @Override
    public List<ModuleRow> findAll(String requestedModule) {
        requireModule(requestedModule);
        return entityDAO.findAll().stream()
                .map(entity -> new ModuleRow(codec.id(entity), codec.values(entity)))
                .toList();
    }

    @Override
    public void replaceAll(String requestedModule, List<List<String>> values) {
        requireModule(requestedModule);
        throw new UnsupportedOperationException("Bulk replacement is not supported for business records.");
    }

    @Override
    public ModuleRow create(String requestedModule, List<String> values) {
        requireModule(requestedModule);
        T entity = codec.fromValues(UUID.randomUUID(), values);
        T created = entityDAO.create(entity);
        return new ModuleRow(codec.id(created), codec.values(created));
    }

    @Override
    public ModuleRow update(String requestedModule, UUID rowId, List<String> values) {
        requireModule(requestedModule);
        T entity = codec.fromValues(rowId, values);
        T updated = entityDAO.update(entity);
        return new ModuleRow(codec.id(updated), codec.values(updated));
    }

    @Override
    public void delete(String requestedModule, UUID rowId) {
        requireModule(requestedModule);
        entityDAO.delete(rowId);
    }

    private void requireModule(String requestedModule) {
        if (!moduleId.equals(requestedModule)) {
            throw new IllegalArgumentException("DAO adapter only supports " + moduleId + ".");
        }
    }
}
