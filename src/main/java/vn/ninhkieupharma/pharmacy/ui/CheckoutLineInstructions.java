package vn.ninhkieupharma.pharmacy.ui;

import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import vn.ninhkieupharma.pharmacy.common.service.Localization;
import vn.ninhkieupharma.pharmacy.domain.sales.DoseInstructions;
import vn.ninhkieupharma.pharmacy.domain.sales.DoseSession;
import vn.ninhkieupharma.pharmacy.domain.sales.DoseTiming;
import vn.ninhkieupharma.pharmacy.domain.sales.DoseUnit;
import vn.ninhkieupharma.pharmacy.domain.sales.SaleLineKind;
import vn.ninhkieupharma.pharmacy.domain.sales.SaleLineRequest;
import vn.ninhkieupharma.pharmacy.domain.sales.SaleProduct;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

final class CheckoutLineInstructions extends VBox {
    private final RadioButton prescribed;
    private final TextField doseAmount = new TextField();
    private final ComboBox<DoseOption<DoseUnit>> doseUnit;
    private final ComboBox<DoseOption<DoseTiming>> doseTiming;
    private final Map<DoseSession, CheckBox> sessions = new EnumMap<>(DoseSession.class);
    private final TextField notes = new TextField();

    CheckoutLineInstructions(SaleProduct product, SaleLineRequest request, Consumer<SaleLineKind> onKindChanged) {
        super(8);
        getStyleClass().add("checkout-line-instructions");
        boolean requiresPrescription = product != null && product.prescriptionRequired();
        ToggleGroup classification = new ToggleGroup();
        prescribed = kindButton(SaleLineKind.PRESCRIBED, "checkout-line-prescribed", classification);
        RadioButton addOn = kindButton(SaleLineKind.ADD_ON, "checkout-line-add-on", classification);
        addOn.setDisable(requiresPrescription);
        classification.selectToggle(requiresPrescription || request.kind() == SaleLineKind.PRESCRIBED
                ? prescribed : addOn);
        classification.selectedToggleProperty().addListener((observable, previous, selected) ->
                onKindChanged.accept(kind()));
        FlowPane kinds = new FlowPane(12, 6, fieldLabel("Loại thuốc"), prescribed, addOn);
        kinds.getStyleClass().add("checkout-line-kind");
        kinds.setAlignment(Pos.CENTER_LEFT);

        DoseInstructions dose = request.dose();
        doseAmount.setId("checkout-dose-amount");
        doseAmount.setPrefWidth(106);
        if (dose.amount() != null) {
            doseAmount.setText(dose.amount().stripTrailingZeros().toPlainString());
        }
        doseUnit = doseOptions(DoseUnit.values(), dose.unit(), DoseUnit::label, "checkout-dose-unit");
        doseUnit.setPrefWidth(128);
        doseTiming = doseOptions(DoseTiming.values(), dose.timing(), DoseTiming::label, "checkout-dose-timing");
        doseTiming.setPrefWidth(156);
        FlowPane doseFields = new FlowPane(10, 8,
                field("Liều mỗi lần", doseAmount), field("Đơn vị liều", doseUnit), field("Cách dùng", doseTiming));

        FlowPane doseSessions = new FlowPane(12, 6, fieldLabel("Buổi dùng"));
        doseSessions.getStyleClass().add("checkout-dose-sessions");
        doseSessions.setAlignment(Pos.CENTER_LEFT);
        for (DoseSession session : DoseSession.values()) {
            CheckBox choice = new CheckBox(Localization.text(session.label()));
            choice.setId("checkout-dose-" + session.name().toLowerCase(Locale.ROOT));
            choice.setSelected(dose.sessions().contains(session));
            sessions.put(session, choice);
            doseSessions.getChildren().add(choice);
        }

        notes.setId("checkout-dose-notes");
        if (request.dosageInstructions() != null) {
            notes.setText(request.dosageInstructions());
        }
        getChildren().addAll(kinds, doseFields, doseSessions, field("Hướng dẫn thêm", notes));
    }

    SaleLineKind kind() {
        return prescribed.isSelected() ? SaleLineKind.PRESCRIBED : SaleLineKind.ADD_ON;
    }

    boolean hasInstructions() {
        return !doseAmount.getText().isBlank() || doseUnit.getValue().value() != null
                || doseTiming.getValue().value() != null || sessions.values().stream().anyMatch(CheckBox::isSelected)
                || !notes.getText().isBlank();
    }

    SaleLineRequest buildRequest(UUID productId, BigDecimal quantity, int treatmentDays) {
        BigDecimal amount = parseDoseAmount(doseAmount.getText());
        DoseUnit unit = doseUnit.getValue().value();
        if ((amount == null) != (unit == null)) {
            throw new IllegalArgumentException(Localization.text("Hãy nhập liều mỗi lần và chọn đơn vị liều."));
        }
        EnumSet<DoseSession> selectedSessions = EnumSet.noneOf(DoseSession.class);
        sessions.forEach((session, choice) -> {
            if (choice.isSelected()) {
                selectedSessions.add(session);
            }
        });
        DoseInstructions dose = new DoseInstructions(amount, unit, doseTiming.getValue().value(), selectedSessions);
        if (!dose.hasValidAmount()) {
            throw new IllegalArgumentException(Localization.text(DoseInstructions.AMOUNT_ERROR));
        }
        return new SaleLineRequest(productId, quantity, notes.getText().trim(), treatmentDays, kind(), dose);
    }

    private static BigDecimal parseDoseAmount(String input) {
        String value = input.trim().replace(',', '.');
        if (value.isEmpty()) {
            return null;
        }
        if (value.matches("(?:[0-9]{1,6}(?:\\.[0-9]{1,3})?|\\.[0-9]{1,3})")) {
            return new BigDecimal(value);
        }
        throw new IllegalArgumentException(Localization.text(DoseInstructions.AMOUNT_ERROR));
    }

    private static RadioButton kindButton(SaleLineKind kind, String id, ToggleGroup classification) {
        RadioButton button = new RadioButton(Localization.text(kind.label()));
        button.setId(id);
        button.setToggleGroup(classification);
        return button;
    }

    private static <T extends Enum<T>> ComboBox<DoseOption<T>> doseOptions(
            T[] values, T initialValue, Function<T, String> localizationKey, String id) {
        ComboBox<DoseOption<T>> comboBox = new ComboBox<>();
        comboBox.setId(id);
        comboBox.getItems().add(new DoseOption<>(null, Localization.text("Chưa chọn")));
        for (T value : values) {
            comboBox.getItems().add(new DoseOption<>(value, Localization.text(localizationKey.apply(value))));
        }
        comboBox.getSelectionModel().select(initialValue == null ? 0 : initialValue.ordinal() + 1);
        return comboBox;
    }

    private static VBox field(String title, Control control) {
        Label label = fieldLabel(title);
        label.setLabelFor(control);
        return new VBox(4, label, control);
    }

    private static Label fieldLabel(String title) {
        Label label = new Label(Localization.text(title));
        label.getStyleClass().add("field-label");
        return label;
    }

    private record DoseOption<T>(T value, String label) {
        @Override
        public String toString() {
            return label;
        }
    }
}
