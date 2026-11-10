package vn.ninhkieupharma.pharmacy.features.common.model;

import java.util.List;
import java.util.UUID;

public record ModuleRow(UUID id, List<String> values) {
    public ModuleRow {
        values = List.copyOf(values);
    }
}
