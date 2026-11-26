package vn.ninhkieupharma.pharmacy.ui;

import javafx.application.Platform;
import javafx.event.Event;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
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
import vn.ninhkieupharma.pharmacy.domain.sales.DoseSession;
import vn.ninhkieupharma.pharmacy.domain.sales.DoseTiming;
import vn.ninhkieupharma.pharmacy.domain.sales.DoseUnit;
import vn.ninhkieupharma.pharmacy.domain.sales.SaleLineKind;
import vn.ninhkieupharma.pharmacy.domain.sales.SaleLineRequest;
import vn.ninhkieupharma.pharmacy.domain.sales.SaleOrderRequest;
import vn.ninhkieupharma.pharmacy.domain.sales.SalePendingOrder;
import vn.ninhkieupharma.pharmacy.domain.sales.SaleProduct;
import vn.ninhkieupharma.pharmacy.domain.sales.SaleStockPreview;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledIfEnvironmentVariable(named = "PHARMACY_UI_INTEGRATION_TEST", matches = "true")
class CheckoutUiIntegrationTest {
    @Test
    void staffCreatesReopensEditsAndCancelsAMixedPendingOrderThroughTheForm() throws Exception {
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
                boolean originalLanguage = Localization.isEnglish();
                if (!originalLanguage) {
                    Localization.toggleLanguage();
                }
                try {
                    exerciseCheckout();
                } finally {
                    if (Localization.isEnglish() != originalLanguage) {
                        Localization.toggleLanguage();
                    }
                }
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
        Parent page = (Parent) renderer.render(Localization.localize(checkout));
        Stage stage = new Stage();
        Scene scene = new Scene(page, 760, 900);
        scene.getStylesheets().add(CheckoutUiIntegrationTest.class
                .getResource("/vn/ninhkieupharma/pharmacy/styles/app.css").toExternalForm());
        stage.setScene(scene);
        stage.show();
        try {
            page.applyCss();
            page.layout();
            var prescription = (TitledPane) page.lookup("#checkout-prescription");
            var medications = page.lookup("#checkout-medications");
            assertTrue(prescription.isExpanded());
            assertTrue(prescription.getParent().getChildrenUnmodifiable().indexOf(prescription)
                    < medications.getParent().getChildrenUnmodifiable().indexOf(medications));
            var pointBalance = (Label) page.lookup("#checkout-points-balance");
            var pointError = (Label) page.lookup("#checkout-points-error");
            var pointInput = (TextField) page.lookup("#checkout-points-requested");
            assertEquals("Select a customer to see their points balance.", pointBalance.getText());
            assertTrue(pointInput.isDisabled());

            selectCustomer(page, "0910000001");
            assertEquals("Current points: 1,250", pointBalance.getText());
            pointInput.setText("12");
            selectCustomer(page, "0910000002");
            assertEquals("Current points: 0", pointBalance.getText());
            assertEquals("0", pointInput.getText());
            pointInput.setText("1");
            assertEquals("The requested points exceed the customer's balance.", pointError.getText());
            selectCustomer(page, "0910000001");
            assertEquals("0", pointInput.getText());
            assertFalse(pointError.isVisible());

            var search = (TextField) page.lookup("#checkout-product-search");
            var products = (ListView<SaleProduct>) page.lookup("#checkout-product-results");
            assertEquals("Product name, code, barcode, or ingredient", search.getPromptText());
            assertTrue(button(page, "Add to cart").isDisabled());
            search.setText("nonexistent-product");
            assertTrue(products.getItems().isEmpty());
            pressEnter(search);
            assertTrue(page.lookupAll(".checkout-line").isEmpty());

            search.setText("nuoc muoi");
            assertEquals(2, products.getItems().size());
            pressEnter(search);
            assertTrue(page.lookupAll(".checkout-line").isEmpty());
            search.setText("natri clorid 0.9");
            assertEquals(List.of(repository.product), products.getItems());

            search.setText(repository.product.barcode());
            pressEnter(search);
            search.setText(repository.product.barcode());
            pressEnter(search);
            assertEquals(1, page.lookupAll(".checkout-line").size());
            TextField initialQuantity = textField(page, "Số lượng");
            assertEquals("2", initialQuantity.getText());
            ((Button) page.lookup("#checkout-check-stock")).fire();
            Label pickingHint = (Label) page.lookup("#checkout-stock-status");
            assertTrue(pickingHint.isVisible());
            assertTrue(pickingHint.getText().contains(repository.product.name() + " · 2 box"));
            assertTrue(pickingHint.getText().contains("KE-A1 (Front shelf)"));
            assertTrue(pickingHint.getText().contains("FIXTURE-LOT"));
            initialQuantity.setText("3");
            assertEquals("", pickingHint.getText());
            assertFalse(pickingHint.isVisible());
            initialQuantity.setText("2");
            var cart = (VBox) page.lookup(".checkout-cart");
            var prescribed = (VBox) cart.getChildren().getFirst();
            assertTrue(((RadioButton) prescribed.lookup("#checkout-line-add-on")).isSelected());
            prescription.setExpanded(false);
            ((RadioButton) prescribed.lookup("#checkout-line-prescribed")).fire();
            assertTrue(prescription.isExpanded());
            textField(page, "Mã toa thuốc").setText("TOA-01");
            ((DatePicker) prescription.lookup(".date-picker")).setValue(LocalDate.now().minusDays(1));
            textField(page, "Bác sĩ kê đơn").setText("Test doctor");
            ((TextField) prescribed.lookup("#checkout-dose-amount")).setText("1,5");
            selectOption(prescribed, "#checkout-dose-unit", "Sachet");
            selectOption(prescribed, "#checkout-dose-timing", "After meals");
            ((CheckBox) prescribed.lookup("#checkout-dose-morning")).fire();
            ((CheckBox) prescribed.lookup("#checkout-dose-evening")).fire();
            ((TextField) prescribed.lookup("#checkout-dose-notes")).setText("Test note");

            search.setText(repository.product.barcode());
            pressEnter(search);
            var addOn = (VBox) cart.getChildren().getLast();
            assertTrue(((RadioButton) addOn.lookup("#checkout-line-add-on")).isSelected());
            assertEquals(2, cart.getChildren().size());

            search.setText(repository.prescriptionOnly.barcode());
            pressEnter(search);
            var required = (VBox) cart.getChildren().getLast();
            assertTrue(((RadioButton) required.lookup("#checkout-line-prescribed")).isSelected());
            assertTrue(required.lookup("#checkout-line-add-on").isDisabled());

            Button save = button(page, "Save pending order");
            for (String invalid : List.of("-1", "0.5", "abc", "4294967296")) {
                pointInput.setText(invalid);
                assertEquals("Enter a valid non-negative whole number of points.", pointError.getText());
                assertTrue(save.isDisabled());
            }
            pointInput.setText("1251");
            assertEquals("The requested points exceed the customer's balance.", pointError.getText());
            assertTrue(save.isDisabled());
            pointInput.setText("12");
            assertFalse(save.isDisabled());
            save.fire();
            assertEquals(12L, repository.pending.request().requestedPoints());
            var saved = repository.pending.request().lines();
            assertEquals(List.of(SaleLineKind.PRESCRIBED, SaleLineKind.ADD_ON, SaleLineKind.PRESCRIBED),
                    saved.stream().map(line -> line.kind()).toList());
            assertEquals(BigDecimal.valueOf(2), saved.getFirst().quantity());
            assertEquals(repository.product.id(), saved.getFirst().productId());
            assertEquals(repository.product.id(), saved.get(1).productId());
            assertEquals(new BigDecimal("1.5"), saved.getFirst().dose().amount());
            assertEquals(DoseUnit.PACKET, saved.getFirst().dose().unit());
            assertEquals(DoseTiming.AFTER_MEAL, saved.getFirst().dose().timing());
            assertEquals(Set.of(DoseSession.MORNING, DoseSession.EVENING), saved.getFirst().dose().sessions());
            assertEquals("Test note", saved.getFirst().dosageInstructions());

            button(page, "Đơn mới").fire();
            assertTrue(pointInput.isDisabled());
            repository.customerPoints = 5;
            var orders = (TableView<ModuleRow>) page.lookup(".data-table");
            assertEquals("Pending", orders.getColumns().get(5).getCellData(0));
            orders.getSelectionModel().selectFirst();
            button(page, "Open selected pending order").fire();
            assertEquals("Current points: 5", pointBalance.getText());
            assertEquals("12", pointInput.getText());
            assertTrue(save.isDisabled());
            assertTrue(button(page, "Tính tiền và xác nhận").isDisabled());
            repository.customerPoints = 15;
            button(page, "Làm mới danh sách khách").fire();
            assertEquals("Current points: 15", pointBalance.getText());
            assertEquals("12", pointInput.getText());
            assertFalse(pointError.isVisible());
            var reopened = (VBox) cart.getChildren().getFirst();
            assertTrue(((RadioButton) reopened.lookup("#checkout-line-prescribed")).isSelected());
            assertTrue(((CheckBox) reopened.lookup("#checkout-dose-evening")).isSelected());
            assertEquals("After meals",
                    ((ComboBox<?>) reopened.lookup("#checkout-dose-timing")).getValue().toString());
            assertEquals("TOA-01", textField(page, "Mã toa thuốc").getText());
            TextField quantity = textField(reopened, "Số lượng");
            assertEquals("2", quantity.getText());
            quantity.setText("3");
            button(page, "Save pending order").fire();
            assertEquals(1, repository.pending.revision());
            assertEquals(BigDecimal.valueOf(3), repository.pending.request().lines().getFirst().quantity());
            assertEquals(saved.getFirst().dose(), repository.pending.request().lines().getFirst().dose());

            String[] dialogTexts = new String[2];
            Platform.runLater(() -> {
                DialogPane dialog = Window.getWindows().stream()
                        .map(window -> window.getScene().getRoot())
                        .filter(DialogPane.class::isInstance)
                        .map(DialogPane.class::cast).findFirst().orElseThrow();
                ((Button) dialog.lookupButton(ButtonType.OK)).fire();
                dialogTexts[0] = dialog.getContentText();
                dialogTexts[1] = ((Button) dialog.lookupButton(ButtonType.CANCEL)).getText();
            });
            button(page, "Cancel pending order").fire();
            assertEquals("Cancelling this pending order will prevent further edits.", dialogTexts[0]);
            assertEquals("Cancel", dialogTexts[1]);
            assertEquals("VOIDED", orders.getItems().getFirst().values().get(5));
            assertEquals("Voided", orders.getColumns().get(5).getCellData(0));
            assertTrue(button(page, "Cancel pending order").isDisabled());
        } finally {
            stage.close();
        }
    }

    private static void pressEnter(TextField field) {
        Event.fireEvent(field, new KeyEvent(KeyEvent.KEY_PRESSED, "", "",
                KeyCode.ENTER, false, false, false, false));
    }

    private static void selectCustomer(Parent page, String phoneNumber) {
        textField(page, "Nhập tên, số điện thoại hoặc email khách hàng").setText(phoneNumber);
        Popup popup = Window.getWindows().stream().filter(Popup.class::isInstance)
                .map(Popup.class::cast).filter(Popup::isShowing).findFirst().orElseThrow();
        var suggestions = (ListView<?>) popup.getContent().getFirst();
        suggestions.getSelectionModel().selectFirst();
        Event.fireEvent(suggestions, new KeyEvent(KeyEvent.KEY_PRESSED, "", "",
                KeyCode.ENTER, false, false, false, false));
    }

    private static void selectOption(Parent row, String selector, String label) {
        ComboBox<?> combo = (ComboBox<?>) row.lookup(selector);
        for (int index = 0; index < combo.getItems().size(); index++) {
            if (label.equals(combo.getItems().get(index).toString())) {
                combo.getSelectionModel().select(index);
                return;
            }
        }
        throw new AssertionError("Missing option " + label);
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
        private final UUID customerId = UUID.randomUUID();
        private final ModuleRow customerWithoutPoints = new ModuleRow(UUID.randomUUID(),
                List.of("Second customer", "0910000002", "", "", "0", ""));
        private int customerPoints = 1250;
        private final SaleProduct product = new SaleProduct(UUID.randomUUID(), "SP01", "Nước muối 0,9%",
                "box", false, BigDecimal.valueOf(25000), BigDecimal.valueOf(100),
                List.of("Natri clorid 0,9%"), "8930000001009");
        private final SaleProduct otherProduct = new SaleProduct(UUID.randomUUID(), "SP02", "Nước muối 3%",
                "box", false, BigDecimal.valueOf(30000), BigDecimal.ZERO,
                List.of("Natri clorid 3%"), "8930000002008");
        private final SaleProduct prescriptionOnly = new SaleProduct(UUID.randomUUID(), "SP03", "Test Rx product",
                "box", true, BigDecimal.valueOf(30000), BigDecimal.TEN, List.of(), "8930000003007");
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
            return List.of(new ModuleRow(customerId,
                    List.of("Customer", "0910000001", "", "", Integer.toString(customerPoints), "")),
                    customerWithoutPoints);
        }

        @Override
        public List<SaleProduct> findSaleProducts() {
            return List.of(product, otherProduct, prescriptionOnly);
        }

        @Override
        public SaleStockPreview previewSalePicking(List<SaleLineRequest> lines) {
            assertEquals(List.of(product.id()), lines.stream().map(SaleLineRequest::productId).toList());
            var pick = new SaleStockPreview.Pick(1, product.name(), product.unit(), "FIXTURE-LOT",
                    "KE-A1", "Front shelf", LocalDate.of(2030, 1, 1), lines.getFirst().quantity());
            return new SaleStockPreview(List.of(pick), List.of());
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
            return request.lines().stream()
                    .map(line -> findSaleProducts().stream()
                            .filter(candidate -> candidate.id().equals(line.productId())).findFirst().orElseThrow()
                            .sellingPrice().multiply(line.quantity()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
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
