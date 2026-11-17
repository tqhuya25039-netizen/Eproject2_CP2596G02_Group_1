package vn.ninhkieupharma.pharmacy.ui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Point2D;
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
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Popup;
import vn.ninhkieupharma.pharmacy.common.model.ModuleRow;
import vn.ninhkieupharma.pharmacy.common.service.FeatureModuleService;
import vn.ninhkieupharma.pharmacy.common.service.FeatureServices;
import vn.ninhkieupharma.pharmacy.common.service.ExchangeRateService;
import vn.ninhkieupharma.pharmacy.common.model.ModuleDefinition;
import vn.ninhkieupharma.pharmacy.common.service.DemoData;
import vn.ninhkieupharma.pharmacy.common.service.Localization;
import vn.ninhkieupharma.pharmacy.common.service.ModuleCatalog;
import vn.ninhkieupharma.pharmacy.domain.sales.PromotionsLoyaltyService;
import vn.ninhkieupharma.pharmacy.domain.sales.CustomerLoyaltyService;
import vn.ninhkieupharma.pharmacy.domain.sales.SaleAllergyWarning;
import vn.ninhkieupharma.pharmacy.domain.sales.SaleLineRequest;
import vn.ninhkieupharma.pharmacy.domain.sales.SaleOrderRequest;
import vn.ninhkieupharma.pharmacy.domain.sales.SaleProduct;
import vn.ninhkieupharma.pharmacy.domain.sales.SaleReceipt;
import vn.ninhkieupharma.pharmacy.domain.sales.SalesService;

import java.awt.Desktop;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import javafx.util.StringConverter;

public class PageRenderer {
    private record RenderedTable(
            TableView<ModuleRow> table,
            ObservableList<ModuleRow> source,
            FilteredList<ModuleRow> filteredRows) {
    }

    private record ProductCard(DemoData.Product product, Node node, Button addButton) {
    }

    private record SalesCustomerEntry(Node node) {
    }

    private record CheckoutLineControls(
            ComboBox<SaleProduct> product,
            TextField quantity,
            TextField dosage,
            TextField treatmentDays,
            Label allergyWarning,
            Node row) {
    }

    private record CheckoutSummary(
            VBox section,
            CheckBox redeemPoints,
            TextArea note,
            Label subtotal,
            Label status,
            Button completeButton) {
    }

    private record PromotionFilters(
            VBox container,
            TextField code,
            TextField name,
            ComboBox<String> status,
            DatePicker accrualStartFrom,
            DatePicker redemptionEndTo,
            Button applyButton) {
    }

    private record CustomerFilters(VBox container, ComboBox<String> status, Button applyButton) {
    }

    private final FeatureServices featureServices;
    private final ExchangeRateService exchangeRateService;
    private final long authenticatedUserId;

    public PageRenderer(FeatureServices featureServices, ExchangeRateService exchangeRateService) {
        this(featureServices, exchangeRateService, -1);
    }

    public PageRenderer(
            FeatureServices featureServices, ExchangeRateService exchangeRateService, long authenticatedUserId) {
        this.featureServices = Objects.requireNonNull(featureServices);
        this.exchangeRateService = Objects.requireNonNull(exchangeRateService);
        this.authenticatedUserId = authenticatedUserId;
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
        SalesCustomerEntry salesCustomerEntry = "UC-17".equals(module.id())
                ? createSalesCustomerEntry(module, featureService, renderedTable)
                : null;
        List<Node> filterInputs = new ArrayList<>();
        PromotionFilters promotionFilters = null;
        CustomerFilters customerFilters = null;
        Node filters = null;
        if (module.report()) {
            filters = createFilters(module, filterInputs, renderedTable.filteredRows(), search);
        } else if ("UC-18".equals(module.id())) {
            promotionFilters = createPromotionFilters();
            filters = promotionFilters.container();
            PromotionFilters capturedFilters = promotionFilters;
            capturedFilters.applyButton().setOnAction(event -> applyPromotionFilters(
                            renderedTable.filteredRows(), search, capturedFilters));
        } else if ("UC-16".equals(module.id())) {
            customerFilters = createCustomerFilters();
            filters = customerFilters.container();
            CustomerFilters capturedFilters = customerFilters;
            capturedFilters.applyButton().setOnAction(event -> applyCustomerFilters(
                    renderedTable.filteredRows(), search, capturedFilters));
        }
        if (renderedTable != null) {
            PromotionFilters capturedFilters = promotionFilters;
            CustomerFilters capturedCustomerFilters = customerFilters;
            search.textProperty().addListener((observable, oldValue, newValue) -> {
                if (capturedFilters == null) {
                    if (capturedCustomerFilters == null) {
                        applyFilters(renderedTable.filteredRows(), search, filterInputs);
                    } else {
                        applyCustomerFilters(
                                renderedTable.filteredRows(), search, capturedCustomerFilters);
                    }
                } else {
                    applyPromotionFilters(renderedTable.filteredRows(), search, capturedFilters);
                }
            });
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
        } else if (salesCustomerEntry != null) {
            Button refreshSales = new Button(Localization.text("Làm mới"));
            refreshSales.getStyleClass().add("button-secondary");
            refreshSales.setOnAction(event -> refreshRows(renderedTable, featureService, module));
            actions.getChildren().add(refreshSales);
        } else if (isWritableModule(module.id())) {
            Button addButton = new Button(module.actionLabel());
            addButton.getStyleClass().add("button-primary");
            if ("UC-16".equals(module.id())) {
                addButton.setOnAction(event -> editCustomerRow(
                        module, featureService, renderedTable, null));
            } else {
                addButton.setOnAction(event -> editRow(module, featureService, null)
                        .ifPresent(values -> runMutation(() -> featureService.create(module, values),
                                () -> refreshRows(renderedTable, featureService, module))));
            }

            actions.getChildren().add(addButton);
            if (!"UC-01".equals(module.id())) {
                Button editButton = new Button(Localization.text("Chỉnh sửa"));
                editButton.getStyleClass().add("button-secondary");
                editButton.disableProperty().bind(
                        renderedTable.table().getSelectionModel().selectedItemProperty().isNull());
                editButton.setOnAction(event -> {
                    ModuleRow selected = renderedTable.table().getSelectionModel().getSelectedItem();
                    if (selected != null) {
                        if ("UC-16".equals(module.id())) {
                            editCustomerRow(module, featureService, renderedTable, selected);
                        } else {
                            editRow(module, featureService, selected)
                                    .ifPresent(values -> runMutation(
                                            () -> featureService.update(module, selected.id(), values),
                                            () -> refreshRows(renderedTable, featureService, module)));
                        }
                    }
                });

                Button deleteButton = new Button(Localization.text(
                        "UC-18".equals(module.id()) ? "Vô hiệu hóa"
                                : "UC-16".equals(module.id()) ? "Lưu trữ" : "Xóa"));
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
                if ("UC-16".equals(module.id())) {
                    Button historyButton = new Button(Localization.text("Lịch sử mua"));
                    historyButton.getStyleClass().add("button-secondary");
                    historyButton.disableProperty().bind(
                            renderedTable.table().getSelectionModel().selectedItemProperty().isNull());
                    historyButton.setOnAction(event -> {
                        ModuleRow selected = renderedTable.table().getSelectionModel().getSelectedItem();
                        if (selected != null) {
                            showCustomerPurchaseHistory(selected);
                        }
                    });
                    actions.getChildren().add(historyButton);
                }
                if ("UC-18".equals(module.id())) {
                    Button endNowButton = new Button(Localization.text("Kết thúc ngay"));
                    endNowButton.getStyleClass().add("button-secondary");
                    endNowButton.disableProperty().bind(
                            renderedTable.table().getSelectionModel().selectedItemProperty().isNull());
                    endNowButton.setOnAction(event -> {
                        ModuleRow selected = renderedTable.table().getSelectionModel().getSelectedItem();
                        if (selected != null && confirmEndPromotionImmediately()) {
                            runMutation(
                                    () -> ((PromotionsLoyaltyService) featureService)
                                            .finishImmediately(module, selected.id()),
                                    () -> refreshRows(renderedTable, featureService, module));
                        }
                    });
                    actions.getChildren().add(endNowButton);
                }
            }
        }

        if (module.report()) {
            page.getChildren().addAll(title, description,
                    createReportSummary(module, featureService), filters,
                    actions, createReportCharts(module, featureService), renderedTable.table());
        } else {
            String note = salesCustomerEntry != null
                    ? "Đơn hoàn tất được lưu cùng chi tiết thuốc, ưu đãi, điểm và biến động FEFO trong một giao dịch."
                    : isWritableModule(module.id())
                            ? "Dữ liệu được đọc và ghi theo các bảng trong schema MySQL đã cung cấp."
                            : "Màn hình này hiện chỉ đọc. Thao tác ghi cần thêm thông tin liên quan để bảo toàn dữ liệu.";
            Label supportNote = new Label(Localization.text(note));
            supportNote.getStyleClass().add("helper-text");
            supportNote.setWrapText(true);
            if ("UC-18".equals(module.id())) {
                page.getChildren().addAll(title, description, supportNote,
                        createExchangeRateInformation(), filters, actions, renderedTable.table());
            } else if (salesCustomerEntry != null) {
                page.getChildren().addAll(
                        title, description, supportNote, salesCustomerEntry.node(), actions, renderedTable.table());
            } else {
                page.getChildren().addAll(title, description, supportNote, actions, renderedTable.table());
            }
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

    private PromotionFilters createPromotionFilters() {
        TextField code = new TextField();
        code.setPromptText(Localization.text("Mã chương trình"));
        TextField name = new TextField();
        name.setPromptText(Localization.text("Tên chương trình"));
        ComboBox<String> status = new ComboBox<>();
        status.getItems().addAll(
                Localization.text("Tất cả trạng thái"),
                Localization.text("Đang áp dụng"),
                Localization.text("Đã vô hiệu hóa"));
        status.getSelectionModel().selectFirst();
        DatePicker accrualStartFrom = new DatePicker();
        accrualStartFrom.setPromptText(Localization.text("Ngày bắt đầu từ"));
        DatePicker redemptionEndTo = new DatePicker();
        redemptionEndTo.setPromptText(Localization.text("Ngày kết thúc đến"));
        Button applyButton = new Button(Localization.text("Áp dụng bộ lọc"));
        applyButton.getStyleClass().add("button-secondary");

        HBox textFields = new HBox(10, code, name, status);
        HBox datesAndApply = new HBox(10, accrualStartFrom, redemptionEndTo, applyButton);
        textFields.setAlignment(Pos.CENTER_LEFT);
        datesAndApply.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(code, Priority.ALWAYS);
        HBox.setHgrow(name, Priority.ALWAYS);
        VBox filters = new VBox(10, textFields, datesAndApply);
        filters.getStyleClass().add("filter-bar");
        return new PromotionFilters(
                filters, code, name, status, accrualStartFrom, redemptionEndTo, applyButton);
    }

    private CustomerFilters createCustomerFilters() {
        ComboBox<String> status = new ComboBox<>();
        status.getItems().addAll(
                Localization.text("Tất cả trạng thái"),
                Localization.text("Đang hoạt động"),
                Localization.text("Hạn chế bán"),
                Localization.text("Đã lưu trữ"));
        status.getSelectionModel().selectFirst();
        Button applyButton = new Button(Localization.text("Áp dụng bộ lọc"));
        applyButton.getStyleClass().add("button-secondary");
        HBox filters = new HBox(10, status, applyButton);
        filters.setAlignment(Pos.CENTER_LEFT);
        filters.getStyleClass().add("filter-bar");
        return new CustomerFilters(new VBox(filters), status, applyButton);
    }

    private void applyCustomerFilters(
            FilteredList<ModuleRow> rows, TextField search, CustomerFilters filters) {
        String query = normalize(search.getText());
        String phoneQuery = normalize(search.getText() == null
                ? "" : search.getText().replaceAll("[\\s().-]", ""));
        int selectedStatus = filters.status().getSelectionModel().getSelectedIndex();
        rows.setPredicate(row -> {
            String searchableText = normalize(String.join(" ", row.values()));
            String customerPhone = row.values().get(2).replaceAll("[\\s().-]", "");
            if (!searchableText.contains(query) && !customerPhone.contains(phoneQuery)) {
                return false;
            }
            String status = row.values().get(6);
            return switch (selectedStatus) {
                case 1 -> "ACTIVE".equals(status);
                case 2 -> "BLOCKED".equals(status) || "ARCHIVED_BLOCKED".equals(status);
                case 3 -> "ARCHIVED".equals(status) || "ARCHIVED_BLOCKED".equals(status);
                default -> true;
            };
        });
    }

    private void applyPromotionFilters(
            FilteredList<ModuleRow> rows, TextField search, PromotionFilters filters) {
        String query = normalize(search.getText());
        String code = normalize(filters.code().getText());
        String name = normalize(filters.name().getText());
        String status = filters.status().getSelectionModel().getSelectedIndex() == 1
                ? "ACTIVE"
                : filters.status().getSelectionModel().getSelectedIndex() == 2 ? "INACTIVE" : "";
        LocalDate accrualStartFrom = filters.accrualStartFrom().getValue();
        LocalDate redemptionEndTo = filters.redemptionEndTo().getValue();
        rows.setPredicate(row -> {
            List<String> values = row.values();
            String searchable = values.stream()
                    .map(Localization::text)
                    .map(value -> value.toLowerCase(Locale.ROOT))
                    .reduce("", (left, right) -> left + " " + right);
            boolean searchMatches = query.isBlank() || searchable.contains(query);
            boolean codeMatches = code.isBlank()
                    || values.get(0).toLowerCase(Locale.ROOT).contains(code);
            boolean nameMatches = name.isBlank()
                    || values.get(1).toLowerCase(Locale.ROOT).contains(name);
            boolean statusMatches = status.isBlank() || values.get(9).equalsIgnoreCase(status);
            boolean startMatches = accrualStartFrom == null
                    || !LocalDate.parse(values.get(3)).isBefore(accrualStartFrom);
            boolean endMatches = redemptionEndTo == null
                    || !LocalDate.parse(values.get(6)).isAfter(redemptionEndTo);
            return searchMatches && codeMatches && nameMatches
                    && statusMatches && startMatches && endMatches;
        });
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private Node createExchangeRateInformation() {
        VBox information = new VBox(5);
        information.getStyleClass().add("helper-text");
        Label note = new Label(Localization.text(
                "Tỷ giá tham chiếu USD/VNĐ được cập nhật mỗi ngày khi đăng nhập."));
        note.setWrapText(true);
        Hyperlink attribution = new Hyperlink(Localization.text("Rates By Exchange Rate API"));
        attribution.setOnAction(event -> {
            try {
                if (!Desktop.isDesktopSupported()) {
                    throw new UnsupportedOperationException("Desktop browsing is unavailable.");
                }
                Desktop.getDesktop().browse(URI.create("https://www.exchangerate-api.com/"));
            } catch (IOException | UnsupportedOperationException | SecurityException exception) {
                showInformation(
                        Localization.text("Tỷ giá USD/VNĐ"),
                        Localization.text("Không thể mở liên kết tỷ giá.")
                                + " " + exception.getMessage());
            }
        });
        information.getChildren().addAll(note, attribution);
        return information;
    }

    private String formatPromotionColumn(int columnIndex, String value) {
        return switch (columnIndex) {
            case 7 -> formatPromotionMoney(new BigDecimal(value), true);
            case 8 -> formatPromotionTiers(value);
            case 9 -> "ACTIVE".equalsIgnoreCase(value)
                    ? Localization.text("Đang áp dụng")
                    : Localization.text("Đã vô hiệu hóa");
            default -> value;
        };
    }

    private String formatPromotionTiers(String encodedTiers) {
        List<String> formattedTiers = new ArrayList<>();
        for (String encoded : encodedTiers.split(";")) {
            String[] parts = encoded.split(":", -1);
            BigDecimal thresholdVnd = new BigDecimal(parts[0]);
            String threshold = formatPromotionMoney(thresholdVnd, false);
            formattedTiers.add(threshold + " → " + parts[1] + "%");
        }
        return String.join(" · ", formattedTiers);
    }

    private String formatPromotionMoney(BigDecimal vndAmount, boolean perPoint) {
        Locale locale = Localization.isEnglish() ? Locale.US : Locale.forLanguageTag("vi-VN");
        BigDecimal displayAmount = vndAmount;
        if (Localization.isEnglish()) {
            if (!exchangeRateService.snapshot().available()) {
                return Localization.text("Tỷ giá chưa sẵn sàng");
            }
            displayAmount = exchangeRateService.convertVndToUsd(vndAmount);
        }
        NumberFormat formatter = NumberFormat.getNumberInstance(locale);
        formatter.setMinimumFractionDigits(perPoint || Localization.isEnglish() ? 2 : 0);
        formatter.setMaximumFractionDigits(perPoint || Localization.isEnglish() ? 8 : 2);
        String amount = formatter.format(displayAmount);
        if (Localization.isEnglish()) {
            return "$" + amount;
        }
        return amount + " VNĐ";
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
            column.setCellValueFactory(cell -> {
                String value = cell.getValue().values().get(columnIndex);
                if ("UC-18".equals(module.id())) {
                    value = formatPromotionColumn(columnIndex, value);
                }
                if ("UC-16".equals(module.id()) && columnIndex == 6) {
                    value = customerStatusLabel(value);
                }
                if ("UC-16".equals(module.id()) && columnIndex == 5) {
                    value = NumberFormat.getIntegerInstance(Localization.isEnglish()
                                    ? Locale.US : Locale.forLanguageTag("vi-VN"))
                            .format(Long.parseLong(value));
                }
                if ("UC-08".equals(module.id()) && columnIndex == 8 && !value.isBlank()) {
                    value = formatSaleMoney(new BigDecimal(value));
                }
                return new javafx.beans.property.ReadOnlyStringWrapper(Localization.text(value));
            });
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
                        .map(row -> row.values().get(0) + " · " + Localization.text(row.values().get(1)))
                        .toList();
                List<Number> counts = categoryRows.stream()
                        .map(row -> (Number) Integer.parseInt(row.values().get(2).split("\\s+")[0]))
                        .toList();
                charts.getChildren().add(chartCard("Số sản phẩm theo quý & danh mục", barChart(
                        "Quý · Danh mục", "Số sản phẩm", categories, seriesFromList("Sản phẩm", counts))));
                charts.getChildren().add(chartCard("Tỷ trọng ngành hàng",
                        pieChart("Danh mục", seriesPairs(categories, counts))));
            }
            case "UC-11" -> {
                List<ModuleRow> supplierRows = featureService.findAll(module);
                List<String> suppliers = supplierRows.stream()
                        .map(row -> row.values().get(0) + " · " + Localization.text(row.values().get(1)))
                        .toList();
                List<Number> itemCounts = supplierRows.stream()
                        .map(row -> (Number) Integer.parseInt(row.values().get(2)))
                        .toList();
                charts.getChildren().add(chartCard("Mặt hàng cung ứng theo tháng & NCC", barChart(
                        "Tháng · Nhà cung cấp", "Số mặt hàng", suppliers,
                        seriesFromList("Mặt hàng", itemCounts))));
                charts.getChildren().add(chartCard("Tỷ trọng độ phủ nhà cung cấp",
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
        if ("UC-18".equals(module.id())) {
            return createPromotionDialog(
                    module, (PromotionsLoyaltyService) featureService, existingRow);
        }
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(existingRow == null
                ? Localization.text(module.actionLabel())
                : Localization.text("Chỉnh sửa") + " · " + module.title());
        dialog.setHeaderText(module.title());
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

    private java.util.Optional<List<String>> createCustomerDialog(ModuleRow existingRow) {
        List<String> oldValues = existingRow == null ? List.of() : existingRow.values();
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(Localization.text(existingRow == null ? "Thêm khách hàng" : "Chỉnh sửa khách hàng"));
        dialog.setHeaderText(Localization.text(existingRow == null
                ? "Tạo hồ sơ khách hàng" : "Hồ sơ khách hàng"));
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);
        dialog.getDialogPane().getStyleClass().add("form-dialog");

        TextField name = customerTextField(oldValues, 1);
        TextField phone = customerTextField(oldValues, 2);
        TextField email = customerTextField(oldValues, 3);
        DatePicker dateOfBirth = new DatePicker();
        if (oldValues.size() > 8 && !oldValues.get(8).isBlank()) {
            dateOfBirth.setValue(LocalDate.parse(oldValues.get(8)));
        }
        TextArea allergies = new TextArea(oldValues.isEmpty() ? "" : oldValues.get(4));
        allergies.setPromptText(Localization.text("Mỗi dòng: tên dị ứng | ghi chú tùy chọn"));
        allergies.setPrefRowCount(4);
        allergies.setWrapText(true);
        allergies.getStyleClass().add("text-input");
        CheckBox blocked = new CheckBox(Localization.text("Hạn chế bán"));
        CheckBox archived = new CheckBox(Localization.text("Lưu trữ hồ sơ"));
        String oldStatus = oldValues.isEmpty() ? "ACTIVE" : oldValues.get(6);
        blocked.setSelected(oldStatus.equals("BLOCKED") || oldStatus.equals("ARCHIVED_BLOCKED"));
        archived.setSelected(oldStatus.equals("ARCHIVED") || oldStatus.equals("ARCHIVED_BLOCKED"));
        TextField reason = customerTextField(oldValues, 7);
        reason.setPromptText(Localization.text("Bắt buộc nếu hạn chế bán"));

        GridPane fields = new GridPane();
        fields.setHgap(16);
        fields.setVgap(12);
        fields.setPadding(new Insets(12, 0, 8, 0));
        addCustomerField(fields, 0, "Họ và tên", name);
        addCustomerField(fields, 1, "Điện thoại", phone);
        addCustomerField(fields, 2, "Email", email);
        addCustomerField(fields, 3, "Ngày sinh", dateOfBirth);
        addCustomerField(fields, 4, "Dị ứng", allergies);
        addCustomerField(fields, 5, "Trạng thái", new VBox(8, blocked, archived));
        addCustomerField(fields, 6, "Lý do hạn chế", reason);
        if (!oldValues.isEmpty()) {
            String points = NumberFormat.getIntegerInstance(Localization.isEnglish()
                            ? Locale.US : Locale.forLanguageTag("vi-VN"))
                    .format(Long.parseLong(oldValues.get(5)));
            Label loyaltyPoints = new Label(Localization.text("Điểm hiện có: ") + points);
            loyaltyPoints.getStyleClass().add("helper-text");
            addCustomerField(fields, 7, "Điểm", loyaltyPoints);
        }

        Label validationMessage = new Label();
        validationMessage.getStyleClass().add("form-error");
        validationMessage.setWrapText(true);
        VBox content = new VBox(12, fields, new Label(Localization.text(
                "Dị ứng do khách hàng khai báo. Mỗi dị ứng nhập trên một dòng; có thể thêm ghi chú sau dấu |.")),
                validationMessage);
        content.getChildren().get(1).getStyleClass().add("helper-text");
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setPrefWidth(680);

        javafx.scene.Node okButton = dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.addEventFilter(ActionEvent.ACTION, event -> {
            String validationError = validateCustomerForm(name, phone, email, blocked, reason);
            if (dateOfBirth.getValue() != null && dateOfBirth.getValue().isAfter(LocalDate.now())) {
                validationError = "Ngày sinh không thể ở tương lai.";
            }
            if (!validationError.isEmpty()) {
                validationMessage.setText(Localization.text(validationError));
                event.consume();
            }
        });
        dialog.setResultConverter(button -> button == ButtonType.OK ? ButtonType.OK : null);
        return dialog.showAndWait()
                .filter(ButtonType.OK::equals)
                .map(ignored -> {
                    String status = blocked.isSelected()
                            ? (archived.isSelected() ? "ARCHIVED_BLOCKED" : "BLOCKED")
                            : (archived.isSelected() ? "ARCHIVED" : "ACTIVE");
                    return List.of(
                            oldValues.isEmpty() ? "" : oldValues.get(0),
                            name.getText().trim(),
                            phone.getText().trim(),
                            email.getText().trim(),
                            allergies.getText().trim(),
                            oldValues.isEmpty() ? "0" : oldValues.get(5),
                            status,
                            reason.getText().trim(),
                            dateOfBirth.getValue() == null ? "" : dateOfBirth.getValue().toString());
                });
    }

    private void editCustomerRow(
            ModuleDefinition module,
            FeatureModuleService featureService,
            RenderedTable renderedTable,
            ModuleRow selectedRow) {
        ModuleRow draft = selectedRow;
        while (true) {
            java.util.Optional<List<String>> submitted = createCustomerDialog(draft);
            if (submitted.isEmpty()) {
                return;
            }
            List<String> values = submitted.orElseThrow();
            try {
                if (selectedRow == null) {
                    featureService.create(module, values);
                } else {
                    featureService.update(module, selectedRow.id(), values);
                }
                refreshRows(renderedTable, featureService, module);
                return;
            } catch (IllegalArgumentException | IllegalStateException
                     | java.util.NoSuchElementException
                     | vn.ninhkieupharma.pharmacy.common.dao.DataAccessException exception) {
                showInformation(Localization.text("Không thể cập nhật dữ liệu"), exception.getMessage());
                draft = new ModuleRow(selectedRow == null ? UUID.randomUUID() : selectedRow.id(), values);
            } catch (UnsupportedOperationException exception) {
                showInformation(Localization.text("Thao tác chưa hỗ trợ"), exception.getMessage());
                draft = new ModuleRow(selectedRow == null ? UUID.randomUUID() : selectedRow.id(), values);
            }
        }
    }

    private TextField customerTextField(List<String> values, int index) {
        TextField field = new TextField();
        field.setText(values.size() > index ? values.get(index) : "");
        field.getStyleClass().add("text-input");
        return field;
    }

    private void addCustomerField(GridPane fields, int row, String labelText, Node input) {
        Label label = new Label(Localization.text(labelText));
        label.getStyleClass().add("field-label");
        fields.add(label, 0, row);
        fields.add(input, 1, row);
        GridPane.setHgrow(input, Priority.ALWAYS);
    }

    private String validateCustomerForm(
            TextField name, TextField phone, TextField email, CheckBox blocked, TextField reason) {
        if (name.getText().isBlank()) {
            return "Tên khách hàng là bắt buộc.";
        }
        String normalizedPhone = phone.getText().replaceAll("[\\s().-]", "");
        if (!normalizedPhone.isEmpty() && !normalizedPhone.matches("\\+?[0-9]{7,15}")) {
            return "Số điện thoại cần có từ 7 đến 15 chữ số.";
        }
        String emailValue = email.getText().trim();
        if (!emailValue.isEmpty() && !emailValue.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            return "Email không hợp lệ.";
        }
        if (normalizedPhone.isEmpty() && emailValue.isEmpty()) {
            return "Cần nhập số điện thoại hoặc email của khách hàng.";
        }
        if (blocked.isSelected() && reason.getText().isBlank()) {
            return "Cần nhập lý do khi hạn chế bán.";
        }
        return "";
    }

    private String customerStatusLabel(String status) {
        return switch (status) {
            case "BLOCKED" -> "Hạn chế bán";
            case "ARCHIVED" -> "Đã lưu trữ";
            case "ARCHIVED_BLOCKED" -> "Đã lưu trữ · hạn chế bán";
            default -> "Đang hoạt động";
        };
    }

    private void showCustomerPurchaseHistory(ModuleRow customer) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(Localization.text("Lịch sử mua"));
        dialog.setHeaderText(customer.values().get(1));
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().getStyleClass().add("form-dialog");

        TableView<ModuleRow> historyTable = new TableView<>();
        historyTable.getStyleClass().add("data-table");
        historyTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        List<String> columns = List.of("Mã hóa đơn", "Ngày giao dịch", "Tổng tiền", "Trạng thái");
        for (int index = 0; index < columns.size(); index++) {
            final int columnIndex = index;
            TableColumn<ModuleRow, String> column = new TableColumn<>(Localization.text(columns.get(index)));
            column.setCellValueFactory(cell -> {
                String value = cell.getValue().values().get(columnIndex);
                if (columnIndex == 2) {
                    value = NumberFormat.getNumberInstance(Localization.isEnglish()
                                    ? Locale.US : Locale.forLanguageTag("vi-VN"))
                            .format(new BigDecimal(value)) + " ₫";
                } else if (columnIndex == 3 && "COMPLETED".equals(value)) {
                    value = "Hoàn tất";
                } else if (columnIndex == 3 && "VOIDED".equals(value)) {
                    value = "Đã hủy";
                }
                return new javafx.beans.property.ReadOnlyStringWrapper(Localization.text(value));
            });
            historyTable.getColumns().add(column);
        }
        historyTable.setItems(FXCollections.observableArrayList(
                featureServices.customerLoyaltyService().findPurchaseHistory(customer.id())));
        Label emptyState = new Label(Localization.text("Khách hàng chưa có giao dịch."));
        emptyState.getStyleClass().add("table-empty-state");
        historyTable.setPlaceholder(emptyState);
        dialog.getDialogPane().setContent(historyTable);
        dialog.getDialogPane().setPrefSize(760, 420);
        dialog.showAndWait();
    }

    private record TierInput(TextField threshold, TextField rate) {
    }

    private java.util.Optional<List<String>> createPromotionDialog(
            ModuleDefinition module, PromotionsLoyaltyService service, ModuleRow existingRow) {
        if (Localization.isEnglish() && !exchangeRateService.snapshot().available()) {
            showInformation(
                    Localization.text("Tỷ giá USD/VNĐ"),
                    Localization.text("Tỷ giá chưa sẵn sàng"));
            return java.util.Optional.empty();
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(existingRow == null
                ? Localization.text(module.actionLabel())
                : Localization.text("Chỉnh sửa") + " · " + module.title());
        dialog.setHeaderText(module.title());
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);
        dialog.getDialogPane().getStyleClass().add("form-dialog");

        List<String> oldValues = existingRow == null ? List.of() : existingRow.values();
        LocalDate today = LocalDate.now();
        TextField code = promotionTextField(oldValues, 0, "KM" + today.toString().replace("-", ""));
        TextField name = promotionTextField(oldValues, 1, "");
        TextField discount = promotionTextField(oldValues, 2, "0");
        TextField redemptionValue = promotionTextField(oldValues, 7, "1000");
        if (!oldValues.isEmpty()) {
            redemptionValue.setText(formatInputMoney(new BigDecimal(oldValues.get(7))));
        }

        DatePicker accrualStart = promotionDatePicker(oldValues, 3, today);
        DatePicker redemptionStart = promotionDatePicker(oldValues, 4, today);
        DatePicker accrualEnd = promotionDatePicker(oldValues, 5, today);
        DatePicker redemptionEnd = promotionDatePicker(oldValues, 6, today);
        ComboBox<String> status = new ComboBox<>();
        status.getItems().addAll(
                Localization.text("Đang áp dụng"), Localization.text("Đã vô hiệu hóa"));
        status.getSelectionModel().select(
                oldValues.isEmpty() || "ACTIVE".equalsIgnoreCase(oldValues.get(9)) ? 0 : 1);

        GridPane fields = new GridPane();
        fields.setHgap(16);
        fields.setVgap(10);
        fields.setPadding(new Insets(10, 0, 0, 0));
        addPromotionField(fields, 0, "Mã chương trình", code);
        addPromotionField(fields, 1, "Tên chương trình", name);
        addPromotionField(fields, 2, "Giảm giá (%)", discount);
        addPromotionField(fields, 3, "Bắt đầu tích điểm", accrualStart);
        addPromotionField(fields, 4, "Bắt đầu đổi điểm", redemptionStart);
        addPromotionField(fields, 5, "Kết thúc tích điểm", accrualEnd);
        addPromotionField(fields, 6, "Kết thúc đổi điểm", redemptionEnd);
        addPromotionField(fields, 7, "Giá trị quy đổi mỗi điểm", redemptionValue);
        addPromotionField(fields, 8, "Trạng thái", status);

        VBox tierInputs = new VBox(8);
        List<TierInput> tiers = new ArrayList<>();
        if (oldValues.isEmpty()) {
            addTierInput(tierInputs, tiers, "0", "1");
        } else {
            for (String encoded : oldValues.get(8).split(";")) {
                String[] parts = encoded.split(":", -1);
                addTierInput(tierInputs, tiers,
                        formatInputMoney(new BigDecimal(parts[0])), parts[1]);
            }
        }
        Button addTier = new Button(Localization.text("Thêm bậc"));
        addTier.getStyleClass().add("button-secondary");
        addTier.setOnAction(event -> addTierInput(tierInputs, tiers, "", ""));
        Label tierLabel = new Label(Localization.text("Bậc tích điểm"));
        tierLabel.getStyleClass().add("field-label");
        VBox tierSection = new VBox(8, tierLabel, tierInputs, addTier);
        Label note = new Label(Localization.text(
                "Bậc có dạng: ngưỡng hóa đơn → phần trăm điểm. Điểm = hóa đơn cuối × phần trăm, làm tròn xuống.")
                + " " + Localization.text(
                "Điều kiện ngày: bắt đầu tích ≤ bắt đầu đổi ≤ kết thúc tích ≤ kết thúc đổi."));
        note.setWrapText(true);
        note.getStyleClass().add("helper-text");
        VBox content = new VBox(12, fields, tierSection, note);
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefViewportHeight(560);
        scrollPane.getStyleClass().add("page-scroll");
        dialog.getDialogPane().setContent(scrollPane);
        dialog.getDialogPane().setPrefWidth(720);
        dialog.getDialogPane().setPrefHeight(680);
        dialog.setResultConverter(button -> button == ButtonType.OK ? ButtonType.OK : null);

        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.addEventFilter(ActionEvent.ACTION, event -> {
            try {
                List<String> values = promotionValues(
                        code, name, discount, accrualStart, redemptionStart,
                        accrualEnd, redemptionEnd, redemptionValue, tiers, status);
                boolean validateStart = existingRow == null
                        || !existingRow.values().get(3).equals(values.get(3));
                service.validateConfiguration(module, values, validateStart, existingRow);
            } catch (IllegalArgumentException exception) {
                event.consume();
                showInformation(
                        Localization.text("Không thể cập nhật dữ liệu"),
                        Localization.text(exception.getMessage()));
            }
        });
        return dialog.showAndWait().filter(ButtonType.OK::equals)
                .map(ignored -> promotionValues(
                        code, name, discount, accrualStart, redemptionStart,
                        accrualEnd, redemptionEnd, redemptionValue, tiers, status));
    }

    private TextField promotionTextField(List<String> values, int index, String defaultValue) {
        TextField input = new TextField(values.isEmpty() ? defaultValue : values.get(index));
        input.getStyleClass().add("text-input");
        return input;
    }

    private DatePicker promotionDatePicker(List<String> values, int index, LocalDate defaultDate) {
        DatePicker picker = new DatePicker(values.isEmpty()
                ? defaultDate : LocalDate.parse(values.get(index)));
        picker.getStyleClass().add("text-input");
        return picker;
    }

    private void addPromotionField(GridPane grid, int row, String labelText, Node input) {
        Label label = new Label(Localization.text(labelText));
        label.getStyleClass().add("field-label");
        grid.add(label, 0, row);
        grid.add(input, 1, row);
        GridPane.setHgrow(input, Priority.ALWAYS);
        if (input instanceof TextField textField) {
            textField.setPromptText(Localization.text(labelText));
        }
    }

    private void addTierInput(
            VBox container, List<TierInput> tiers, String thresholdValue, String rateValue) {
        TextField threshold = new TextField(thresholdValue);
        threshold.setPromptText(Localization.text("Ngưỡng hóa đơn tối thiểu")
                + (Localization.isEnglish() ? " (USD)" : " (VNĐ)"));
        threshold.getStyleClass().add("text-input");
        TextField rate = new TextField(rateValue);
        rate.setPromptText(Localization.text("Tỷ lệ điểm (%)"));
        rate.getStyleClass().add("text-input");
        Button remove = new Button(Localization.text("Xóa bậc"));
        remove.getStyleClass().add("button-secondary");
        HBox row = new HBox(8, threshold, rate, remove);
        row.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(threshold, Priority.ALWAYS);
        HBox.setHgrow(rate, Priority.ALWAYS);
        TierInput tier = new TierInput(threshold, rate);
        tiers.add(tier);
        container.getChildren().add(row);
        remove.setOnAction(event -> {
            tiers.remove(tier);
            container.getChildren().remove(row);
        });
    }

    private String formatInputMoney(BigDecimal vndAmount) {
        BigDecimal displayAmount = Localization.isEnglish()
                ? exchangeRateService.convertVndToUsd(vndAmount) : vndAmount;
        String value = displayAmount.stripTrailingZeros().toPlainString();
        return Localization.isEnglish() ? value : value.replace('.', ',');
    }

    private List<String> promotionValues(
            TextField code,
            TextField name,
            TextField discount,
            DatePicker accrualStart,
            DatePicker redemptionStart,
            DatePicker accrualEnd,
            DatePicker redemptionEnd,
            TextField redemptionValue,
            List<TierInput> tiers,
            ComboBox<String> status) {
        BigDecimal redemptionVnd = toVnd(parseDecimalInput(redemptionValue), 8);
        List<String> encodedTiers = new ArrayList<>();
        for (TierInput tier : tiers) {
            BigDecimal thresholdVnd = toVnd(parseDecimalInput(tier.threshold()), 2);
            encodedTiers.add(thresholdVnd.toPlainString() + ":"
                    + parseDecimalInput(tier.rate()).toPlainString());
        }
        LocalDate start = requireDate(accrualStart, "Accrual start");
        LocalDate redemptionStartDate = requireDate(redemptionStart, "Redemption start");
        LocalDate end = requireDate(accrualEnd, "Accrual end");
        LocalDate redemptionEndDate = requireDate(redemptionEnd, "Redemption end");
        int statusIndex = status.getSelectionModel().getSelectedIndex();
        if (statusIndex < 0) {
            throw new IllegalArgumentException("Select a program status.");
        }
        return List.of(
                code.getText().trim(),
                name.getText().trim(),
                parseDecimalInput(discount).toPlainString(),
                start.toString(),
                redemptionStartDate.toString(),
                end.toString(),
                redemptionEndDate.toString(),
                redemptionVnd.toPlainString(),
                String.join(";", encodedTiers),
                statusIndex == 0 ? "ACTIVE" : "INACTIVE");
    }

    private BigDecimal toVnd(BigDecimal displayAmount, int scale) {
        BigDecimal vndAmount = Localization.isEnglish()
                ? exchangeRateService.convertUsdToVnd(displayAmount)
                : displayAmount;
        return vndAmount.setScale(scale, RoundingMode.HALF_UP);
    }

    private BigDecimal parseDecimalInput(TextField input) {
        String text = normalizeDecimalInput(input.getText());
        try {
            return new BigDecimal(text);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    Localization.text("Enter a valid decimal number: ") + text, exception);
        }
    }

    private String normalizeDecimalInput(String input) {
        String value = input == null ? "" : input.trim();
        if (Localization.isEnglish()) {
            if (value.contains(".") && value.contains(",")) {
                return value.replace(",", "");
            }
            if (value.matches("\\d{1,3}(,\\d{3})+")) {
                return value.replace(",", "");
            }
            return value.replace(',', '.');
        }
        if (value.contains(",") && value.contains(".")) {
            return value.replace(".", "").replace(',', '.');
        }
        if (value.matches("\\d{1,3}(\\.\\d{3})+")) {
            return value.replace(".", "");
        }
        return value.replace(',', '.');
    }

    private LocalDate requireDate(DatePicker picker, String field) {
        if (picker.getValue() == null) {
            throw new IllegalArgumentException(field + " is required.");
        }
        return picker.getValue();
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
        boolean promotion = "UC-18".equals(module.id());
        boolean customer = "UC-16".equals(module.id());
        alert.setTitle(Localization.text(promotion ? "Xác nhận vô hiệu hóa"
                : customer ? "Xác nhận lưu trữ" : "Xác nhận xóa"));
        alert.setHeaderText(module.title());
        alert.setContentText(Localization.text(promotion
                ? "Bạn có chắc muốn vô hiệu hóa chương trình đang chọn?"
                : customer ? "Hồ sơ được lưu trữ, lịch sử mua hàng và điểm vẫn được giữ."
                        : "Bạn có chắc muốn xóa dòng dữ liệu đang chọn?"));
        alert.getButtonTypes().setAll(ButtonType.CANCEL, ButtonType.OK);
        return alert.showAndWait().filter(ButtonType.OK::equals).isPresent();
    }

    private boolean confirmEndPromotionImmediately() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(Localization.text("Kết thúc ngay"));
        alert.setHeaderText(Localization.text("Khuyến mãi & chương trình điểm"));
        alert.setContentText(Localization.text(
                "Bạn có chắc muốn kết thúc chương trình ngay và vô hiệu hóa chương trình đó?"));
        alert.getButtonTypes().setAll(ButtonType.CANCEL, ButtonType.OK);
        return alert.showAndWait().filter(ButtonType.OK::equals).isPresent();
    }

    private void runMutation(Runnable mutation, Runnable onSuccess) {
        try {
            mutation.run();
            onSuccess.run();
        } catch (IllegalArgumentException | IllegalStateException | java.util.NoSuchElementException
                 | vn.ninhkieupharma.pharmacy.common.dao.DataAccessException exception) {
            showInformation(Localization.text("Không thể cập nhật dữ liệu"),
                    Localization.text(exception.getMessage()));
        } catch (UnsupportedOperationException exception) {
            showInformation(Localization.text("Thao tác chưa hỗ trợ"),
                    Localization.text(exception.getMessage()));
        }
    }

    private boolean isWritableModule(String moduleId) {
        return List.of("UC-01", "UC-02", "UC-07", "UC-08", "UC-09", "UC-13", "UC-16", "UC-18")
                .contains(moduleId);
    }

    private SalesCustomerEntry createSalesCustomerEntry(
            ModuleDefinition module, FeatureModuleService featureService, RenderedTable renderedTable) {
        CustomerLoyaltyService customerService = featureServices.customerLoyaltyService();
        SalesService salesService = featureServices.salesService();
        List<SaleProduct> saleProducts;
        try {
            saleProducts = salesService.findSaleProducts();
        } catch (vn.ninhkieupharma.pharmacy.common.dao.DataAccessException exception) {
            saleProducts = List.of();
            showInformation(Localization.text("Không thể tải sản phẩm bán"),
                    exception.getMessage());
        }
        ObservableList<ModuleRow> customers = FXCollections.observableArrayList();
        FilteredList<ModuleRow> filteredCustomers = new FilteredList<>(customers, customer -> true);
        ModuleRow[] selectedCustomer = {null};
        SimpleBooleanProperty hasSelectedCustomer = new SimpleBooleanProperty(false);
        boolean[] updatingSearch = {false};

        VBox form = new VBox(16);
        form.getStyleClass().add("order-form");
        VBox customerSection = new VBox(12);
        customerSection.getStyleClass().add("dashboard-panel");
        Label customerHeading = new Label(Localization.text("1. Thông tin khách hàng"));
        customerHeading.getStyleClass().add("section-title");
        Label customerStep = new Label(Localization.text(
                "Nhập tên, số điện thoại hoặc email để tìm và gắn khách hàng trước khi thêm thuốc."));
        customerStep.getStyleClass().add("helper-text");
        customerStep.setWrapText(true);

        CheckBox guestOrder = new CheckBox(Localization.text("Khách lẻ / không gắn hồ sơ"));
        guestOrder.setSelected(true);
        TextField customerSearch = new TextField();
        customerSearch.setPromptText(Localization.text("Nhập tên, số điện thoại hoặc email khách hàng"));
        customerSearch.getStyleClass().add("search-input");
        ListView<ModuleRow> suggestions = new ListView<>(filteredCustomers);
        suggestions.setPrefHeight(132);
        suggestions.setMaxHeight(190);
        suggestions.getStyleClass().add("customer-suggestions");
        suggestions.setCellFactory(list -> customerPickerCell());
        Popup suggestionsPopup = new Popup();
        suggestionsPopup.getContent().add(suggestions);
        suggestionsPopup.setAutoFix(true);
        suggestionsPopup.setAutoHide(true);
        suggestionsPopup.setHideOnEscape(true);
        Label customerDetails = new Label(Localization.text("Chưa gắn khách hàng với đơn này."));
        customerDetails.setWrapText(true);
        customerDetails.getStyleClass().add("customer-details");
        Label customerStatus = new Label();
        customerStatus.getStyleClass().add("helper-text");

        customerSearch.textProperty().addListener((observable, oldValue, query) -> {
            if (!updatingSearch[0]) {
                selectedCustomer[0] = null;
                hasSelectedCustomer.set(false);
                updateSalesCustomerDetails(null, customerDetails);
                if (query != null && !query.isBlank()) {
                    guestOrder.setSelected(false);
                }
            }
            filteredCustomers.setPredicate(customer -> matchesSalesCustomerQuery(customer, query));
            showCustomerSuggestions(suggestionsPopup, customerSearch, suggestions, filteredCustomers, query);
        });
        guestOrder.selectedProperty().addListener((observable, wasSelected, isSelected) -> {
            if (isSelected) {
                suggestionsPopup.hide();
                selectedCustomer[0] = null;
                hasSelectedCustomer.set(false);
                updatingSearch[0] = true;
                customerSearch.clear();
                updatingSearch[0] = false;
                suggestions.getSelectionModel().clearSelection();
                updateSalesCustomerDetails(null, customerDetails);
            }
        });
        suggestions.setOnMouseClicked(event -> {
            ModuleRow highlighted = suggestions.getSelectionModel().getSelectedItem();
            if (highlighted != null) {
                selectSalesCustomer(customerService, highlighted, customers, selectedCustomer,
                        hasSelectedCustomer, updatingSearch, customerSearch, suggestionsPopup, suggestions,
                        guestOrder, customerDetails, customerStatus);
            }
        });
        suggestions.setOnKeyPressed(event -> {
            if (event.getCode() == javafx.scene.input.KeyCode.ENTER) {
                ModuleRow highlighted = suggestions.getSelectionModel().getSelectedItem();
                if (highlighted != null) {
                    selectSalesCustomer(customerService, highlighted, customers, selectedCustomer,
                            hasSelectedCustomer, updatingSearch, customerSearch, suggestionsPopup, suggestions,
                            guestOrder, customerDetails, customerStatus);
                }
            }
        });
        customerSearch.setOnKeyPressed(event -> {
            if (event.getCode() == javafx.scene.input.KeyCode.DOWN
                    && suggestionsPopup.isShowing()
                    && !filteredCustomers.isEmpty()) {
                suggestions.requestFocus();
                suggestions.getSelectionModel().selectFirst();
                event.consume();
            }
        });

        Button refreshCustomers = new Button(Localization.text("Làm mới danh sách khách"));
        refreshCustomers.getStyleClass().add("button-secondary");
        refreshCustomers.setOnAction(event -> refreshSalesCustomers(
                customerService, customers, selectedCustomer, hasSelectedCustomer,
                updatingSearch, customerSearch, suggestionsPopup, guestOrder,
                customerDetails, customerStatus));
        Button addCustomer = new Button(Localization.text("Thêm khách hàng"));
        addCustomer.getStyleClass().add("button-secondary");
        addCustomer.setOnAction(event -> editSalesCustomer(
                customerService, null, customers, selectedCustomer, hasSelectedCustomer, updatingSearch,
                customerSearch, suggestionsPopup, suggestions, guestOrder, customerDetails, customerStatus));
        Button editCustomer = new Button(Localization.text("Sửa thông tin khách"));
        editCustomer.getStyleClass().add("button-secondary");
        editCustomer.disableProperty().bind(
                guestOrder.selectedProperty().or(
                        hasSelectedCustomer.not()));
        editCustomer.setOnAction(event -> {
            if (selectedCustomer[0] != null) {
                editSalesCustomer(customerService, selectedCustomer[0], customers, selectedCustomer,
                        hasSelectedCustomer, updatingSearch, customerSearch, suggestionsPopup, suggestions, guestOrder,
                        customerDetails, customerStatus);
            }
        });
        HBox customerActions = new HBox(10, addCustomer, editCustomer, refreshCustomers);
        customerActions.setAlignment(Pos.CENTER_LEFT);
        customerSection.getChildren().addAll(
                customerHeading, customerStep, guestOrder, customerSearch,
                customerDetails, customerStatus, customerActions);

        VBox prescriptionSection = new VBox(12);
        prescriptionSection.getStyleClass().add("dashboard-panel");
        Label prescriptionHeading = new Label(Localization.text("2. Thông tin đơn kê"));
        prescriptionHeading.getStyleClass().add("section-title");
        GridPane prescriptionFields = new GridPane();
        prescriptionFields.setHgap(16);
        prescriptionFields.setVgap(12);
        TextField prescriptionCode = new TextField();
        prescriptionCode.setPromptText(Localization.text("Mã đơn kê / mã toa thuốc"));
        DatePicker prescriptionDate = new DatePicker();
        prescriptionDate.setPromptText(Localization.text("Ngày kê đơn"));
        TextField doctor = new TextField();
        doctor.setPromptText(Localization.text("Họ tên bác sĩ"));
        TextField clinic = new TextField();
        clinic.setPromptText(Localization.text("Cơ sở khám chữa bệnh"));
        TextField diagnosis = new TextField();
        diagnosis.setPromptText(Localization.text("Chẩn đoán"));
        addCustomerField(prescriptionFields, 0, "Mã đơn kê", prescriptionCode);
        addCustomerField(prescriptionFields, 1, "Ngày kê", prescriptionDate);
        addCustomerField(prescriptionFields, 2, "Bác sĩ kê đơn", doctor);
        addCustomerField(prescriptionFields, 3, "Cơ sở khám", clinic);
        addCustomerField(prescriptionFields, 4, "Chẩn đoán", diagnosis);
        prescriptionSection.getChildren().addAll(
                prescriptionHeading,
                new Label(Localization.text(
                        "Bắt buộc điền mã toa, ngày kê và bác sĩ nếu có thuốc kê đơn.")),
                prescriptionFields);

        CheckoutSummary summary = createOrderSummarySection();
        List<CheckoutLineControls> saleLines = new ArrayList<>();
        Runnable refreshCheckout = () -> refreshCheckoutPreview(saleLines, summary.subtotal());
        Runnable refreshAllergyWarnings = () -> {
            updateSaleAllergyWarnings(
                    salesService, selectedCustomer[0], saleLines,
                    summary.status(), summary.completeButton());
            summary.redeemPoints().setDisable(
                    guestOrder.isSelected() || selectedCustomer[0] == null);
            refreshCheckout.run();
        };
        VBox medicationSection = createPrescriptionItemsSection(
                saleProducts, saleLines, refreshAllergyWarnings);
        customerDetails.textProperty().addListener((observable, oldValue, newValue) ->
                refreshAllergyWarnings.run());
        refreshAllergyWarnings.run();

        summary.completeButton().setOnAction(event -> {
            if (!guestOrder.isSelected() && selectedCustomer[0] == null) {
                showInformation(Localization.text("Không thể hoàn tất đơn"),
                        Localization.text("Hãy chọn một khách hàng hoặc chọn khách lẻ."));
                return;
            }
            try {
                List<SaleLineRequest> requestedLines = buildSaleLines(saleLines);
                Optional<UUID> customerId = Optional.ofNullable(selectedCustomer[0])
                        .map(ModuleRow::id);
                List<SaleAllergyWarning> warnings =
                        salesService.findAllergyWarnings(customerId, requestedLines.stream()
                                .map(SaleLineRequest::productId).distinct().toList());
                boolean allergyConfirmed = warnings.isEmpty() || confirmAllergyWarnings(warnings);
                if (!allergyConfirmed) {
                    return;
                }
                SaleOrderRequest request = new SaleOrderRequest(
                        customerId,
                        requestedLines,
                        prescriptionCode.getText().trim(),
                        prescriptionDate.getValue(),
                        doctor.getText().trim(),
                        clinic.getText().trim(),
                        diagnosis.getText().trim(),
                        summary.note().getText().trim(),
                        summary.redeemPoints().isSelected(),
                        warnings);
                SaleReceipt[] receipt = new SaleReceipt[1];
                runMutation(() -> receipt[0] = salesService.completeSale(authenticatedUserId, request), () -> {
                    refreshRows(renderedTable, featureService, module);
                    refreshSalesCustomers(customerService, customers, selectedCustomer, hasSelectedCustomer,
                            updatingSearch, customerSearch, suggestionsPopup, guestOrder,
                            customerDetails, customerStatus);
                    summary.status().setText(formatSaleReceipt(receipt[0]));
                    summary.status().getStyleClass().setAll("helper-text");
                    saleLines.forEach(line -> {
                        line.product().getSelectionModel().clearSelection();
                        line.quantity().setText("1");
                        line.dosage().clear();
                        line.treatmentDays().setText("1");
                    });
                    prescriptionCode.clear();
                    prescriptionDate.setValue(null);
                    doctor.clear();
                    clinic.clear();
                    diagnosis.clear();
                    summary.note().clear();
                    summary.redeemPoints().setSelected(false);
                    refreshAllergyWarnings.run();
                    showInformation(Localization.text("Đã hoàn tất đơn hàng"),
                            formatSaleReceipt(receipt[0]));
                });
            } catch (IllegalArgumentException | IllegalStateException
                     | vn.ninhkieupharma.pharmacy.common.dao.DataAccessException exception) {
                showInformation(Localization.text("Không thể hoàn tất đơn"),
                        Localization.text(exception.getMessage()));
            }
        });

        form.getChildren().addAll(
                customerSection, prescriptionSection, medicationSection, summary.section());
        refreshSalesCustomers(customerService, customers, selectedCustomer, hasSelectedCustomer,
                updatingSearch, customerSearch, suggestionsPopup, guestOrder,
                customerDetails, customerStatus);
        return new SalesCustomerEntry(form);
    }

    private ListCell<ModuleRow> customerPickerCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(ModuleRow customer, boolean empty) {
                super.updateItem(customer, empty);
                setText(empty || customer == null ? "" : customerPickerText(customer));
            }
        };
    }

    private void showCustomerSuggestions(
            Popup popup,
            TextField search,
            ListView<ModuleRow> suggestions,
            FilteredList<ModuleRow> filteredCustomers,
            String query) {
        if (query == null || query.isBlank() || filteredCustomers.isEmpty()
                || search.getScene() == null || search.getScene().getWindow() == null) {
            popup.hide();
            return;
        }
        Point2D location = search.localToScreen(0, search.getHeight());
        if (location == null) {
            popup.hide();
            return;
        }
        suggestions.setPrefWidth(search.getWidth());
        suggestions.setPrefHeight(Math.min(190, Math.max(42, filteredCustomers.size() * 38.0)));
        if (popup.isShowing()) {
            popup.setX(location.getX());
            popup.setY(location.getY());
        } else {
            popup.show(search.getScene().getWindow(), location.getX(), location.getY());
            popup.getScene().getStylesheets().setAll(search.getScene().getStylesheets());
        }
    }

    static boolean matchesSalesCustomerQuery(ModuleRow customer, String query) {
        String normalizedQuery = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        if (normalizedQuery.isEmpty()) {
            return true;
        }
        String normalizedPhoneQuery = query.replaceAll("[\\s().-]", "");
        List<String> values = customer.values();
        return values.get(0).toLowerCase(Locale.ROOT).contains(normalizedQuery)
                || values.get(1).replaceAll("[\\s().-]", "").contains(normalizedPhoneQuery)
                || values.get(2).toLowerCase(Locale.ROOT).contains(normalizedQuery);
    }

    private void refreshSalesCustomers(
            CustomerLoyaltyService customerService,
            ObservableList<ModuleRow> customers,
            ModuleRow[] selectedCustomer,
            SimpleBooleanProperty hasSelectedCustomer,
            boolean[] updatingSearch,
            TextField customerSearch,
            Popup suggestionsPopup,
            CheckBox guestOrder,
            Label customerDetails,
            Label customerStatus) {
        try {
            List<ModuleRow> availableCustomers = customerService.findAvailableForSale();
            customers.setAll(availableCustomers);
            if (selectedCustomer[0] != null) {
                selectedCustomer[0] = availableCustomers.stream()
                        .filter(customer -> customer.id().equals(selectedCustomer[0].id()))
                        .findFirst()
                        .orElse(null);
                if (selectedCustomer[0] == null) {
                    hasSelectedCustomer.set(false);
                    suggestionsPopup.hide();
                    guestOrder.setSelected(true);
                    updateSalesCustomerDetails(null, customerDetails);
                    customerStatus.setText(Localization.text(
                            "Khách hàng đã đổi trạng thái và không còn đủ điều kiện bán."));
                    customerStatus.getStyleClass().setAll("form-error");
                    return;
                }
                hasSelectedCustomer.set(true);
                updatingSearch[0] = true;
                customerSearch.setText(customerPickerText(selectedCustomer[0]));
                updatingSearch[0] = false;
                suggestionsPopup.hide();
                updateSalesCustomerDetails(selectedCustomer[0], customerDetails);
            }
            customerStatus.setText(Localization.text("Danh sách khách hàng đã được cập nhật."));
            customerStatus.getStyleClass().setAll("helper-text");
        } catch (vn.ninhkieupharma.pharmacy.common.dao.DataAccessException
                 | UnsupportedOperationException exception) {
            customerStatus.setText(exception.getMessage());
            customerStatus.getStyleClass().setAll("form-error");
        }
    }

    private void selectSalesCustomer(
            CustomerLoyaltyService customerService,
            ModuleRow candidate,
            ObservableList<ModuleRow> customers,
            ModuleRow[] selectedCustomer,
            SimpleBooleanProperty hasSelectedCustomer,
            boolean[] updatingSearch,
            TextField customerSearch,
            Popup suggestionsPopup,
            ListView<ModuleRow> suggestions,
            CheckBox guestOrder,
            Label customerDetails,
            Label customerStatus) {
        try {
            List<ModuleRow> availableCustomers = customerService.findAvailableForSale();
            customers.setAll(availableCustomers);
            ModuleRow eligibleCustomer = availableCustomers.stream()
                    .filter(customer -> customer.id().equals(candidate.id()))
                    .findFirst()
                    .orElse(null);
            if (eligibleCustomer == null) {
                selectedCustomer[0] = null;
                hasSelectedCustomer.set(false);
                suggestionsPopup.hide();
                updateSalesCustomerDetails(null, customerDetails);
                customerStatus.setText(Localization.text(
                        "Khách hàng đã đổi trạng thái và không còn đủ điều kiện bán."));
                customerStatus.getStyleClass().setAll("form-error");
                return;
            }
            selectedCustomer[0] = eligibleCustomer;
            hasSelectedCustomer.set(true);
            guestOrder.setSelected(false);
            updatingSearch[0] = true;
            customerSearch.setText(customerPickerText(eligibleCustomer));
            updatingSearch[0] = false;
            suggestionsPopup.hide();
            suggestions.getSelectionModel().clearSelection();
            updateSalesCustomerDetails(eligibleCustomer, customerDetails);
            customerStatus.setText(Localization.text("Khách hàng đã được gắn với đơn kê."));
            customerStatus.getStyleClass().setAll("helper-text");
        } catch (vn.ninhkieupharma.pharmacy.common.dao.DataAccessException
                 | UnsupportedOperationException exception) {
            suggestionsPopup.hide();
            customerStatus.setText(exception.getMessage());
            customerStatus.getStyleClass().setAll("form-error");
        }
    }

    private VBox createPrescriptionItemsSection(
            List<SaleProduct> catalogProducts,
            List<CheckoutLineControls> lines,
            Runnable onChange) {
        VBox section = new VBox(12);
        section.getStyleClass().add("dashboard-panel");
        Label heading = new Label(Localization.text("3. Sản phẩm trong đơn"));
        heading.getStyleClass().add("section-title");
        Label note = new Label(Localization.text(
                "Chọn sản phẩm đã có giá bán; nhập liều dùng, số lượng và số ngày điều trị để kiểm tra hạn lô."));
        note.getStyleClass().add("helper-text");
        VBox items = new VBox(8);
        Button addLine = new Button(Localization.text("Thêm dòng thuốc"));
        addLine.getStyleClass().add("button-secondary");
        addLine.setOnAction(event -> addCheckoutLine(catalogProducts, lines, items, onChange));
        section.getChildren().addAll(heading, note, items, addLine);
        addCheckoutLine(catalogProducts, lines, items, onChange);
        return section;
    }

    private CheckoutSummary createOrderSummarySection() {
        VBox section = new VBox(12);
        section.getStyleClass().add("dashboard-panel");
        Label heading = new Label(Localization.text("4. Ưu đãi và hoàn tất đơn"));
        heading.getStyleClass().add("section-title");
        TextArea notes = new TextArea();
        notes.setPromptText(Localization.text("Ghi chú thêm cho đơn thuốc hoặc khách hàng"));
        notes.setPrefRowCount(2);
        notes.setWrapText(true);
        notes.getStyleClass().add("text-input");
        CheckBox redeemPoints = new CheckBox(Localization.text(
                "Dùng điểm tối đa có thể (không vượt quá 50% giá trị gốc đơn hàng)"));
        Label subtotal = new Label(Localization.text("Tạm tính: 0 ₫"));
        subtotal.getStyleClass().add("invoice-total");
        Label status = new Label(Localization.text(
                "Ưu đãi cố định đủ điều kiện tốt nhất sẽ được áp dụng tự động."));
        status.setWrapText(true);
        status.getStyleClass().add("helper-text");
        Button completeOrder = new Button(Localization.text("Hoàn tất bán"));
        completeOrder.getStyleClass().add("button-primary");
        completeOrder.setMaxWidth(Double.MAX_VALUE);
        HBox actions = new HBox(10, completeOrder);
        actions.setAlignment(Pos.CENTER_RIGHT);
        section.getChildren().addAll(heading, subtotal, redeemPoints, notes, status, actions);
        return new CheckoutSummary(section, redeemPoints, notes, subtotal, status, completeOrder);
    }

    private void addCheckoutLine(
            List<SaleProduct> catalogProducts,
            List<CheckoutLineControls> lines,
            VBox container,
            Runnable onChange) {
        ComboBox<SaleProduct> product = new ComboBox<>(FXCollections.observableArrayList(catalogProducts));
        product.setPromptText(Localization.text("Chọn sản phẩm"));
        product.setMaxWidth(Double.MAX_VALUE);
        product.setPrefWidth(290);
        product.setConverter(new StringConverter<>() {
            @Override
            public String toString(SaleProduct item) {
                return item == null ? "" : saleProductLabel(item);
            }

            @Override
            public SaleProduct fromString(String value) {
                return null;
            }
        });
        product.setButtonCell(saleProductCell());
        product.setCellFactory(list -> saleProductCell());

        TextField dosage = new TextField();
        dosage.setPromptText(Localization.text("Liều dùng / cách dùng"));
        dosage.setPrefWidth(210);
        TextField quantity = new TextField("1");
        quantity.setPromptText(Localization.text("Số lượng"));
        quantity.setPrefWidth(84);
        TextField treatmentDays = new TextField("1");
        treatmentDays.setPromptText(Localization.text("Ngày điều trị"));
        treatmentDays.setPrefWidth(94);
        Label allergyWarning = new Label();
        allergyWarning.setWrapText(true);
        allergyWarning.getStyleClass().add("allergy-warning");
        allergyWarning.setVisible(false);
        allergyWarning.setManaged(false);

        Button remove = new Button("−");
        remove.getStyleClass().add("button-secondary");
        HBox row = new HBox(8, product, dosage, quantity, treatmentDays, remove);
        row.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(product, Priority.ALWAYS);
        HBox.setHgrow(dosage, Priority.ALWAYS);
        VBox line = new VBox(4, row, allergyWarning);
        line.getStyleClass().add("checkout-line");
        CheckoutLineControls controls = new CheckoutLineControls(
                product, quantity, dosage, treatmentDays, allergyWarning, line);
        lines.add(controls);
        container.getChildren().add(line);
        remove.setOnAction(event -> {
            lines.remove(controls);
            container.getChildren().remove(line);
            onChange.run();
        });
        product.valueProperty().addListener((observable, oldValue, newValue) -> onChange.run());
        quantity.textProperty().addListener((observable, oldValue, newValue) -> onChange.run());
        treatmentDays.textProperty().addListener((observable, oldValue, newValue) -> onChange.run());
        onChange.run();
    }

    private ListCell<SaleProduct> saleProductCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(SaleProduct product, boolean empty) {
                super.updateItem(product, empty);
                setText(empty || product == null ? "" : saleProductLabel(product));
            }
        };
    }

    private String saleProductLabel(SaleProduct product) {
        String price = product.sellingPrice() == null
                ? Localization.text("Chưa đặt giá")
                : formatSaleMoney(product.sellingPrice());
        return product.code() + " · " + product.name() + " · " + price
                + " · " + Localization.text("Tồn: ")
                + product.stockOnHand().stripTrailingZeros().toPlainString();
    }

    private List<SaleLineRequest> buildSaleLines(List<CheckoutLineControls> controls) {
        if (controls.isEmpty()) {
            throw new IllegalArgumentException(Localization.text("Vui lòng thêm ít nhất một sản phẩm vào giỏ."));
        }
        List<SaleLineRequest> lines = new ArrayList<>();
        for (CheckoutLineControls line : controls) {
            SaleProduct product = line.product().getValue();
            if (product == null) {
                throw new IllegalArgumentException(Localization.text("Hãy chọn sản phẩm cho mỗi dòng."));
            }
            if (product.sellingPrice() == null || product.sellingPrice().signum() <= 0) {
                throw new IllegalArgumentException(
                        Localization.text("Sản phẩm chưa có giá bán hợp lệ. Hãy cập nhật giá trong quản lý sản phẩm."));
            }
            BigDecimal quantity;
            int treatmentDays;
            try {
                quantity = new BigDecimal(line.quantity().getText().trim().replace(',', '.'));
                treatmentDays = Integer.parseInt(line.treatmentDays().getText().trim());
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException(
                        Localization.text("Số lượng và ngày điều trị phải là giá trị số hợp lệ."), exception);
            }
            lines.add(new SaleLineRequest(
                    product.id(), quantity, line.dosage().getText().trim(), treatmentDays));
        }
        return List.copyOf(lines);
    }

    private void refreshCheckoutPreview(List<CheckoutLineControls> lines, Label subtotalLabel) {
        BigDecimal subtotal = BigDecimal.ZERO;
        boolean invalidQuantity = false;
        for (CheckoutLineControls line : lines) {
            SaleProduct product = line.product().getValue();
            if (product == null || product.sellingPrice() == null) {
                continue;
            }
            try {
                BigDecimal quantity = new BigDecimal(line.quantity().getText().trim().replace(',', '.'));
                if (quantity.signum() > 0) {
                    subtotal = subtotal.add(product.sellingPrice().multiply(quantity));
                }
            } catch (NumberFormatException ignored) {
                invalidQuantity = true;
            }
        }
        subtotalLabel.setText(invalidQuantity
                ? Localization.text("Nhập số lượng hợp lệ để xem tạm tính.")
                : Localization.text("Tạm tính (chưa trừ ưu đãi/điểm): ")
                        + formatSaleMoney(subtotal.setScale(2, RoundingMode.HALF_UP)));
    }

    private void updateSaleAllergyWarnings(
            SalesService salesService,
            ModuleRow customer,
            List<CheckoutLineControls> lines,
            Label status,
            Button completeButton) {
        try {
            Optional<UUID> customerId = Optional.ofNullable(customer).map(ModuleRow::id);
            List<UUID> productIds = lines.stream()
                    .map(line -> line.product().getValue())
                    .filter(Objects::nonNull)
                    .map(SaleProduct::id)
                    .distinct()
                    .toList();
            List<SaleAllergyWarning> warnings =
                    salesService.findAllergyWarnings(customerId, productIds);
            for (CheckoutLineControls line : lines) {
                List<SaleAllergyWarning> productWarnings = warnings.stream()
                        .filter(warning -> line.product().getValue() != null
                                && warning.productId().equals(line.product().getValue().id()))
                        .toList();
                boolean hasWarning = !productWarnings.isEmpty();
                line.allergyWarning().setVisible(hasWarning);
                line.allergyWarning().setManaged(hasWarning);
                line.row().getStyleClass().remove("allergy-warning-line");
                if (hasWarning) {
                    line.row().getStyleClass().add("allergy-warning-line");
                    line.allergyWarning().setText(productWarnings.stream()
                            .map(warning -> Localization.text("Dị ứng: ")
                                    + warning.allergenName() + " · "
                                    + Localization.text("Thành phần: ")
                                    + warning.ingredientName())
                            .distinct()
                            .collect(java.util.stream.Collectors.joining("\n")));
                } else {
                    line.allergyWarning().setText("");
                }
            }
            completeButton.setDisable(false);
            status.getStyleClass().setAll("helper-text");
        } catch (vn.ninhkieupharma.pharmacy.common.dao.DataAccessException exception) {
            status.setText(exception.getMessage());
            status.getStyleClass().setAll("form-error");
            completeButton.setDisable(true);
        }
    }

    private boolean confirmAllergyWarnings(List<SaleAllergyWarning> warnings) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(Localization.text("Cảnh báo dị ứng"));
        alert.setHeaderText(Localization.text("Có thành phần trùng với dị ứng đã khai báo"));
        alert.setContentText(Localization.text(
                "Chỉ tiếp tục sau khi đã kiểm tra và xác nhận tư vấn với khách hàng:\n")
                + warnings.stream()
                        .map(warning -> warning.productName() + " — "
                                + warning.ingredientName() + " (" + warning.allergenName() + ")")
                        .distinct()
                        .collect(java.util.stream.Collectors.joining("\n")));
        alert.getButtonTypes().setAll(ButtonType.CANCEL, ButtonType.OK);
        return alert.showAndWait().filter(ButtonType.OK::equals).isPresent();
    }

    private String formatSaleReceipt(SaleReceipt receipt) {
        return Localization.text("Mã đơn: ") + receipt.saleCode()
                + "\n" + Localization.text("Tạm tính: ") + formatSaleMoney(receipt.subtotal())
                + "\n" + Localization.text("Giảm khuyến mãi: ") + formatSaleMoney(receipt.promotionDiscount())
                + (receipt.promotionCode().isBlank() ? "" : " (" + receipt.promotionCode() + ")")
                + "\n" + Localization.text("Giảm bằng điểm: ") + formatSaleMoney(receipt.pointsDiscount())
                + "\n" + Localization.text("Tổng thanh toán: ") + formatSaleMoney(receipt.total())
                + "\n" + Localization.text("Điểm đã dùng / tích lũy / còn lại: ")
                + receipt.pointsRedeemed() + " / " + receipt.pointsEarned() + " / " + receipt.remainingPoints();
    }

    private String formatSaleMoney(BigDecimal amount) {
        NumberFormat formatter = NumberFormat.getNumberInstance(
                Localization.isEnglish() ? Locale.US : Locale.forLanguageTag("vi-VN"));
        formatter.setMaximumFractionDigits(2);
        formatter.setMinimumFractionDigits(amount.stripTrailingZeros().scale() > 0 ? 2 : 0);
        return formatter.format(amount) + " ₫";
    }

    private String customerPickerText(ModuleRow customer) {
        String phone = customer.values().get(1);
        String email = customer.values().get(2);
        String contact = !phone.isBlank() ? phone : email;
        return customer.values().get(0) + (contact.isBlank() ? "" : " · " + contact);
    }

    private void updateSalesCustomerDetails(ModuleRow customer, Label details) {
        if (customer == null) {
            details.setText(Localization.text("Chưa gắn khách hàng với đơn này."));
            return;
        }
        List<String> values = customer.values();
        String phone = values.get(1).isBlank()
                ? Localization.text("Chưa có số điện thoại")
                : values.get(1);
        String email = values.get(2).isBlank()
                ? Localization.text("Chưa có email")
                : values.get(2);
        String allergies = values.get(3).isBlank()
                ? Localization.text("Chưa ghi nhận")
                : values.get(3);
        String age = values.size() <= 5 || values.get(5).isBlank()
                ? Localization.text("Chưa có ngày sinh")
                : Integer.toString(java.time.Period.between(
                        LocalDate.parse(values.get(5)), LocalDate.now()).getYears());
        String points = NumberFormat.getIntegerInstance(Localization.isEnglish()
                        ? Locale.US : Locale.forLanguageTag("vi-VN"))
                .format(Long.parseLong(values.get(4)));
        details.setText(Localization.text("Tên: ") + values.get(0)
                + "\n" + Localization.text("Điện thoại: ") + phone
                + "\n" + Localization.text("Email: ") + email
                + "\n" + Localization.text("Tuổi: ") + age
                + "\n" + Localization.text("Dị ứng: ") + allergies
                + "\n" + Localization.text("Điểm tích lũy hiện có: ") + points);
        if (values.get(3).isBlank()) {
            details.getStyleClass().remove("form-error");
        } else if (!details.getStyleClass().contains("form-error")) {
            details.getStyleClass().add("form-error");
        }
    }

    private void editSalesCustomer(
            CustomerLoyaltyService customerService,
            ModuleRow selectedCustomer,
            ObservableList<ModuleRow> customers,
            ModuleRow[] selectedCustomerState,
            SimpleBooleanProperty hasSelectedCustomer,
            boolean[] updatingSearch,
            TextField customerSearch,
            Popup suggestionsPopup,
            ListView<ModuleRow> suggestions,
            CheckBox guestOrder,
            Label customerDetails,
            Label customerStatus) {
        ModuleRow draft = selectedCustomer;
        while (true) {
            java.util.Optional<List<String>> submitted = createSalesCustomerProfileDialog(draft);
            if (submitted.isEmpty()) {
                return;
            }
            List<String> values = submitted.orElseThrow();
            try {
                ModuleRow savedCustomer = selectedCustomer == null
                        ? customerService.createFromSale(values)
                        : customerService.updateFromSale(selectedCustomer.id(), values);
                customers.removeIf(customer -> customer.id().equals(savedCustomer.id()));
                customers.add(savedCustomer);
                customers.sort(java.util.Comparator.comparing(
                        customer -> customer.values().get(0), String.CASE_INSENSITIVE_ORDER));
                selectSalesCustomer(customerService, savedCustomer, customers, selectedCustomerState,
                        hasSelectedCustomer, updatingSearch, customerSearch, suggestionsPopup, suggestions,
                        guestOrder, customerDetails, customerStatus);
                return;
            } catch (IllegalArgumentException | IllegalStateException | java.util.NoSuchElementException
                     | vn.ninhkieupharma.pharmacy.common.dao.DataAccessException
                     | UnsupportedOperationException exception) {
                showInformation(Localization.text("Không thể cập nhật khách hàng"),
                        Localization.text(exception.getMessage()));
                draft = new ModuleRow(
                        selectedCustomer == null ? UUID.randomUUID() : selectedCustomer.id(),
                        salesCustomerDraft(values,
                                selectedCustomer == null ? "0" : selectedCustomer.values().get(4)));
            }
        }
    }

    private java.util.Optional<List<String>> createSalesCustomerProfileDialog(ModuleRow existingCustomer) {
        List<String> oldValues = existingCustomer == null ? List.of() : existingCustomer.values();
        Dialog<List<String>> dialog = new Dialog<>();
        dialog.setTitle(Localization.text(existingCustomer == null ? "Thêm khách hàng" : "Sửa thông tin khách hàng"));
        dialog.setHeaderText(Localization.text("Thông tin khách hàng dùng tại quầy"));
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.getDialogPane().getButtonTypes().setAll(ButtonType.CANCEL, ButtonType.OK);
        dialog.getDialogPane().getStyleClass().add("form-dialog");

        TextField name = customerTextField(oldValues, 0);
        TextField phone = customerTextField(oldValues, 1);
        TextField email = customerTextField(oldValues, 2);
        DatePicker dateOfBirth = new DatePicker();
        if (oldValues.size() > 5 && !oldValues.get(5).isBlank()) {
            dateOfBirth.setValue(LocalDate.parse(oldValues.get(5)));
        }
        TextArea allergies = new TextArea(oldValues.size() > 3 ? oldValues.get(3) : "");
        allergies.setPromptText(Localization.text("Mỗi dòng: tên dị ứng | ghi chú tùy chọn"));
        allergies.setPrefRowCount(4);
        allergies.setWrapText(true);
        allergies.getStyleClass().add("text-input");

        GridPane fields = new GridPane();
        fields.setHgap(16);
        fields.setVgap(12);
        fields.setPadding(new Insets(12, 0, 8, 0));
        addCustomerField(fields, 0, "Họ và tên", name);
        addCustomerField(fields, 1, "Điện thoại", phone);
        addCustomerField(fields, 2, "Email", email);
        addCustomerField(fields, 3, "Ngày sinh", dateOfBirth);
        addCustomerField(fields, 4, "Dị ứng", allergies);
        if (oldValues.size() > 4) {
            Label loyaltyPoints = new Label(Localization.text("Điểm hiện có: ") + oldValues.get(4));
            loyaltyPoints.getStyleClass().add("helper-text");
            addCustomerField(fields, 5, "Điểm", loyaltyPoints);
        }
        Label validationMessage = new Label();
        validationMessage.getStyleClass().add("form-error");
        validationMessage.setWrapText(true);
        VBox content = new VBox(12, fields, new Label(Localization.text(
                "Nhân viên không thể thay đổi điểm tích lũy hoặc trạng thái hạn chế bán.")), validationMessage);
        content.getChildren().get(1).getStyleClass().add("helper-text");
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setPrefWidth(620);

        javafx.scene.Node okButton = dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.addEventFilter(ActionEvent.ACTION, event -> {
            String validationError = validateCustomerForm(name, phone, email, new CheckBox(), new TextField());
            if (dateOfBirth.getValue() != null && dateOfBirth.getValue().isAfter(LocalDate.now())) {
                validationError = "Ngày sinh không thể ở tương lai.";
            }
            if (!validationError.isEmpty()) {
                validationMessage.setText(Localization.text(validationError));
                event.consume();
            }
        });
        dialog.setResultConverter(button -> button == ButtonType.OK
                ? List.of(name.getText().trim(), phone.getText().trim(),
                        email.getText().trim(), allergies.getText().trim(),
                        dateOfBirth.getValue() == null ? "" : dateOfBirth.getValue().toString())
                : null);
        return dialog.showAndWait();
    }

    private List<String> salesCustomerDraft(List<String> values, String points) {
        return List.of(values.get(0), values.get(1), values.get(2), values.get(3), points, values.get(4));
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
