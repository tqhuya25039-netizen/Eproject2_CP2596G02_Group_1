package vn.ninhkieupharma.pharmacy.ui;

import javafx.beans.binding.Bindings;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import vn.ninhkieupharma.pharmacy.common.service.ExchangeRateService;
import vn.ninhkieupharma.pharmacy.common.service.Localization;
import vn.ninhkieupharma.pharmacy.domain.sales.SaleProduct;
import vn.ninhkieupharma.pharmacy.domain.sales.SaleProductSearch;

import java.util.List;
import java.util.function.Consumer;

final class CheckoutProductPicker extends VBox {
    private static final int VISIBLE_RESULTS = 4;
    private static final int RESULT_HEIGHT = 54;
    private static final int RESULTS_INSETS = 8;

    private final TextField search = new TextField();
    private final ListView<SaleProduct> results = new ListView<>();
    private final Label feedback = new Label();
    private final Label emptyMessage = new Label();
    private final ObservableList<SaleProduct> products;
    private SaleProductSearch catalog;
    private final ExchangeRateService exchangeRateService;
    private final Consumer<SaleProduct> onAdd;

    CheckoutProductPicker(ObservableList<SaleProduct> products, ExchangeRateService exchangeRateService,
                          Consumer<SaleProduct> onAdd) {
        super(8);
        this.products = products;
        catalog = new SaleProductSearch(products);
        this.exchangeRateService = exchangeRateService;
        this.onAdd = onAdd;

        search.setId("checkout-product-search");
        search.setPromptText(Localization.text("Tên thuốc, mã hàng, mã vạch hoặc hoạt chất"));
        search.setAccessibleText(Localization.text("Tìm thuốc"));
        search.getStyleClass().add("search-input");
        HBox.setHgrow(search, Priority.ALWAYS);

        Button add = new Button(Localization.text("Thêm vào giỏ"));
        add.getStyleClass().add("button-primary");
        add.disableProperty().bind(Bindings.createBooleanBinding(
                () -> !hasPrice(results.getSelectionModel().getSelectedItem()),
                results.getSelectionModel().selectedItemProperty()));
        add.setOnAction(event -> addSelected());

        HBox toolbar = new HBox(8, search, add);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.getStyleClass().add("checkout-search-bar");

        results.setId("checkout-product-results");
        results.setAccessibleText(Localization.text("Kết quả tìm thuốc"));
        results.getStyleClass().add("checkout-product-results");
        results.setFixedCellSize(RESULT_HEIGHT);
        results.setMinHeight(RESULT_HEIGHT + RESULTS_INSETS);
        results.managedProperty().bind(results.visibleProperty());
        results.setPlaceholder(emptyMessage);
        results.setCellFactory(list -> resultCell());

        feedback.setWrapText(true);
        feedback.getStyleClass().add("helper-text");
        feedback.managedProperty().bind(feedback.textProperty().isNotEmpty());
        feedback.visibleProperty().bind(feedback.managedProperty());
        getChildren().addAll(toolbar, results, feedback);

        search.textProperty().addListener((observable, previous, query) -> updateResults(query));
        products.addListener((ListChangeListener<SaleProduct>) change -> {
            catalog = new SaleProductSearch(products);
            updateResults(search.getText());
        });
        search.setOnAction(event -> addSelected());
        search.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.DOWN || event.getCode() == KeyCode.UP) {
                moveSelection(event.getCode() == KeyCode.DOWN ? 1 : -1);
                event.consume();
            } else if (event.getCode() == KeyCode.ESCAPE && !search.getText().isEmpty()) {
                clear();
                event.consume();
            }
        });
        results.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                addSelected();
                event.consume();
            }
        });
        updateResults("");
    }

    void clear() {
        search.clear();
        results.getSelectionModel().clearSelection();
        feedback.setText("");
    }

    void refreshCurrency() {
        results.refresh();
    }

    private void updateResults(String query) {
        boolean browsing = query.isBlank();
        boolean hasProducts = !products.isEmpty();
        List<SaleProduct> matches = browsing ? List.of() : catalog.find(query);
        results.getItems().setAll(matches);
        results.getSelectionModel().clearSelection();
        if (!matches.isEmpty()) {
            results.scrollTo(0);
            SaleProduct first = matches.getFirst();
            String identifier = query.strip();
            if (matches.size() == 1 || first.barcode().equalsIgnoreCase(identifier)
                    || first.code().equalsIgnoreCase(identifier)) {
                results.getSelectionModel().selectFirst();
            }
        }
        results.setPrefHeight(Math.clamp(matches.size(), 1, VISIBLE_RESULTS) * RESULT_HEIGHT + RESULTS_INSETS);
        results.setVisible(!browsing || !hasProducts);
        emptyMessage.setText(Localization.text(hasProducts
                ? "Không tìm thấy thuốc phù hợp." : "Chưa có sản phẩm để bán."));
        feedback.setText("");
    }

    private void moveSelection(int direction) {
        if (results.getItems().isEmpty()) {
            return;
        }
        int index = results.getSelectionModel().getSelectedIndex();
        int target = index < 0 ? 0 : Math.clamp(index + direction, 0, results.getItems().size() - 1);
        results.getSelectionModel().select(target);
        results.scrollTo(target);
    }

    private void addSelected() {
        SaleProduct product = results.getSelectionModel().getSelectedItem();
        if (product == null) {
            return;
        }
        if (!hasPrice(product)) {
            feedback.setText(Localization.text("Sản phẩm chưa có giá bán hợp lệ. Hãy cập nhật giá trong quản lý sản phẩm."));
            return;
        }
        onAdd.accept(product);
        clear();
        feedback.setText(Localization.text("Đã thêm vào giỏ: ") + product.name());
        search.requestFocus();
    }

    static boolean hasPrice(SaleProduct product) {
        return product != null && product.sellingPrice() != null && product.sellingPrice().signum() > 0;
    }

    private ListCell<SaleProduct> resultCell() {
        return new ListCell<>() {
            private final Label name = new Label();
            private final Label meta = new Label();
            private final VBox item = new VBox(3, name, meta);

            {
                item.getStyleClass().add("checkout-product-result");
                name.getStyleClass().add("checkout-product-name");
                meta.getStyleClass().add("checkout-product-meta");
                setOnMouseClicked(event -> {
                    if (!isEmpty() && event.getButton() == javafx.scene.input.MouseButton.PRIMARY
                            && event.getClickCount() == 2) {
                        addSelected();
                    }
                });
            }

            @Override
            protected void updateItem(SaleProduct product, boolean empty) {
                super.updateItem(product, empty);
                if (empty || product == null) {
                    setGraphic(null);
                    return;
                }
                name.setText(product.name());
                String availability = product.stockOnHand().signum() <= 0
                        ? Localization.text("Hết tồn khả dụng")
                        : Localization.text("Tồn: ") + product.stockOnHand().stripTrailingZeros().toPlainString();
                String price = hasPrice(product)
                        ? SalesMoney.capture(exchangeRateService).format(product.sellingPrice(), 2)
                        : Localization.text("Chưa đặt giá");
                meta.setText(product.code() + " · " + price + " / " + Localization.saleUnitText(product.unit())
                        + " · " + availability + (product.prescriptionRequired()
                        ? " · " + Localization.text("Cần toa thuốc") : ""));
                meta.getStyleClass().remove("checkout-product-unavailable");
                if (!hasPrice(product) || product.stockOnHand().signum() <= 0) {
                    meta.getStyleClass().add("checkout-product-unavailable");
                }
                setGraphic(item);
            }
        };
    }
}
