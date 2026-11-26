package vn.ninhkieupharma.pharmacy.common.dao;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import vn.ninhkieupharma.pharmacy.common.auth.User;
import vn.ninhkieupharma.pharmacy.common.auth.UserRole;
import vn.ninhkieupharma.pharmacy.common.service.FeatureServices;
import vn.ninhkieupharma.pharmacy.common.service.ModuleCatalog;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledIfEnvironmentVariable(named = MySqlTestSchema.ENABLED_ENV, matches = "true")
class CategoryDeletionMySqlIntegrationTest {
    @Test
    void deletesUnusedCategoriesAndPreservesCategoriesReferencedByProducts() throws Exception {
        try (MySqlTestSchema schema = MySqlTestSchema.withCurrentSchema()) {
            schema.executeScript("""
                    INSERT INTO product_categories (category_id, category_name)
                    VALUES (91001, 'Category with active product'),
                           (91002, 'Category with inactive product'),
                           (91003, 'Unused category');
                    INSERT INTO products (product_id, category_id, product_code, product_name, unit_name, is_active)
                    VALUES (91001, 91001, 'CAT-DELETE-ACTIVE', 'Active product', 'box', 1),
                           (91002, 91002, 'CAT-DELETE-INACTIVE', 'Inactive product', 'box', 0);
                    """);
            schema.runInChild(CategoryDeletionScenario.class);

            try (var connection = schema.connect();
                 var statement = connection.createStatement();
                 var resultSet = statement.executeQuery("""
                         SELECT p.category_id, p.is_active
                         FROM products p JOIN product_categories c ON p.category_id = c.category_id
                         WHERE p.product_id IN (91001, 91002)
                         ORDER BY p.product_id
                         """)) {
                assertTrue(resultSet.next());
                assertEquals(91001, resultSet.getLong("category_id"));
                assertTrue(resultSet.getBoolean("is_active"));
                assertTrue(resultSet.next());
                assertEquals(91002, resultSet.getLong("category_id"));
                assertFalse(resultSet.getBoolean("is_active"));
            }
        }
    }

    public static final class CategoryDeletionScenario {
        public static void main(String[] args) {
            String schema = System.getenv("PHARMACY_DB_NAME");
            if (schema == null || !schema.matches("pharmacy_test_[a-f0-9]{32}")) {
                throw new IllegalStateException("The category deletion scenario requires a disposable test database.");
            }

            var categories = ModuleCatalog.MODULES.stream()
                    .filter(module -> module.id().equals("UC-07"))
                    .findFirst().orElseThrow();
            var services = new FeatureServices(new RelationalFeatureModuleDAO(
                    new User(1, "admin_demo", "", UserRole.ADMIN, true)));
            var service = services.forModule(categories);
            var rows = service.findAll(categories);
            for (long categoryId : List.of(91001L, 91002L)) {
                var linked = rows.stream().filter(row -> row.id().getLeastSignificantBits() == categoryId)
                        .findFirst().orElseThrow();
                var blocked = assertThrows(IllegalArgumentException.class, () -> service.delete(categories, linked.id()));
                assertEquals("Không thể xóa danh mục vì còn sản phẩm hoặc dữ liệu khác đang tham chiếu.",
                        blocked.getMessage());
            }

            var unused = rows.stream().filter(row -> row.id().getLeastSignificantBits() == 91003)
                    .findFirst().orElseThrow();
            service.delete(categories, unused.id());
            assertFalse(service.findAll(categories).stream().anyMatch(row -> row.id().equals(unused.id())));
        }
    }
}
