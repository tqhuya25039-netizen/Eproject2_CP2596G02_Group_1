package vn.ninhkieupharma.pharmacy.features.common.dao;

import java.util.Map;

public final class TypedBusinessDAOs {
    private TypedBusinessDAOs() {
    }

    public static Map<String, FeatureModuleDAO> createAdapters() {
        RelationalFeatureModuleDAO dao = new RelationalFeatureModuleDAO();
        return Map.ofEntries(
                Map.entry("UC-01", dao), Map.entry("UC-02", dao), Map.entry("UC-03", dao),
                Map.entry("UC-04", dao), Map.entry("UC-07", dao), Map.entry("UC-08", dao),
                Map.entry("UC-09", dao), Map.entry("UC-12", dao), Map.entry("UC-13", dao),
                Map.entry("UC-14", dao), Map.entry("UC-16", dao), Map.entry("UC-17", dao),
                Map.entry("UC-18", dao));
    }
}
