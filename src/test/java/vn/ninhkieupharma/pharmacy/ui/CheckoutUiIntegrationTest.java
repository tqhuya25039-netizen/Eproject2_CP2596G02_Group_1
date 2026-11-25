package vn.ninhkieupharma.pharmacy.ui;

import javafx.application.Platform;
import javafx.event.Event;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DialogPane;
import javafx.scene.control.ListView;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import vn.ninhkieupharma.pharmacy.common.auth.UserRole;
import vn.ninhkieupharma.pharmacy.common.dao.FeatureModuleDAO;
import vn.ninhkieupharma.pharmacy.common.model.ModuleRow;
import vn.ninhkieupharma.pharmacy.common.service.ExchangeRateService;
import vn.ninhkieupharma.pharmacy.common.service.FeatureServices;
import vn.ninhkieupharma.pharmacy.common.service.Localization;
import vn.ninhkieupharma.pharmacy.common.service.ModuleCatalog;
import vn.ninhkieupharma.pharmacy.domain.sales.SaleOrderRequest;
import vn.ninhkieupharma.pharmacy.domain.sales.SalePendingOrder;
import vn.ninhkieupharma.pharmacy.domain.sales.SaleProduct;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledIfEnvironmentVariable(named = "PHARMACY_UI_INTEGRATION_TEST", matches = "true")
class CheckoutUiIntegrationTest {
    @Test
    void staffCreatesReopensEditsAndCancelsAPendingOrderThroughTheForm() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        boolean ownsToolkit;
        try {
            Platform.startup(started::countDown);
            ownsToolkit = true;
        } catch (IllegalStateException alreadyStarted) {
            started.countDown();
            ownsToolkit = false;
        }
        try {
            assertTrue(started.await(15, TimeUnit.SECONDS));
            FutureTask<Void> scenario = new FutureTask<>(() -> {
                exerciseCheckout();
                return null;
            });
            Platform.runLater(scenario);
            scenario.get(20, TimeUnit.SECONDS);
        } finally {
            if (ownsToolkit) {
                Platform.exit();
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void exerciseCheckout() {
        CheckoutRepository repository = new CheckoutRepository();
        PageRenderer renderer = new PageRenderer(
                new FeatureServices(repository), new ExchangeRateService(), 2, UserRole.STAFF);
        var checkout = ModuleCatalog.MODULES.stream()
                .filter(module -> module.id().equals("UC-17")).findFirst().orElseThrow();
        Parent page = (Parent) renderer.render(checkout);
        Stage stage = new Stage();
        stage.setScene(new Scene(page, 1080, 900));
        stage.show();
        try {
            page.applyCss();
            page.layout();
            textField(page, "Nhập tên, số điện thoại hoặc email khách hàng").setText("0910000001");
            Popup popup = Window.getWindows().stream().filter(Popup.class::isInstance)
                    .map(Popup.class::cast).findFirst().orElseThrow();
            var suggestions = (ListView<ModuleRow>) popup.getContent().getFirst();
            suggestions.getSelectionModel().selectFirst();
            Event.fireEvent(suggestions, new KeyEvent(KeyEvent.KEY_PRESSED, "", "",
                    KeyCode.ENTER, false, false, false, false));

            var products = (ComboBox<SaleProduct>) page.lookup(".combo-box");
            products.getSelectionModel().selectFirst();
            TextField quantity = textField(page, "Số lượng");
            quantity.setText("2");
            button(page, "Lưu đơn Pending").fire();
            assertEquals(BigDecimal.valueOf(2), repository.pending.request().lines().getFirst().quantity());

            button(page, "Đơn mới").fire();
            var orders = (TableView<ModuleRow>) page.lookup(".data-table");
            orders.getSelectionModel().selectFirst();
            button(page, "Tải đơn chờ đã chọn").fire();
            assertEquals("2", quantity.getText());
            quantity.setText("3");
            button(page, "Lưu đơn Pending").fire();
            assertEquals(1, repository.pending.revision());
            assertEquals(BigDecimal.valueOf(3), repository.pending.request().lines().getFirst().quantity());

            Platform.runLater(() -> {
                DialogPane dialog = Window.getWindows().stream()
                        .map(window -> window.getScene().getRoot())
                        .filter(DialogPane.class::isInstance)
                        .map(DialogPane.class::cast).findFirst().orElseThrow();
                ((Button) dialog.lookupButton(ButtonType.OK)).fire();
            });
            button(page, "Hủy đơn Pending").fire();
            assertEquals("VOIDED", orders.getItems().getFirst().values().get(5));
            assertTrue(button(page, "Hủy đơn Pending").isDisabled());
        } finally {
            stage.close();
        }
    }

    private static TextField textField(Parent page, String prompt) {
        return page.lookupAll(".text-field").stream()
                .filter(TextField.class::isInstance)
                .map(TextField.class::cast)
                .filter(field -> Localization.text(prompt).equals(field.getPromptText()))
                .findFirst().orElseThrow();
    }

    private static Button button(Parent page, String title) {
        return page.lookupAll(".button").stream()
                .filter(Button.class::isInstance)
                .map(Button.class::cast)
                .filter(button -> Localization.text(title).equals(button.getText()))
                .findFirst().orElseThrow();
    }

    private static final class CheckoutRepository implements FeatureModuleDAO {
        private final ModuleRow customer = new ModuleRow(UUID.randomUUID(),
                List.of("Customer", "0910000001", "", "", "0", ""));
        private final SaleProduct product = new SaleProduct(UUID.randomUUID(), "SP01", "Product",
                "box", false, BigDecimal.TEN, BigDecimal.valueOf(100), List.of());
        private SalePendingOrder pending;
        private String status;

        @Override
        public List<ModuleRow> findAll(String moduleId) {
            return pending == null ? List.of() : List.of(new ModuleRow(pending.id(),
                    List.of(pending.saleCode(), "2026-11-24 12:00", "Customer", "Staff",
                            pending.subtotal().toPlainString(), status)));
        }

        @Override
        public List<ModuleRow> findCustomersAvailableForSale() {
            return List.of(customer);
        }

        @Override
        public List<SaleProduct> findSaleProducts() {
            return List.of(product);
        }

        @Override
        public List<vn.ninhkieupharma.pharmacy.domain.sales.SaleAllergyWarning> findSaleAllergyWarnings(
                Optional<UUID> customerId, List<UUID> productIds) {
            return List.of();
        }

        @Override
        public SalePendingOrder createPendingSale(long userId, SaleOrderRequest request) {
            status = "PENDING";
            pending = new SalePendingOrder(UUID.randomUUID(), "TEST-ORDER", request, subtotal(request), 0);
            return pending;
        }

        @Override
        public SalePendingOrder updatePendingSale(long userId, UUID saleId, long revision, SaleOrderRequest request) {
            assertEquals(pending.id(), saleId);
            assertEquals(pending.revision(), revision);
            pending = new SalePendingOrder(saleId, pending.saleCode(), request, subtotal(request), revision + 1);
            return pending;
        }

        @Override
        public SalePendingOrder findPendingSale(UUID saleId) {
            assertEquals(pending.id(), saleId);
            return pending;
        }

        @Override
        public void cancelPendingSale(long userId, UUID saleId, long revision) {
            assertEquals(pending.id(), saleId);
            assertEquals(pending.revision(), revision);
            status = "VOIDED";
        }

        private BigDecimal subtotal(SaleOrderRequest request) {
            return product.sellingPrice().multiply(request.lines().getFirst().quantity());
        }

        @Override
        public void replaceAll(String moduleId, List<List<String>> values) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ModuleRow create(String moduleId, List<String> values) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ModuleRow update(String moduleId, UUID rowId, List<String> values) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void delete(String moduleId, UUID rowId) {
            throw new UnsupportedOperationException();
        }
    }
}
