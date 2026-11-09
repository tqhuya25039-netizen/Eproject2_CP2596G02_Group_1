package vn.ninhkieupharma.pharmacy.view;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.Chart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import vn.ninhkieupharma.pharmacy.model.ModuleDefinition;
import vn.ninhkieupharma.pharmacy.service.DemoData;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PageRenderer {
    private record RenderedTable(
            TableView<List<String>> table,
            FilteredList<List<String>> filteredRows) {
    }

    private record ProductCard(DemoData.Product product, Node node, Button addButton) {
    }

    public Node render(ModuleDefinition module) {
        VBox page = new VBox(22);
        page.getStyleClass().add("page");
        page.setPadding(new Insets(28, 36, 36, 36));

        Label title = new Label(module.title());
        title.getStyleClass().add("page-title");
        Label description = new Label(module.description());
        description.getStyleClass().add("page-description");
        description.setWrapText(true);

        TextField search = new TextField();
        search.setPromptText("Tìm theo mã, tên hoặc nội dung...");
        search.getStyleClass().add("search-input");
        HBox.setHgrow(search, Priority.ALWAYS);

        RenderedTable renderedTable = "UC-17".equals(module.id())
                ? null : createTable(module, search);
        List<Node> filterInputs = new ArrayList<>();
        HBox filters = module.report()
                ? createFilters(module, filterInputs, renderedTable.filteredRows(), search)
                : null;
        if (renderedTable != null) {
            search.textProperty().addListener((observable, oldValue, newValue) ->
                    applyFilters(renderedTable.filteredRows(), search, filterInputs));
        }

        HBox actions = new HBox(12);
        actions.setAlignment(Pos.CENTER_LEFT);
        Button primaryAction = new Button(module.actionLabel());
        primaryAction.getStyleClass().add("button-primary");
        primaryAction.setOnAction(event -> {
            if (module.report()) {
                applyFilters(renderedTable.filteredRows(), search, filterInputs);
            } else {
                showForm(module);
            }
        });
        actions.getChildren().add(search);
        if (module.report()) {
            Button exportButton = new Button("Xuất báo cáo");
            exportButton.getStyleClass().add("button-secondary");
            exportButton.setOnAction(event -> showInformation(
                    "Báo cáo dữ liệu mẫu",
                    "Bản demo chưa xuất tệp. Dữ liệu đang hiển thị chỉ dùng để xem giao diện."));
            actions.getChildren().add(exportButton);
        }
        actions.getChildren().add(primaryAction);

        if (module.report()) {
            page.getChildren().addAll(title, description, createReportSummary(module), filters,
                    actions, createReportCharts(module), renderedTable.table());
        } else if ("UC-17".equals(module.id())) {
            page.getChildren().addAll(title, description, createSalesWorkspace());
        } else {
            page.getChildren().addAll(title, description, actions, renderedTable.table());
        }
        return page;
    }

    private HBox createFilters(
            ModuleDefinition module,
            List<Node> filterInputs,
            FilteredList<List<String>> rows,
            TextField search) {
        HBox filters = new HBox(12);
        filters.setAlignment(Pos.CENTER_LEFT);
        filters.getStyleClass().add("filter-bar");
        for (int index = 0; index < Math.min(module.formFields().size(), 2); index++) {
            TextField filter = new TextField();
            filter.setPromptText(module.formFields().get(index));
            filter.getStyleClass().add("filter-input");
            HBox.setHgrow(filter, Priority.ALWAYS);
            filterInputs.add(filter);
            filters.getChildren().add(filter);
        }
        Button applyButton = new Button("Áp dụng");
        applyButton.getStyleClass().add("button-secondary");
        applyButton.setOnAction(event -> applyFilters(rows, search, filterInputs));
        filters.getChildren().add(applyButton);
        return filters;
    }

    private RenderedTable createTable(ModuleDefinition module, TextField search) {
        TableView<List<String>> table = new TableView<>();
        table.getStyleClass().add("data-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        for (int index = 0; index < module.columns().size(); index++) {
            final int columnIndex = index;
            TableColumn<List<String>, String> column = new TableColumn<>(module.columns().get(index));
            column.setCellValueFactory(cell ->
                    new javafx.beans.property.ReadOnlyStringWrapper(cell.getValue().get(columnIndex)));
            table.getColumns().add(column);
        }

        ObservableList<List<String>> source =
                FXCollections.observableArrayList(DemoData.rowsFor(module.id()));
        FilteredList<List<String>> filteredRows = new FilteredList<>(source, row -> true);
        table.setItems(filteredRows);
        Label emptyState = new Label("Không tìm thấy dữ liệu phù hợp.");
        emptyState.getStyleClass().add("table-empty-state");
        emptyState.setWrapText(true);
        table.setPlaceholder(emptyState);
        VBox.setVgrow(table, Priority.ALWAYS);

        RenderedTable result = new RenderedTable(table, filteredRows);
        return result;
    }

    private Node createReportSummary(ModuleDefinition module) {
        HBox summary = new HBox(12);
        summary.getStyleClass().add("demo-summary");
        summary.getChildren().add(summaryCard("BẢN GHI", DemoData.rowsFor(module.id()).size() + " dòng"));
        summary.getChildren().add(summaryCard("PHẠM VI", module.id()));
        summary.getChildren().add(summaryCard("TRẠNG THÁI", "Dữ liệu minh họa"));
        return summary;
    }

    private Node createReportCharts(ModuleDefinition module) {
        FlowPane charts = new FlowPane(16, 16);
        charts.getStyleClass().add("report-chart-row");
        switch (module.id()) {
            case "UC-05" -> {
                charts.getChildren().add(chartCard("Giờ làm theo nhân viên", barChart(
                        "Nhân viên", "Số giờ",
                        List.of("Quốc Bảo", "Thu Hà", "Gia Huy", "Ngọc Lan"),
                        series("Kế hoạch", 110, 100, 90, 105,
                                "Thực tế", 108.5, 97, 86, 101))));
                charts.getChildren().add(chartCard("Phân bổ số ca", pieChart(
                        "Ca làm", "Ca sáng", 32, "Ca chiều", 28, "Ca tối", 21)));
            }
            case "UC-06" -> {
                charts.getChildren().add(chartCard("Tỷ lệ đúng giờ theo ngày", lineChart(
                        "Ngày", "Đúng giờ (%)",
                        List.of("26/10", "27/10", "28/10", "29/10", "30/10", "31/10", "01/11"),
                        series("Tỷ lệ đúng giờ", 96, 92, 100, 95, 98, 90, 97))));
                charts.getChildren().add(chartCard("Phút đi trễ theo nhân viên", barChart(
                        "Nhân viên", "Phút",
                        List.of("Quốc Bảo", "Thu Hà", "Gia Huy", "Ngọc Lan"),
                        series("Đi trễ", 0, 8, 0, 0))));
            }
            case "UC-10" -> {
                charts.getChildren().add(chartCard("Số sản phẩm mỗi danh mục", barChart(
                        "Danh mục", "Số sản phẩm",
                        List.of("Giảm đau", "Kháng sinh", "Vitamin", "Bổ sung", "Vật tư"),
                        series("Sản phẩm", 12, 10, 9, 6, 3))));
                charts.getChildren().add(chartCard("Tỷ trọng danh mục", pieChart(
                        "Danh mục", "Thuốc giảm đau", 12, "Thuốc kháng sinh", 10,
                        "Vitamin & khoáng chất", 9, "Thực phẩm bổ sung", 6, "Vật tư y tế", 3)));
            }
            case "UC-11" -> {
                charts.getChildren().add(chartCard("Sản phẩm theo nhà cung cấp", barChart(
                        "Nhà cung cấp", "Số mặt hàng",
                        List.of("Hậu Giang", "Mekong", "An Khang", "Tây Đô"),
                        series("Mặt hàng", 18, 11, 8, 3))));
                charts.getChildren().add(chartCard("Tỷ trọng nhà cung cấp", pieChart(
                        "Nhà cung cấp", "Dược phẩm Hậu Giang", 18,
                        "Thiết bị y tế Mekong", 11, "Phân phối An Khang", 8,
                        "Dược liệu Tây Đô", 3)));
            }
            case "UC-15" -> {
                charts.getChildren().add(chartCard("Biến động tồn kho theo ngày", areaChart(
                        "Ngày", "Thay đổi tồn (đơn vị)",
                        List.of("26/10", "27/10", "28/10", "29/10", "30/10", "31/10", "01/11"),
                        series("Biến động ròng", 18, -6, 24, -3, 11, -2, 8))));
                charts.getChildren().add(chartCard("Tỷ trọng loại biến động", pieChart(
                        "Loại biến động", "Nhập hàng", 12, "Bán hàng", 35,
                        "Điều chỉnh", 4, "Hư hỏng", 2, "Hết hạn", 1)));
            }
            case "UC-19" -> {
                List<String> dates = List.of("28/10", "29/10", "30/10", "31/10", "01/11");
                charts.getChildren().add(chartCard("Số lượng hóa đơn theo ngày", barChart(
                        "Ngày", "Số đơn", dates, series("Hóa đơn", 39, 46, 37, 58, 42))));
                charts.getChildren().add(chartCard("Doanh thu theo ngày", lineChart(
                        "Ngày", "Doanh thu (triệu ₫)", dates,
                        series("Doanh thu", 4.21, 5.18, 3.92, 6.24, 4.86))));
            }
            default -> throw new IllegalArgumentException(
                    "Chưa cấu hình biểu đồ cho báo cáo " + module.id());
        }
        return charts;
    }

    private VBox chartCard(String title, Chart chart) {
        VBox card = new VBox(10);
        card.getStyleClass().add("chart-card");
        Label heading = new Label(title);
        heading.getStyleClass().add("section-title");
        chart.setAnimated(false);
        chart.setTitle(null);
        chart.setMinHeight(280);
        chart.setPrefHeight(310);
        chart.setMaxWidth(Double.MAX_VALUE);
        chart.getStyleClass().add("report-chart");
        card.setPrefWidth(510);
        card.setMaxWidth(Double.MAX_VALUE);
        card.getChildren().addAll(heading, chart);
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    private BarChart<String, Number> barChart(
            String categoryLabel, String valueLabel, List<String> categories,
            Map<String, List<Number>> values) {
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel(categoryLabel);
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel(valueLabel);
        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setCategoryGap(18);
        chart.setBarGap(5);
        addSeries(chart, categories, values);
        return chart;
    }

    private LineChart<String, Number> lineChart(
            String categoryLabel, String valueLabel, List<String> categories,
            Map<String, List<Number>> values) {
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel(categoryLabel);
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel(valueLabel);
        LineChart<String, Number> chart = new LineChart<>(xAxis, yAxis);
        addSeries(chart, categories, values);
        return chart;
    }

    private AreaChart<String, Number> areaChart(
            String categoryLabel, String valueLabel, List<String> categories,
            Map<String, List<Number>> values) {
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel(categoryLabel);
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel(valueLabel);
        AreaChart<String, Number> chart = new AreaChart<>(xAxis, yAxis);
        addSeries(chart, categories, values);
        return chart;
    }

    private PieChart pieChart(String nameLabel, Object... nameValuePairs) {
        PieChart chart = new PieChart();
        chart.setLabelsVisible(false);
        chart.setLegendVisible(true);
        for (int index = 0; index < nameValuePairs.length; index += 2) {
            chart.getData().add(new PieChart.Data(
                    (String) nameValuePairs[index], ((Number) nameValuePairs[index + 1]).doubleValue()));
        }
        chart.setAccessibleText(nameLabel);
        return chart;
    }

    private void addSeries(
            XYChart<String, Number> chart, List<String> categories, Map<String, List<Number>> values) {
        for (Map.Entry<String, List<Number>> entry : values.entrySet()) {
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName(entry.getKey());
            for (int index = 0; index < categories.size(); index++) {
                series.getData().add(new XYChart.Data<>(categories.get(index), entry.getValue().get(index)));
            }
            chart.getData().add(series);
        }
    }

    private Map<String, List<Number>> series(Object... nameValues) {
        Map<String, List<Number>> result = new LinkedHashMap<>();
        int index = 0;
        while (index < nameValues.length) {
            String name = (String) nameValues[index++];
            List<Number> values = new ArrayList<>();
            while (index < nameValues.length && nameValues[index] instanceof Number value) {
                values.add(value);
                index++;
            }
            result.put(name, List.copyOf(values));
        }
        return result;
    }

    private VBox summaryCard(String heading, String value) {
        VBox card = new VBox(7);
        card.getStyleClass().add("summary-card");
        Label title = new Label(heading);
        title.getStyleClass().add("overview-label");
        Label content = new Label(value);
        content.getStyleClass().add("overview-value");
        card.getChildren().addAll(title, content);
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    private void applyFilters(
            FilteredList<List<String>> rows, TextField search, List<Node> filterInputs) {
        String query = search.getText() == null
                ? "" : search.getText().trim().toLowerCase(Locale.ROOT);
        List<String> criteria = filterInputs.stream()
                .map(this::inputValue)
                .filter(value -> !value.isBlank())
                .map(value -> value.toLowerCase(Locale.ROOT))
                .toList();
        rows.setPredicate(row -> {
            String searchableRow = String.join(" ", row).toLowerCase(Locale.ROOT);
            return (query.isBlank() || searchableRow.contains(query))
                    && criteria.stream().allMatch(searchableRow::contains);
        });
    }

    private String inputValue(Node node) {
        if (node instanceof TextInputControl textInput) {
            return textInput.getText().trim();
        }
        return "";
    }

    private Node createSalesWorkspace() {
        HBox workspace = new HBox(18);
        VBox catalog = new VBox(12);
        catalog.getStyleClass().add("dashboard-panel");
        HBox.setHgrow(catalog, Priority.ALWAYS);
        Label catalogTitle = new Label("Sản phẩm mẫu · " + DemoData.products().size() + " mặt hàng");
        catalogTitle.getStyleClass().add("section-title");
        Label inventoryNote = new Label("Tồn kho và giá bán chỉ mang tính minh họa.");
        inventoryNote.getStyleClass().add("helper-text");
        TextField productSearch = new TextField();
        productSearch.setPromptText("Tìm tên sản phẩm hoặc mã hàng...");
        productSearch.getStyleClass().add("search-input");

        FlowPane productGrid = new FlowPane(12, 12);
        productGrid.getStyleClass().add("product-grid");
        List<ProductCard> products = DemoData.products().stream()
                .map(this::productCard)
                .toList();
        productGrid.getChildren().setAll(products.stream().map(ProductCard::node).toList());
        productSearch.textProperty().addListener((observable, oldValue, query) -> {
            String normalizedQuery = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
            List<Node> matches = products.stream()
                    .filter(card -> (card.product().name() + " " + card.product().code())
                            .toLowerCase(Locale.ROOT).contains(normalizedQuery))
                    .map(ProductCard::node)
                    .toList();
            productGrid.getChildren().setAll(matches);
        });
        catalog.getChildren().addAll(catalogTitle, inventoryNote, productSearch, productGrid);

        VBox invoice = new VBox(12);
        invoice.setPrefWidth(340);
        invoice.getStyleClass().add("invoice-panel");
        Label invoiceTitle = new Label("Hóa đơn mẫu");
        invoiceTitle.getStyleClass().add("section-title");
        ComboBox<String> customer = new ComboBox<>();
        customer.getItems().addAll(
                "Khách lẻ", "Nguyễn Thị Mai · KH0001", "Trần Văn Nam · KH0002");
        customer.getSelectionModel().select(1);
        customer.setMaxWidth(Double.MAX_VALUE);
        customer.getStyleClass().add("text-input");

        ObservableList<DemoData.Product> cart = FXCollections.observableArrayList(
                DemoData.products().get(0), DemoData.products().get(1));
        VBox cartItems = new VBox(8);
        VBox.setVgrow(cartItems, Priority.ALWAYS);
        Label loyalty = new Label("Điểm tích lũy mẫu: 1.250 điểm");
        loyalty.getStyleClass().add("helper-text");
        Label total = new Label();
        total.getStyleClass().add("invoice-total");
        refreshCart(cart, cartItems, total);

        for (ProductCard card : products) {
            card.addButton().setOnAction(event -> {
                if (card.product().stock() > 0) {
                    cart.add(card.product());
                    refreshCart(cart, cartItems, total);
                } else {
                    showInformation("Hết hàng mẫu", "Sản phẩm minh họa này hiện không còn tồn kho.");
                }
            });
        }

        Button clear = new Button("Xóa giỏ");
        clear.getStyleClass().add("button-secondary");
        clear.setMaxWidth(Double.MAX_VALUE);
        clear.setOnAction(event -> {
            cart.clear();
            refreshCart(cart, cartItems, total);
        });
        Button checkout = new Button("Hoàn tất hóa đơn");
        checkout.getStyleClass().add("button-primary");
        checkout.setMaxWidth(Double.MAX_VALUE);
        checkout.setOnAction(event -> showInformation(
                "Chế độ xem dữ liệu mẫu",
                "Đây là hóa đơn minh họa. Không có giao dịch thật nào được tạo hoặc lưu."));
        invoice.getChildren().addAll(invoiceTitle, customer, cartItems, loyalty, total, clear, checkout);
        workspace.getChildren().addAll(catalog, invoice);
        VBox.setVgrow(workspace, Priority.ALWAYS);
        return workspace;
    }

    private void refreshCart(
            ObservableList<DemoData.Product> cart,
            VBox cartItems,
            Label total) {
        cartItems.getChildren().clear();
        for (DemoData.Product product : cart) {
            HBox item = new HBox(8);
            item.setAlignment(Pos.CENTER_LEFT);
            item.getStyleClass().add("cart-item");
            VBox details = new VBox(4);
            Label name = new Label(product.name());
            name.getStyleClass().add("cart-item-name");
            Label price = new Label(formatCurrency(product.price()));
            price.getStyleClass().add("helper-text");
            details.getChildren().addAll(name, price);
            HBox.setHgrow(details, Priority.ALWAYS);
            Button remove = new Button("−");
            remove.getStyleClass().add("button-secondary");
            remove.setOnAction(event -> {
                cart.remove(product);
                refreshCart(cart, cartItems, total);
            });
            item.getChildren().addAll(details, remove);
            cartItems.getChildren().add(item);
        }
        long subtotal = cart.stream().mapToLong(DemoData.Product::price).sum();
        total.setText("Tổng thanh toán     " + formatCurrency(subtotal));
    }

    private ProductCard productCard(DemoData.Product product) {
        VBox card = new VBox(8);
        card.setPrefWidth(190);
        card.getStyleClass().add("product-card");
        Label category = new Label(product.category());
        category.getStyleClass().add("overview-label");
        Label name = new Label(product.name());
        name.setWrapText(true);
        name.getStyleClass().add("cart-item-name");
        Label code = new Label(product.code() + " · " + product.unit());
        code.getStyleClass().add("helper-text");
        Label price = new Label(formatCurrency(product.price()));
        price.getStyleClass().add("overview-value");
        Label stock = new Label("Tồn mẫu: " + product.stock());
        stock.getStyleClass().add("helper-text");
        Button add = new Button("Thêm vào hóa đơn");
        add.getStyleClass().add("button-secondary");
        add.setMaxWidth(Double.MAX_VALUE);
        card.getChildren().addAll(category, name, code, price, stock, add);
        return new ProductCard(product, card, add);
    }

    private String formatCurrency(long amount) {
        return NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN")).format(amount) + " ₫";
    }

    private void showForm(ModuleDefinition module) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(module.actionLabel());
        dialog.setHeaderText(module.id() + "  ·  " + module.title());
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);
        dialog.getDialogPane().getStyleClass().add("form-dialog");

        GridPane fields = new GridPane();
        fields.setHgap(16);
        fields.setVgap(12);
        fields.setPadding(new Insets(12, 0, 8, 0));
        int row = 0;
        for (String fieldName : module.formFields()) {
            Label label = new Label(fieldName);
            label.getStyleClass().add("field-label");
            Node input = createInput(fieldName);
            fields.add(label, 0, row);
            fields.add(input, 1, row++);
            GridPane.setHgrow(input, Priority.ALWAYS);
        }
        VBox content = new VBox(12, fields);
        Label notice = new Label("Dữ liệu đang nhập chỉ dùng để xem giao diện; thao tác này không lưu.");
        notice.setWrapText(true);
        notice.getStyleClass().add("helper-text");
        content.getChildren().add(notice);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setPrefWidth(560);
        dialog.setResultConverter(button -> button == ButtonType.OK ? ButtonType.OK : null);
        dialog.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                showInformation("Bản demo", "Thao tác chỉ minh họa biểu mẫu, chưa ghi dữ liệu vào hệ thống.");
            }
        });
    }

    private Node createInput(String fieldName) {
        if (fieldName.toLowerCase(Locale.ROOT).contains("ngày")) {
            DatePicker datePicker = new DatePicker();
            datePicker.setPromptText(fieldName);
            datePicker.getStyleClass().add("text-input");
            return datePicker;
        }
        if (fieldName.toLowerCase(Locale.ROOT).contains("trạng thái")
                || fieldName.toLowerCase(Locale.ROOT).contains("vai trò")
                || fieldName.toLowerCase(Locale.ROOT).contains("danh mục")) {
            ComboBox<String> comboBox = new ComboBox<>();
            comboBox.setPromptText("Chọn " + fieldName.toLowerCase(Locale.ROOT));
            comboBox.setMaxWidth(Double.MAX_VALUE);
            comboBox.getStyleClass().add("text-input");
            if (fieldName.toLowerCase(Locale.ROOT).contains("vai trò")) {
                comboBox.getItems().addAll("Quản trị viên", "Nhân viên");
            } else if (fieldName.toLowerCase(Locale.ROOT).contains("trạng thái")) {
                comboBox.getItems().addAll("Đang hoạt động", "Ngừng hoạt động");
            }
            return comboBox;
        }
        TextInputControl input = fieldName.toLowerCase(Locale.ROOT).contains("ghi chú")
                || fieldName.toLowerCase(Locale.ROOT).contains("dị ứng")
                || fieldName.toLowerCase(Locale.ROOT).contains("địa chỉ")
                ? new javafx.scene.control.TextArea()
                : new TextField();
        input.setPromptText("Nhập " + fieldName.toLowerCase(Locale.ROOT));
        input.getStyleClass().add("text-input");
        if (input instanceof javafx.scene.control.TextArea textArea) {
            textArea.setPrefRowCount(2);
        }
        return input;
    }

    private void showInformation(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
