package vn.ninhkieupharma.pharmacy.features.common.view;

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
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import vn.ninhkieupharma.pharmacy.features.common.model.ModuleRow;
import vn.ninhkieupharma.pharmacy.features.common.service.FeatureModuleService;
import vn.ninhkieupharma.pharmacy.features.common.service.FeatureServices;
import vn.ninhkieupharma.pharmacy.features.common.model.ModuleDefinition;
import vn.ninhkieupharma.pharmacy.features.common.service.DemoData;
import vn.ninhkieupharma.pharmacy.features.common.service.Localization;
import vn.ninhkieupharma.pharmacy.features.common.service.ModuleCatalog;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class PageRenderer {
    private record RenderedTable(
            TableView<ModuleRow> table,
            ObservableList<ModuleRow> source,
            FilteredList<ModuleRow> filteredRows) {
    }

    private record ProductCard(DemoData.Product product, Node node, Button addButton) {
    }

    private final FeatureServices featureServices;

    public PageRenderer(FeatureServices featureServices) {
        this.featureServices = Objects.requireNonNull(featureServices);
    }

    public Node render(ModuleDefinition module) {
        FeatureModuleService featureService = featureServices.forModule(module);
        VBox page = new VBox(22);
        page.getStyleClass().add("page");
        page.setPadding(new Insets(28, 36, 36, 36));

        Label title = new Label(module.title());
        title.getStyleClass().add("page-title");
        Label description = new Label(module.description());
        description.getStyleClass().add("page-description");
        description.setWrapText(true);

        TextField search = new TextField();
        search.setPromptText(Localization.text("Tìm theo mã, tên hoặc nội dung..."));
        search.getStyleClass().add("search-input");
        HBox.setHgrow(search, Priority.ALWAYS);

        RenderedTable renderedTable = createTable(module, search, featureService);
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
        actions.getChildren().add(search);
        if (module.report()) {
            Button exportButton = new Button(Localization.text("Xuất báo cáo"));
            exportButton.getStyleClass().add("button-secondary");
            exportButton.setOnAction(event -> showInformation(
                    Localization.text("Xuất báo cáo"),
                    Localization.text("Chức năng xuất tệp chưa được triển khai. Bảng đang hiển thị dữ liệu đã lưu.")));
            actions.getChildren().add(exportButton);
            Button refreshButton = new Button(Localization.text("Làm mới"));
            refreshButton.getStyleClass().add("button-secondary");
            refreshButton.setOnAction(event -> {
                refreshRows(renderedTable, featureService, module);
                applyFilters(renderedTable.filteredRows(), search, filterInputs);
            });
            actions.getChildren().add(refreshButton);
        } else if (isWritableModule(module.id())) {
            Button addButton = new Button(module.actionLabel());
            addButton.getStyleClass().add("button-primary");
            addButton.setOnAction(event -> editRow(module, featureService, null)
                    .ifPresent(values -> runMutation(() -> featureService.create(module, values),
                            () -> refreshRows(renderedTable, featureService, module))));

            actions.getChildren().add(addButton);
            if (!"UC-01".equals(module.id())) {
                Button editButton = new Button(Localization.text("Chỉnh sửa"));
                editButton.getStyleClass().add("button-secondary");
                editButton.disableProperty().bind(
                        renderedTable.table().getSelectionModel().selectedItemProperty().isNull());
                editButton.setOnAction(event -> {
                    ModuleRow selected = renderedTable.table().getSelectionModel().getSelectedItem();
                    if (selected != null) {
                        editRow(module, featureService, selected)
                                .ifPresent(values -> runMutation(
                                        () -> featureService.update(module, selected.id(), values),
                                        () -> refreshRows(renderedTable, featureService, module)));
                    }
                });

                Button deleteButton = new Button(Localization.text("Xóa"));
                deleteButton.getStyleClass().add("button-secondary");
                deleteButton.disableProperty().bind(
                        renderedTable.table().getSelectionModel().selectedItemProperty().isNull());
                deleteButton.setOnAction(event -> {
                    ModuleRow selected = renderedTable.table().getSelectionModel().getSelectedItem();
                    if (selected != null && confirmDelete(module)) {
                        runMutation(() -> featureService.delete(module, selected.id()),
                                () -> refreshRows(renderedTable, featureService, module));
                    }
                });
                actions.getChildren().addAll(editButton, deleteButton);
            }
        }

        if (module.report()) {
            page.getChildren().addAll(title, description,
                    createReportSummary(module, featureService), filters,
                    actions, createReportCharts(module, featureService), renderedTable.table());
        } else {
            Label supportNote = new Label(Localization.text(isWritableModule(module.id())
                    ? "Dữ liệu được đọc và ghi theo các bảng trong schema MySQL đã cung cấp."
                    : "Màn hình này hiện chỉ đọc. Thao tác ghi cần thêm thông tin liên quan để bảo toàn dữ liệu."));
            supportNote.getStyleClass().add("helper-text");
            supportNote.setWrapText(true);
            page.getChildren().addAll(title, description, supportNote, actions, renderedTable.table());
        }
        return page;
    }

    private HBox createFilters(
            ModuleDefinition module,
            List<Node> filterInputs,
            FilteredList<ModuleRow> rows,
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
        Button applyButton = new Button(Localization.text("Áp dụng"));
        applyButton.getStyleClass().add("button-secondary");
        applyButton.setOnAction(event -> applyFilters(rows, search, filterInputs));
        filters.getChildren().add(applyButton);
        return filters;
    }

    private RenderedTable createTable(
            ModuleDefinition module, TextField search, FeatureModuleService featureService) {
        TableView<ModuleRow> table = new TableView<>();
        table.getStyleClass().add("data-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        for (int index = 0; index < module.columns().size(); index++) {
            final int columnIndex = index;
            TableColumn<ModuleRow, String> column = new TableColumn<>(module.columns().get(index));
            column.setCellValueFactory(cell ->
                    new javafx.beans.property.ReadOnlyStringWrapper(
                            Localization.text(cell.getValue().values().get(columnIndex))));
            table.getColumns().add(column);
        }

        ObservableList<ModuleRow> source =
                FXCollections.observableArrayList(featureService.findAll(module));
        FilteredList<ModuleRow> filteredRows = new FilteredList<>(source, row -> true);
        table.setItems(filteredRows);
        Label emptyState = new Label(Localization.text("Không tìm thấy dữ liệu phù hợp."));
        emptyState.getStyleClass().add("table-empty-state");
        emptyState.setWrapText(true);
        table.setPlaceholder(emptyState);
        VBox.setVgrow(table, Priority.ALWAYS);

        RenderedTable result = new RenderedTable(table, source, filteredRows);
        return result;
    }

    private Node createReportSummary(ModuleDefinition module, FeatureModuleService featureService) {
        HBox summary = new HBox(12);
        summary.getStyleClass().add("demo-summary");
        summary.getChildren().add(summaryCard(Localization.text("BẢN GHI"),
                featureService.findAll(module).size() + Localization.text(" dòng")));
        summary.getChildren().add(summaryCard(Localization.text("PHẠM VI"), module.id()));
        summary.getChildren().add(summaryCard(Localization.text("TRẠNG THÁI"),
                Localization.text("Đọc từ MySQL")));
        return summary;
    }

    private Node createReportCharts(ModuleDefinition module, FeatureModuleService featureService) {
        FlowPane charts = new FlowPane(16, 16);
        charts.getStyleClass().add("report-chart-row");
        switch (module.id()) {
            case "UC-05" -> {
                List<ModuleRow> employees = featureService.findAll(module);
                List<String> names = employees.stream().map(row -> row.values().get(0)).toList();
                List<Number> hours = employees.stream()
                        .map(row -> (Number) parseDecimal(row.values().get(3).replace(" giờ", "")))
                        .toList();
                List<Number> shifts = employees.stream()
                        .map(row -> (Number) Integer.parseInt(row.values().get(2)))
                        .toList();
                charts.getChildren().add(chartCard("Giờ làm theo nhân viên", barChart(
                        "Nhân viên", "Số giờ", names, seriesFromList("Thực tế", hours))));
                charts.getChildren().add(chartCard("Phân bổ số ca", pieChart(
                        "Ca làm", seriesPairs(names, shifts))));
            }
            case "UC-06" -> {
                List<ModuleRow> attendance = featureService.findAll(module);
                List<String> names = attendance.stream().map(row -> row.values().get(0)).toList();
                List<Number> lateMinutes = attendance.stream()
                        .map(row -> (Number) parseLateMinutes(row.values().get(4)))
                        .toList();
                charts.getChildren().add(chartCard("Phút đi trễ theo nhân viên", barChart(
                        "Nhân viên", "Phút", names, seriesFromList("Đi trễ", lateMinutes))));
                long lateCount = attendance.stream()
                        .filter(row -> row.values().get(4).contains("Trễ")
                                || row.values().get(4).toLowerCase(Locale.ROOT).contains("late"))
                        .count();
                charts.getChildren().add(chartCard("Tỷ lệ đúng giờ", pieChart(
                        "Trạng thái", "Đúng giờ", attendance.size() - lateCount,
                        "Đi trễ", lateCount)));
            }
            case "UC-10" -> {
                List<ModuleRow> categoryRows = featureService.findAll(module);
                List<String> categories = categoryRows.stream()
                        .map(row -> Localization.text(row.values().get(0)))
                        .toList();
                List<Number> counts = categoryRows.stream()
                        .map(row -> (Number) Integer.parseInt(row.values().get(1)))
                        .toList();
                charts.getChildren().add(chartCard("Số sản phẩm mỗi danh mục", barChart(
                        "Danh mục", "Số sản phẩm", categories, seriesFromList("Sản phẩm", counts))));
                charts.getChildren().add(chartCard("Tỷ trọng danh mục",
                        pieChart("Danh mục", seriesPairs(categories, counts))));
            }
            case "UC-11" -> {
                List<ModuleRow> supplierRows = featureService.findAll(module);
                List<String> suppliers = supplierRows.stream()
                        .map(row -> Localization.text(row.values().get(0)))
                        .toList();
                List<Number> itemCounts = supplierRows.stream()
                        .map(row -> (Number) Integer.parseInt(row.values().get(1)))
                        .toList();
                charts.getChildren().add(chartCard("Sản phẩm theo nhà cung cấp", barChart(
                        "Nhà cung cấp", "Số mặt hàng", suppliers,
                        seriesFromList("Mặt hàng", itemCounts))));
                charts.getChildren().add(chartCard("Tỷ trọng nhà cung cấp",
                        pieChart("Nhà cung cấp", seriesPairs(suppliers, itemCounts))));
            }
            case "UC-15" -> {
                List<ModuleRow> movementRows = featureService.findAll(module);
                Map<String, Double> quantitiesByType = new LinkedHashMap<>();
                Map<String, Integer> countsByType = new LinkedHashMap<>();
                for (ModuleRow row : movementRows) {
                    String type = Localization.text(row.values().get(2));
                    quantitiesByType.merge(type, parseSignedQuantity(row.values().get(3)), Double::sum);
                    countsByType.merge(type, 1, Integer::sum);
                }
                List<String> movementTypes = new ArrayList<>(quantitiesByType.keySet());
                List<Number> movementQuantities = movementTypes.stream()
                        .map(type -> (Number) quantitiesByType.get(type))
                        .toList();
                List<Number> movementCounts = movementTypes.stream()
                        .map(type -> (Number) countsByType.get(type))
                        .toList();
                charts.getChildren().add(chartCard("Biến động tồn kho theo loại", areaChart(
                        "Loại biến động", "Số lượng", movementTypes,
                        seriesFromList("Biến động ròng", movementQuantities))));
                charts.getChildren().add(chartCard("Tỷ trọng loại biến động",
                        pieChart("Loại biến động", seriesPairs(movementTypes, movementCounts))));
            }
            case "UC-19" -> {
                List<ModuleRow> revenueRows = featureService.findAll(module);
                List<String> dates = revenueRows.stream().map(row -> row.values().get(0)).toList();
                List<Number> orderCounts = revenueRows.stream()
                        .map(row -> (Number) Integer.parseInt(row.values().get(1)))
                        .toList();
                List<Number> revenues = revenueRows.stream()
                        .map(row -> (Number) (parseAmount(row.values().get(2)) / 1_000_000.0))
                        .toList();
                charts.getChildren().add(chartCard("Số lượng hóa đơn theo ngày", barChart(
                        "Ngày", "Số đơn", dates, seriesFromList("Hóa đơn", orderCounts))));
                charts.getChildren().add(chartCard("Doanh thu theo ngày", lineChart(
                        "Ngày", "Doanh thu (triệu ₫)", dates,
                        seriesFromList("Doanh thu", revenues))));
            }
            default -> throw new IllegalArgumentException(
                    "Chưa cấu hình biểu đồ cho báo cáo " + module.id());
        }
        return charts;
    }

    private VBox chartCard(String title, Chart chart) {
        VBox card = new VBox(10);
        card.getStyleClass().add("chart-card");
        Label heading = new Label(Localization.text(title));
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
        xAxis.setLabel(Localization.text(categoryLabel));
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel(Localization.text(valueLabel));
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
        xAxis.setLabel(Localization.text(categoryLabel));
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel(Localization.text(valueLabel));
        LineChart<String, Number> chart = new LineChart<>(xAxis, yAxis);
        addSeries(chart, categories, values);
        return chart;
    }

    private AreaChart<String, Number> areaChart(
            String categoryLabel, String valueLabel, List<String> categories,
            Map<String, List<Number>> values) {
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel(Localization.text(categoryLabel));
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel(Localization.text(valueLabel));
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
                    Localization.text((String) nameValuePairs[index]),
                    ((Number) nameValuePairs[index + 1]).doubleValue()));
        }
        chart.setAccessibleText(nameLabel);
        return chart;
    }

    private void addSeries(
            XYChart<String, Number> chart, List<String> categories, Map<String, List<Number>> values) {
        for (Map.Entry<String, List<Number>> entry : values.entrySet()) {
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName(Localization.text(entry.getKey()));
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

    private Map<String, List<Number>> seriesFromList(String name, List<Number> values) {
        return Map.of(name, List.copyOf(values));
    }

    private Object[] seriesPairs(List<String> labels, List<Number> values) {
        if (labels.size() != values.size()) {
            throw new IllegalArgumentException("Chart labels and values must have the same size.");
        }
        List<Object> pairs = new ArrayList<>();
        for (int index = 0; index < labels.size(); index++) {
            pairs.add(labels.get(index));
            pairs.add(values.get(index));
        }
        return pairs.toArray();
    }

    private double parseDecimal(String value) {
        try {
            return Double.parseDouble(value.trim().replace(',', '.'));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Expected a numeric report value: " + value, exception);
        }
    }

    private long parseAmount(String value) {
        String digits = value.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) {
            throw new IllegalArgumentException("Expected a currency value: " + value);
        }
        try {
            return Long.parseLong(digits);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Currency value is outside the supported range: " + value,
                    exception);
        }
    }

    private int parseLateMinutes(String status) {
        if (!status.contains("Trễ") && !status.toLowerCase(Locale.ROOT).contains("late")) {
            return 0;
        }
        String digits = status.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) {
            throw new IllegalArgumentException("Late attendance status must include the delay in minutes.");
        }
        return Integer.parseInt(digits);
    }

    private double parseSignedQuantity(String quantity) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("[+-]?\\d+(?:[,.]\\d+)?")
                .matcher(quantity);
        if (!matcher.find()) {
            throw new IllegalArgumentException("Expected a numeric inventory quantity: " + quantity);
        }
        return parseDecimal(matcher.group());
    }

    private VBox summaryCard(String heading, String value) {
        VBox card = new VBox(7);
        card.getStyleClass().add("summary-card");
        Label title = new Label(Localization.text(heading));
        title.getStyleClass().add("overview-label");
        Label content = new Label(Localization.text(value));
        content.getStyleClass().add("overview-value");
        card.getChildren().addAll(title, content);
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    private void applyFilters(
            FilteredList<ModuleRow> rows, TextField search, List<Node> filterInputs) {
        String query = search.getText() == null
                ? "" : search.getText().trim().toLowerCase(Locale.ROOT);
        List<String> criteria = filterInputs.stream()
                .map(this::inputValue)
                .filter(value -> !value.isBlank())
                .map(value -> value.toLowerCase(Locale.ROOT))
                .toList();
        rows.setPredicate(row -> {
            String searchableRow = row.values().stream()
                    .map(Localization::text)
                    .reduce("", (left, right) -> left + " " + right)
                    .toLowerCase(Locale.ROOT);
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

    private void refreshRows(
            RenderedTable renderedTable,
            FeatureModuleService featureService,
            ModuleDefinition module) {
        renderedTable.source().setAll(featureService.findAll(module));
    }

    private java.util.Optional<List<String>> editRow(
            ModuleDefinition module,
            FeatureModuleService featureService,
            ModuleRow existingRow) {
        if ("UC-01".equals(module.id())) {
            return createEmployeeAccountDialog(module);
        }
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(existingRow == null
                ? Localization.text(module.actionLabel())
                : Localization.text("Chỉnh sửa") + " · " + module.title());
        dialog.setHeaderText(module.id() + " · " + module.title());
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);
        dialog.getDialogPane().getStyleClass().add("form-dialog");

        GridPane fields = new GridPane();
        fields.setHgap(16);
        fields.setVgap(12);
        fields.setPadding(new Insets(12, 0, 8, 0));
        List<TextField> inputs = new ArrayList<>();
        for (int index = 0; index < module.columns().size(); index++) {
            TextField input = new TextField();
            input.setPromptText(module.columns().get(index));
            input.getStyleClass().add("text-input");
            if (existingRow != null) {
                input.setText(Localization.text(existingRow.values().get(index)));
            }
            Label label = new Label(module.columns().get(index));
            label.getStyleClass().add("field-label");
            fields.add(label, 0, index);
            fields.add(input, 1, index);
            GridPane.setHgrow(input, Priority.ALWAYS);
            inputs.add(input);
        }
        Label note = new Label(Localization.text(
                "Dữ liệu nghiệp vụ được lưu trong MySQL; kết quả báo cáo được tính lại khi mở."));
        note.setWrapText(true);
        note.getStyleClass().add("helper-text");
        VBox content = new VBox(12, fields, note);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setPrefWidth(620);
        dialog.setResultConverter(button -> button == ButtonType.OK ? ButtonType.OK : null);
        return dialog.showAndWait()
                .filter(ButtonType.OK::equals)
                .map(ignored -> inputs.stream().map(input -> input.getText().trim()).toList())
                .map(values -> preserveUnchangedValues(existingRow, values));
    }

    private java.util.Optional<List<String>> createEmployeeAccountDialog(ModuleDefinition module) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(Localization.text(module.actionLabel()));
        dialog.setHeaderText(module.title());
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);
        dialog.getDialogPane().getStyleClass().add("form-dialog");

        GridPane fields = new GridPane();
        fields.setHgap(16);
        fields.setVgap(12);
        fields.setPadding(new Insets(12, 0, 8, 0));
        List<TextField> inputs = new ArrayList<>();
        for (int index = 0; index < 5; index++) {
            TextField input = new TextField();
            input.setPromptText(module.formFields().get(index));
            input.getStyleClass().add("text-input");
            Label label = new Label(module.formFields().get(index));
            label.getStyleClass().add("field-label");
            fields.add(label, 0, index);
            fields.add(input, 1, index);
            GridPane.setHgrow(input, Priority.ALWAYS);
            inputs.add(input);
        }

        PasswordField password = new PasswordField();
        password.setPromptText(module.formFields().get(5));
        password.getStyleClass().add("text-input");
        TextField visiblePassword = new TextField();
        visiblePassword.setPromptText(module.formFields().get(5));
        visiblePassword.getStyleClass().add("text-input");
        visiblePassword.textProperty().bindBidirectional(password.textProperty());
        visiblePassword.setVisible(false);
        visiblePassword.setManaged(false);
        StackPane passwordFields = new StackPane(password, visiblePassword);
        Button passwordVisibilityButton = new Button(Localization.text("Hiện"));
        passwordVisibilityButton.getStyleClass().add("button-secondary");
        passwordVisibilityButton.setOnAction(event -> {
            boolean showPassword = !visiblePassword.isVisible();
            visiblePassword.setVisible(showPassword);
            visiblePassword.setManaged(showPassword);
            password.setVisible(!showPassword);
            password.setManaged(!showPassword);
            passwordVisibilityButton.setText(Localization.text(showPassword ? "Ẩn" : "Hiện"));
            (showPassword ? visiblePassword : password).requestFocus();
        });
        Label passwordLabel = new Label(module.formFields().get(5));
        passwordLabel.getStyleClass().add("field-label");
        HBox passwordEntry = new HBox(8, passwordFields, passwordVisibilityButton);
        HBox.setHgrow(passwordFields, Priority.ALWAYS);
        fields.add(passwordLabel, 0, 5);
        fields.add(passwordEntry, 1, 5);

        Label note = new Label(Localization.text(
                "Tài khoản mới luôn có vai trò Nhân viên; mật khẩu được lưu dưới dạng mã hóa."));
        note.setWrapText(true);
        note.getStyleClass().add("helper-text");
        dialog.getDialogPane().setContent(new VBox(12, fields, note));
        dialog.getDialogPane().setPrefWidth(680);
        dialog.setResultConverter(button -> button == ButtonType.OK ? ButtonType.OK : null);
        return dialog.showAndWait()
                .filter(ButtonType.OK::equals)
                .map(ignored -> List.of(
                        inputs.get(0).getText().trim(),
                        inputs.get(1).getText().trim(),
                        inputs.get(2).getText().trim(),
                        inputs.get(3).getText().trim(),
                        inputs.get(4).getText().trim(),
                        "STAFF",
                        password.getText()));
    }

    private List<String> preserveUnchangedValues(ModuleRow original, List<String> editedValues) {
        if (original == null) {
            return editedValues;
        }
        List<String> values = new ArrayList<>(editedValues);
        for (int index = 0; index < values.size(); index++) {
            if (values.get(index).equals(Localization.text(original.values().get(index)))) {
                values.set(index, original.values().get(index));
            }
        }
        return List.copyOf(values);
    }

    private boolean confirmDelete(ModuleDefinition module) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(Localization.text("Xác nhận xóa"));
        alert.setHeaderText(module.title());
        alert.setContentText(Localization.text("Bạn có chắc muốn xóa dòng dữ liệu đang chọn?"));
        alert.getButtonTypes().setAll(ButtonType.CANCEL, ButtonType.OK);
        return alert.showAndWait().filter(ButtonType.OK::equals).isPresent();
    }

    private void runMutation(Runnable mutation, Runnable onSuccess) {
        try {
            mutation.run();
            onSuccess.run();
        } catch (IllegalArgumentException | IllegalStateException | java.util.NoSuchElementException
                 | vn.ninhkieupharma.pharmacy.features.common.dao.DataAccessException exception) {
            showInformation(Localization.text("Không thể cập nhật dữ liệu"), exception.getMessage());
        } catch (UnsupportedOperationException exception) {
            showInformation(Localization.text("Thao tác chưa hỗ trợ"), exception.getMessage());
        }
    }

    private boolean isWritableModule(String moduleId) {
        return List.of("UC-01", "UC-02", "UC-07", "UC-08", "UC-09", "UC-13", "UC-16", "UC-18")
                .contains(moduleId);
    }

    private Node createSalesWorkspace(
            ModuleDefinition module, FeatureModuleService featureService) {
        HBox workspace = new HBox(18);
        VBox catalog = new VBox(12);
        catalog.getStyleClass().add("dashboard-panel");
        HBox.setHgrow(catalog, Priority.ALWAYS);
        List<DemoData.Product> catalogProducts = productsInCatalog();
        Label catalogTitle = new Label(Localization.text("Sản phẩm mẫu · ")
                + catalogProducts.size() + Localization.text(" mặt hàng"));
        catalogTitle.getStyleClass().add("section-title");
        Label inventoryNote = new Label(Localization.text("Tồn kho và giá bán chỉ mang tính minh họa."));
        inventoryNote.getStyleClass().add("helper-text");
        TextField productSearch = new TextField();
        productSearch.setPromptText(Localization.text("Tìm tên sản phẩm hoặc mã hàng..."));
        productSearch.getStyleClass().add("search-input");

        FlowPane productGrid = new FlowPane(12, 12);
        productGrid.getStyleClass().add("product-grid");
        List<ProductCard> products = catalogProducts.stream()
                .map(this::productCard)
                .toList();
        productGrid.getChildren().setAll(products.stream().map(ProductCard::node).toList());
        productSearch.textProperty().addListener((observable, oldValue, query) -> {
            String normalizedQuery = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
            List<Node> matches = products.stream()
                    .filter(card -> (card.product().name() + " " + card.product().code() + " "
                                    + Localization.text(card.product().category()))
                            .toLowerCase(Locale.ROOT).contains(normalizedQuery))
                    .map(ProductCard::node)
                    .toList();
            productGrid.getChildren().setAll(matches);
        });
        catalog.getChildren().addAll(catalogTitle, inventoryNote, productSearch, productGrid);

        VBox invoice = new VBox(12);
        invoice.setPrefWidth(340);
        invoice.getStyleClass().add("invoice-panel");
        Label invoiceTitle = new Label(Localization.text("Hóa đơn mẫu"));
        invoiceTitle.getStyleClass().add("section-title");
        ComboBox<String> customer = new ComboBox<>();
        customer.getItems().addAll(
                Localization.text("Khách lẻ"),
                Localization.text("Nguyễn Thị Mai · KH0001"),
                Localization.text("Trần Văn Nam · KH0002"));
        customer.getSelectionModel().select(1);
        customer.setMaxWidth(Double.MAX_VALUE);
        customer.getStyleClass().add("text-input");

        ObservableList<DemoData.Product> cart = FXCollections.observableArrayList(
                catalogProducts.stream().limit(2).toList());
        VBox cartItems = new VBox(8);
        VBox.setVgrow(cartItems, Priority.ALWAYS);
        Label loyalty = new Label(Localization.text("Điểm tích lũy mẫu: 1.250 điểm"));
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
                    showInformation(Localization.text("Hết hàng mẫu"),
                            Localization.text("Sản phẩm minh họa này hiện không còn tồn kho."));
                }
            });
        }

        Button clear = new Button(Localization.text("Xóa giỏ"));
        clear.getStyleClass().add("button-secondary");
        clear.setMaxWidth(Double.MAX_VALUE);
        clear.setOnAction(event -> {
            cart.clear();
            refreshCart(cart, cartItems, total);
        });
        Button checkout = new Button(Localization.text("Hoàn tất hóa đơn"));
        checkout.getStyleClass().add("button-primary");
        checkout.setMaxWidth(Double.MAX_VALUE);
        checkout.setOnAction(event -> {
            if (cart.isEmpty()) {
                showInformation(Localization.text("Không thể tạo hóa đơn"),
                        Localization.text("Vui lòng thêm ít nhất một sản phẩm vào giỏ."));
                return;
            }
            long invoiceNumber = 842L + featureService.findAll(module).size();
            String invoiceId = "HD" + String.format(Locale.ROOT, "%05d", invoiceNumber);
            String dateTime = java.time.LocalDateTime.now().format(
                    java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
            long amount = cart.stream().mapToLong(DemoData.Product::price).sum();
            List<String> invoiceRow = List.of(
                    invoiceId,
                    dateTime,
                    List.of("Khách lẻ", "Nguyễn Thị Mai · KH0001", "Trần Văn Nam · KH0002")
                            .get(customer.getSelectionModel().getSelectedIndex()),
                    "Nhân viên demo",
                    NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN")).format(amount) + " ₫",
                    "Hoàn tất");
            runMutation(() -> featureService.create(module, invoiceRow), () -> {
                cart.clear();
                refreshCart(cart, cartItems, total);
                showInformation(Localization.text("Đã tạo hóa đơn"),
                        Localization.text("Hóa đơn mẫu đã được thêm vào dữ liệu phiên chạy: ") + invoiceId);
            });
        });
        invoice.getChildren().addAll(invoiceTitle, customer, cartItems, loyalty, total, clear, checkout);
        workspace.getChildren().addAll(catalog, invoice);
        VBox.setVgrow(workspace, Priority.ALWAYS);
        return workspace;
    }

    private List<DemoData.Product> productsInCatalog() {
        ModuleDefinition productsModule = ModuleCatalog.MODULES.stream()
                .filter(module -> module.id().equals("UC-08"))
                .findFirst()
                .orElseThrow();
        Map<String, DemoData.Product> sampleProducts = DemoData.products().stream()
                .collect(java.util.stream.Collectors.toMap(DemoData.Product::code, product -> product));
        return featureServices.forModule(productsModule).findAll(productsModule).stream()
                .filter(row -> row.values().size() >= 5)
                .map(row -> {
                    List<String> values = row.values();
                    DemoData.Product sample = sampleProducts.get(values.get(0));
                    return new DemoData.Product(
                            values.get(0),
                            values.get(1),
                            values.get(2),
                            values.get(3),
                            values.get(4),
                            sample == null ? 0 : sample.price(),
                            sample == null ? 0 : sample.stock());
                })
                .toList();
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
        total.setText(Localization.text("Tổng thanh toán     ") + formatCurrency(subtotal));
    }

    private ProductCard productCard(DemoData.Product product) {
        VBox card = new VBox(8);
        card.setPrefWidth(190);
        card.getStyleClass().add("product-card");
        Label category = new Label(Localization.text(product.category()));
        category.getStyleClass().add("overview-label");
        Label name = new Label(product.name());
        name.setWrapText(true);
        name.getStyleClass().add("cart-item-name");
        Label code = new Label(product.code() + " · " + Localization.text(product.unit()));
        code.getStyleClass().add("helper-text");
        Label price = new Label(formatCurrency(product.price()));
        price.getStyleClass().add("overview-value");
        Label stock = new Label(Localization.text("Tồn mẫu: ") + product.stock());
        stock.getStyleClass().add("helper-text");
        Button add = new Button(Localization.text("Thêm vào hóa đơn"));
        add.getStyleClass().add("button-secondary");
        add.setMaxWidth(Double.MAX_VALUE);
        card.getChildren().addAll(category, name, code, price, stock, add);
        return new ProductCard(product, card, add);
    }

    private String formatCurrency(long amount) {
        Locale locale = Localization.isEnglish() ? Locale.US : Locale.forLanguageTag("vi-VN");
        return NumberFormat.getNumberInstance(locale).format(amount) + " ₫";
    }

    private void showInformation(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

}
