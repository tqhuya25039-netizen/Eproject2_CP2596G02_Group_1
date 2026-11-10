package vn.ninhkieupharma.pharmacy.features.common.dao;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import vn.ninhkieupharma.pharmacy.database.DatabaseConnection;
import vn.ninhkieupharma.pharmacy.features.common.service.FeatureServices;
import vn.ninhkieupharma.pharmacy.features.common.service.ModuleCatalog;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MySqlSchemaIntegrationTest {
    @Test
    @EnabledIfEnvironmentVariable(named = "PHARMACY_DB_INTEGRATION_TEST", matches = "true")
    void readsEveryModuleFromTheProvidedSchemaWithoutCreatingBusinessRows() throws SQLException {
        DatabaseConnection.verifySchema();
        FeatureServices services = new FeatureServices(new RelationalFeatureModuleDAO());

        for (var module : ModuleCatalog.MODULES) {
            services.forModule(module).findAll(module).forEach(row ->
                    assertEquals(module.columns().size(), row.values().size(),
                            "Rows should match displayed columns for " + module.id()));
        }
    }
}
