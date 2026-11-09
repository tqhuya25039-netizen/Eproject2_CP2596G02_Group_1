package vn.ninhkieupharma.pharmacy.model;

import java.util.List;

public record ModuleDefinition(
        String id,
        String title,
        String group,
        String description,
        String actionLabel,
        List<String> columns,
        List<String> formFields,
        boolean report,
        boolean staffAccessible) {
}
