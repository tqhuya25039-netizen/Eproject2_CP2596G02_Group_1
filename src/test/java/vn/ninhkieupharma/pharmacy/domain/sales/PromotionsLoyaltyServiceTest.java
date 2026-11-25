package vn.ninhkieupharma.pharmacy.domain.sales;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vn.ninhkieupharma.pharmacy.common.dao.FeatureModuleDAO;
import vn.ninhkieupharma.pharmacy.common.model.ModuleDefinition;
import vn.ninhkieupharma.pharmacy.common.model.ModuleRow;
import vn.ninhkieupharma.pharmacy.common.service.ModuleCatalog;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PromotionsLoyaltyServiceTest {
    private final ModuleDefinition module = ModuleCatalog.MODULES.stream()
            .filter(candidate -> candidate.id().equals("UC-18"))
            .findFirst()
            .orElseThrow();
    private InMemoryPromotionDao dao;
    private PromotionsLoyaltyService service;

    @BeforeEach
    void setUp() {
        dao = new InMemoryPromotionDao();
        service = new PromotionsLoyaltyService(dao);
    }

    @Test
    void validatesOrderedDatesAndPositivePointRates() {
        List<String> values = validValues();
        service.validateConfiguration(module, values, true, null);

        List<String> invalidDateOrder = new ArrayList<>(values);
        invalidDateOrder.set(4, LocalDate.now().minusDays(1).toString());
        assertThrows(IllegalArgumentException.class,
                () -> service.validateConfiguration(module, invalidDateOrder, true, null));

        List<String> pastStart = new ArrayList<>(values);
        pastStart.set(3, LocalDate.now().minusDays(1).toString());
        assertThrows(IllegalArgumentException.class,
                () -> service.validateConfiguration(module, pastStart, true, null));

        List<String> invalidTierRate = new ArrayList<>(values);
        invalidTierRate.set(8, "0:0");
        assertThrows(IllegalArgumentException.class,
                () -> service.validateConfiguration(module, invalidTierRate, true, null));

        List<String> invalidRedemptionValue = new ArrayList<>(values);
        invalidRedemptionValue.set(7, "0");
        assertThrows(IllegalArgumentException.class,
                () -> service.validateConfiguration(module, invalidRedemptionValue, true, null));

        List<String> unsupportedThreshold = new ArrayList<>(values);
        unsupportedThreshold.set(8, "0:1;0.001:2");
        assertThrows(IllegalArgumentException.class,
                () -> service.validateConfiguration(module, unsupportedThreshold, true, null));
    }

    @Test
    void updateRejectsIncorrectFieldCountBeforeReadingStartDate() {
        ModuleRow created = service.create(module, validValues());

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.update(module, created.id(), List.of("short")));

        assertTrue(error.getMessage().contains("10"));
    }

    @Test
    void deleteDeactivatesInsteadOfRemovingTheProgram() {
        ModuleRow created = service.create(module, validValues());

        service.delete(module, created.id());

        assertEquals(1, service.findAll(module).size());
        assertEquals("INACTIVE", service.findAll(module).getFirst().values().get(9));
    }

    @Test
    void endingProgramImmediatelyClosesItsDateWindowAndDeactivatesIt() {
        LocalDate today = LocalDate.now();
        List<String> values = List.of(
                "PROMO-END", "End now", "0",
                today.minusDays(2).toString(),
                today.toString(),
                today.plusDays(2).toString(),
                today.plusDays(4).toString(),
                "1000",
                "0:1",
                "ACTIVE");
        ModuleRow created = dao.create("UC-18", values);

        service.finishImmediately(module, created.id());

        List<String> updated = service.findAll(module).getFirst().values();
        assertEquals(today.toString(), updated.get(5));
        assertEquals(today.toString(), updated.get(6));
        assertEquals("INACTIVE", updated.get(9));
    }

    @Test
    void endingAnExpiredProgramPreservesItsOriginalDatesAndTiers() {
        LocalDate today = LocalDate.now();
        List<String> values = List.of(
                "PROMO-EXPIRED", "Expired program", "0",
                today.minusDays(5).toString(), today.minusDays(4).toString(),
                today.minusDays(3).toString(), today.minusDays(2).toString(),
                "1000", "0:1;100000:1.5", "ACTIVE");
        ModuleRow created = dao.create("UC-18", values);

        service.finishImmediately(module, created.id());

        List<String> updated = service.findAll(module).getFirst().values();
        assertEquals(values.subList(0, 9), updated.subList(0, 9));
        assertEquals("INACTIVE", updated.get(9));
    }

    private List<String> validValues() {
        LocalDate today = LocalDate.now();
        return List.of(
                "PROMO-TEST", "Test program", "0",
                today.toString(),
                today.plusDays(1).toString(),
                today.plusDays(2).toString(),
                today.plusDays(3).toString(),
                "1000",
                "0:1;100000:1.5",
                "ACTIVE");
    }

    private static final class InMemoryPromotionDao implements FeatureModuleDAO {
        private final Map<UUID, ModuleRow> rows = new LinkedHashMap<>();

        @Override
        public List<ModuleRow> findAll(String moduleId) {
            return List.copyOf(rows.values());
        }

        @Override
        public void replaceAll(String moduleId, List<List<String>> values) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ModuleRow create(String moduleId, List<String> values) {
            ModuleRow row = new ModuleRow(UUID.randomUUID(), values);
            rows.put(row.id(), row);
            return row;
        }

        @Override
        public ModuleRow update(String moduleId, UUID rowId, List<String> values) {
            ModuleRow row = new ModuleRow(rowId, values);
            rows.put(rowId, row);
            return row;
        }

        @Override
        public void delete(String moduleId, UUID rowId) {
            rows.remove(rowId);
        }
    }
}
