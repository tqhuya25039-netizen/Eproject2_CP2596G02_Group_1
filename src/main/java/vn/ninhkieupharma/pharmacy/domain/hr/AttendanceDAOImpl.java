package vn.ninhkieupharma.pharmacy.domain.hr;

import vn.ninhkieupharma.pharmacy.common.dao.EntityCodec;
import vn.ninhkieupharma.pharmacy.common.dao.EntityCodecs;
import vn.ninhkieupharma.pharmacy.common.dao.RelationalFeatureModuleDAO;
import vn.ninhkieupharma.pharmacy.common.model.ModuleRow;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class AttendanceDAOImpl implements AttendanceDAO {
    private static final String MODULE_ID = "UC-04";
    private static final EntityCodec<Attendance> CODEC = EntityCodecs.of(Attendance::id,
            value -> List.of(value.employee(), value.shift(), value.clockIn(), value.clockOut(), value.status()),
            (id, values) -> new Attendance(id, values.get(0), values.get(1), values.get(2),
                    values.get(3), values.get(4)));

    private final RelationalFeatureModuleDAO repository = new RelationalFeatureModuleDAO();

    public static EntityCodec<Attendance> codec() {
        return CODEC;
    }

    @Override
    public List<Attendance> findAll() {
        return repository.findAll(MODULE_ID).stream().map(AttendanceDAOImpl::fromRow).toList();
    }

    @Override
    public Optional<Attendance> findById(UUID id) {
        return findAll().stream().filter(attendance -> attendance.id().equals(id)).findFirst();
    }

    @Override
    public Attendance create(Attendance entity) {
        return fromRow(repository.create(MODULE_ID, CODEC.values(entity)));
    }

    @Override
    public Attendance update(Attendance entity) {
        return fromRow(repository.update(MODULE_ID, entity.id(), CODEC.values(entity)));
    }

    @Override
    public void delete(UUID id) {
        repository.delete(MODULE_ID, id);
    }

    private static Attendance fromRow(ModuleRow row) {
        return CODEC.fromValues(row.id(), row.values());
    }
}
