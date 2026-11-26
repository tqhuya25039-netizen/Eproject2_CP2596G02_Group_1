package vn.ninhkieupharma.pharmacy.ui;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Point2D;
import javafx.print.PrinterJob;
import javafx.scene.Node;
import javafx.scene.Scene;
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
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuButton;
import javafx.scene.control.PasswordField;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.TextInputControl;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Popup;
import javafx.stage.FileChooser;
import vn.ninhkieupharma.pharmacy.common.model.ModuleRow;
import vn.ninhkieupharma.pharmacy.common.service.FeatureModuleService;
import vn.ninhkieupharma.pharmacy.common.service.FeatureServices;
import vn.ninhkieupharma.pharmacy.common.service.ExchangeRateService;
import vn.ninhkieupharma.pharmacy.common.model.ModuleDefinition;
import vn.ninhkieupharma.pharmacy.common.service.DemoData;
import vn.ninhkieupharma.pharmacy.common.service.Localization;
import vn.ninhkieupharma.pharmacy.common.service.ModuleCatalog;
import vn.ninhkieupharma.pharmacy.common.auth.UserRole;
import vn.ninhkieupharma.pharmacy.common.dao.InventoryWorkflowDAO;
import vn.ninhkieupharma.pharmacy.domain.inventory.InventoryWorkflowService;
import vn.ninhkieupharma.pharmacy.domain.hr.AvailabilityService;
import vn.ninhkieupharma.pharmacy.domain.hr.AvailabilitySlot;
import vn.ninhkieupharma.pharmacy.domain.hr.AttendanceService;
import vn.ninhkieupharma.pharmacy.domain.sales.PromotionsLoyaltyService;
import vn.ninhkieupharma.pharmacy.domain.sales.CustomerLoyaltyService;
import vn.ninhkieupharma.pharmacy.domain.sales.SaleAllergyWarning;
import vn.ninhkieupharma.pharmacy.domain.sales.SaleLineRequest;
import vn.ninhkieupharma.pharmacy.domain.sales.SaleOrderRequest;
import vn.ninhkieupharma.pharmacy.domain.sales.SalePendingOrder;
import vn.ninhkieupharma.pharmacy.domain.sales.SaleQuote;
import vn.ninhkieupharma.pharmacy.domain.sales.SaleProduct;
import vn.ninhkieupharma.pharmacy.domain.sales.SaleReceipt;
import vn.ninhkieupharma.pharmacy.domain.sales.SalesReport;
import vn.ninhkieupharma.pharmacy.domain.sales.SalesService;

import java.awt.Desktop;
import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.IntStream;
import javafx.util.StringConverter;

public class PageRenderer {
    private static final DateTimeFormatter VIETNAMESE_DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter VIETNAMESE_DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter TIME_INPUT_FORMAT =
            DateTimeFormatter.ofPattern("H:mm");
    private static final DateTimeFormatter ISO_DATE_TIME_WITH_SPACE =
            new DateTimeFormatterBuilder()
                    .appendPattern("yyyy-MM-dd HH:mm")
                    .optionalStart()
                    .appendPattern(":ss")
                    .optionalEnd()
                    .toFormatter();

    private record RenderedTable(
            TableView<ModuleRow> table,
            ObservableList<ModuleRow> source,
            FilteredList<ModuleRow> filteredRows) {
    }

    private record ProductCard(DemoData.Product product, Node node, Button addButton) {
    }

    private record SalesCustomerEntry(Node node, Runnable loadSelectedPending) {
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
            TextField requestedPoints,
            TextArea note,
            Label subtotal,
            Label status,
            Label stockStatus,
            Button checkStockButton,
            Button savePendingButton,
            Button completeButton,
            Button cancelPendingButton,
            Button newOrderButton) {
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

    private record ReportFilters(VBox container, List<Node> fields, Button applyButton) {
    }

    record TrendChartData(
            List<String> periods,
            Map<String, List<Number>> series,
            List<String> dimensions,
            List<Number> latestPeriodTotals) {
    }

    private record ProductFilterOption(String value, String label) {
        @Override
        public String toString() {
            return label;
        }
    }

    private record ProductFilters(
            GridPane container,
            List<Integer> columnIndexes,
            List<Node> fields,
            Map<Integer, ComboBox<ProductFilterOption>> selectors,
            TextField minimumPrice,
            TextField maximumPrice) {
    }

    private record InventoryFilters(
            VBox container,
            DatePicker movementFrom,
            DatePicker movementTo,
            ComboBox<String> movementType,
            ComboBox<String> expiryStatus,
            ComboBox<String> location,
            Button applyButton) {
    }

    private record ReceiptLineInput(
            ComboBox<InventoryWorkflowDAO.ProductOption> product,
            TextField batchCode,
            DatePicker expiryDate,
            ComboBox<InventoryWorkflowDAO.LocationOption> location,
            TextField quantity,
            TextField unitCost,
            Node row) {
    }

    private record InventoryLossInput(
            long batchId, InventoryWorkflowDAO.LossType lossType, BigDecimal quantity, String note) {
    }

    private final FeatureServices featureServices;
    private final ExchangeRateService exchangeRateService;
    private final long authenticatedUserId;
    private final UserRole userRole;

    public PageRenderer(FeatureServices featureServices, ExchangeRateService exchangeRateService) {
        this(featureServices, exchangeRateService, -1, UserRole.STAFF);
    }

    public PageRenderer(
            FeatureServices featureServices, ExchangeRateService exchangeRateService, long authenticatedUserId) {
        this(featureServices, exchangeRateService, authenticatedUserId, UserRole.STAFF);
    }

    public PageRenderer(
            FeatureServices featureServices, ExchangeRateService exchangeRateService,
            long authenticatedUserId, UserRole userRole) {
        this.featureServices = Objects.requireNonNull(featureServices);
        this.exchangeRateService = Objects.requireNonNull(exchangeRateService);
        this.authenticatedUserId = authenticatedUserId;
        this.userRole = Objects.requireNonNull(userRole);
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

        if (userRole != UserRole.ADMIN && !module.staffAccessible()) {
            page.getChildren().addAll(title, description, new Label(
                    Localization.text("Chức năng này chỉ dành cho quản trị viên.")));
            return page;
        }
        if ("UC-19".equals(module.id())) {
            page.getChildren().addAll(title, description);
            configureSalesReport(page, module);
            return page;
        }

        FeatureModuleService featureService = featureServices.forModule(module);
        TextField search = new TextField();
        search.setPromptText(Localization.text("Tìm theo mã, tên hoặc nội dung..."));
        search.getStyleClass().add("search-input");
        HBox.setHgrow(search, Priority.ALWAYS);

        RenderedTable renderedTable = createTable(module, featureService, featureService.findAll(module));
        SalesCustomerEntry salesCustomerEntry = "UC-17".equals(module.id())
                && userRole == UserRole.STAFF
                ? createSalesCustomerEntry(module, featureService, renderedTable)
                : null;
        List<Node> filterInputs = new ArrayList<>();
        PromotionFilters promotionFilters = null;
        CustomerFilters customerFilters = null;
        InventoryFilters inventoryFilters = null;
        ReportFilters reportFilters = null;
        ProductFilters productFilters = null;
        ComboBox<String> workflowStatusFilter = null;
        if ("UC-12".equals(module.id()) || "UC-14".equals(module.id())) {
            workflowStatusFilter = new ComboBox<>(FXCollections.observableArrayList(
                    Localization.text("Tất cả trạng thái"),
                    Localization.text("Chưa xác nhận"),
                    Localization.text("Đã xác nhận"),
                    Localization.text("Đã hủy")));
            workflowStatusFilter.getSelectionModel().selectFirst();
        }
        Node filters = null;
        if (module.report()) {
            if ("UC-15".equals(module.id())) {
                inventoryFilters = createInventoryFilters();
                filters = inventoryFilters.container();
                InventoryFilters capturedFilters = inventoryFilters;
                capturedFilters.applyButton().setOnAction(event -> applyInventoryFilters(
                        renderedTable.filteredRows(), search, capturedFilters));
            } else if ("UC-10".equals(module.id()) || "UC-11".equals(module.id())) {
                reportFilters = createReportFilters(module);
                filters = reportFilters.container();
                ReportFilters capturedFilters = reportFilters;
                capturedFilters.applyButton().setOnAction(event -> applyReportFilters(
                        renderedTable.filteredRows(), search, capturedFilters));
            } else {
                filters = createFilters(module, filterInputs, renderedTable.filteredRows(), search);
            }
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
        } else if ("UC-08".equals(module.id())) {
            productFilters = createProductFilters(module, renderedTable.source());
            filters = productFilters.container();
            ProductFilters capturedFilters = productFilters;
            capturedFilters.fields().forEach(field -> {
                if (field instanceof TextInputControl textInput) {
                    textInput.textProperty().addListener(
                            (observable, oldValue, newValue) -> applyProductFilters(
                                    renderedTable.filteredRows(), search, capturedFilters));
                } else if (field instanceof ComboBox<?> comboBox) {
                    comboBox.valueProperty().addListener(
                            (observable, oldValue, newValue) -> applyProductFilters(
                                    renderedTable.filteredRows(), search, capturedFilters));
                }
            });
            capturedFilters.minimumPrice().textProperty().addListener(
                    (observable, oldValue, newValue) -> applyProductFilters(
                            renderedTable.filteredRows(), search, capturedFilters));
            capturedFilters.maximumPrice().textProperty().addListener(
                    (observable, oldValue, newValue) -> applyProductFilters(
                            renderedTable.filteredRows(), search, capturedFilters));
            renderedTable.source().addListener((ListChangeListener<ModuleRow>) change ->
                    refreshProductFilterOptions(capturedFilters, renderedTable.source()));
        }
        if (renderedTable != null) {
            PromotionFilters capturedFilters = promotionFilters;
            CustomerFilters capturedCustomerFilters = customerFilters;
            InventoryFilters capturedInventoryFilters = inventoryFilters;
            ReportFilters capturedReportFilters = reportFilters;
            ProductFilters capturedProductFilters = productFilters;
            ComboBox<String> capturedWorkflowStatusFilter = workflowStatusFilter;
            search.textProperty().addListener((observable, oldValue, newValue) -> {
                if (capturedWorkflowStatusFilter != null) {
                    applyStatusFilters(
                            renderedTable.filteredRows(), search, capturedWorkflowStatusFilter);
                } else if (capturedInventoryFilters != null) {
                    applyInventoryFilters(
                            renderedTable.filteredRows(), search, capturedInventoryFilters);
                } else if (capturedReportFilters != null) {
                    applyReportFilters(
                            renderedTable.filteredRows(), search, capturedReportFilters);
                } else if (capturedProductFilters != null) {
                    applyProductFilters(
                            renderedTable.filteredRows(), search, capturedProductFilters);
                } else if (capturedFilters == null) {
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
            if (capturedWorkflowStatusFilter != null) {
                capturedWorkflowStatusFilter.valueProperty().addListener(
                        (observable, oldValue, newValue) -> applyStatusFilters(
                                renderedTable.filteredRows(), search, capturedWorkflowStatusFilter));
            }
        }

        FlowPane actions = new FlowPane(12, 8);
        actions.setAlignment(Pos.CENTER_LEFT);
        actions.setMaxWidth(Double.MAX_VALUE);
        actions.getStyleClass().add("action-bar");
        search.setMinWidth(260);
        search.setPrefWidth(420);
        search.setMaxWidth(Double.MAX_VALUE);
        actions.getChildren().add(search);
        if (workflowStatusFilter != null) {
            actions.getChildren().add(workflowStatusFilter);
        }
        if (module.report()) {
            Button exportButton = new Button(Localization.text("Xuất báo cáo"));
            exportButton.getStyleClass().add("button-secondary");
            exportButton.setOnAction(event -> exportReport(
                    module, renderedTable.filteredRows(), renderedTable.table().getScene().getWindow()));
            actions.getChildren().add(exportButton);
            Button refreshButton = new Button(Localization.text("Làm mới"));
            refreshButton.getStyleClass().add("button-secondary");
            InventoryFilters capturedInventoryFilters = inventoryFilters;
            ComboBox<String> capturedRefreshStatusFilter = workflowStatusFilter;
            refreshButton.setOnAction(event -> {
                refreshRows(renderedTable, featureService, module);
                if (capturedInventoryFilters != null) {
                    applyInventoryFilters(renderedTable.filteredRows(), search, capturedInventoryFilters);
                } else if (capturedRefreshStatusFilter != null) {
                    applyStatusFilters(
                            renderedTable.filteredRows(), search, capturedRefreshStatusFilter);
                } else {
                    applyFilters(renderedTable.filteredRows(), search, filterInputs);
                }
            });
            actions.getChildren().add(refreshButton);
            if ("UC-15".equals(module.id())) {
                Button lossButton = new Button(Localization.text("Ghi nhận hư hỏng / hết hạn"));
                lossButton.getStyleClass().add("button-primary");
                lossButton.setOnAction(event -> createInventoryLossDialog()
                        .ifPresent(input -> runMutation(
                                () -> featureServices.inventoryWorkflowService().recordInventoryLoss(
                                        authenticatedUserId, input.batchId(), input.lossType(),
                                        input.quantity(), input.note()),
                                () -> refreshRows(renderedTable, featureService, module))));
                actions.getChildren().add(lossButton);
            }
        } else if ("UC-17".equals(module.id())) {
            Button refreshSales = new Button(Localization.text("Làm mới"));
            refreshSales.getStyleClass().add("button-secondary");
            refreshSales.setOnAction(event -> refreshSalesRows(renderedTable, featureService, module));
            Button detailsButton = new Button(Localization.text("Xem chi tiết"));
            detailsButton.getStyleClass().add("button-secondary");
            detailsButton.disableProperty().bind(
                    renderedTable.table().getSelectionModel().selectedItemProperty().isNull());
            detailsButton.setOnAction(event -> {
                ModuleRow selected = renderedTable.table().getSelectionModel().getSelectedItem();
                if (selected != null) {
                    showSaleDetails(featureServices.salesService(), selected);
                }
            });
            actions.getChildren().addAll(refreshSales, detailsButton);
            if (salesCustomerEntry != null) {
                Button loadPending = new Button(Localization.text("Tải đơn chờ đã chọn"));
                loadPending.getStyleClass().add("button-secondary");
                loadPending.disableProperty().bind(
                        renderedTable.table().getSelectionModel().selectedItemProperty().isNull());
                loadPending.setOnAction(event -> salesCustomerEntry.loadSelectedPending().run());
                actions.getChildren().add(loadPending);
            }
        } else if ("UC-12".equals(module.id())) {
            Button receiveButton = new Button(module.actionLabel());
            receiveButton.getStyleClass().add("button-primary");
            receiveButton.setOnAction(event -> createGoodsReceiptDialog()
                    .ifPresent(request -> runMutation(
                            () -> featureServices.inventoryWorkflowService()
                                    .createGoodsReceiptDraft(authenticatedUserId, request),
                            () -> refreshRows(renderedTable, featureService, module))));
            Button confirmButton = new Button(Localization.text("Xác nhận phiếu"));
            confirmButton.getStyleClass().add("button-primary");
            confirmButton.setVisible(canConfirmInventoryWorkflow(module.id(), userRole));
            confirmButton.setManaged(canConfirmInventoryWorkflow(module.id(), userRole));
            Button editButton = new Button(Localization.text("Sửa phiếu"));
            editButton.getStyleClass().add("button-secondary");
            Button cancelReceiptButton = new Button(Localization.text("Hủy phiếu"));
            cancelReceiptButton.getStyleClass().add("button-secondary");
            javafx.beans.value.ObservableValue<ModuleRow> selectedReceipt =
                    renderedTable.table().getSelectionModel().selectedItemProperty();
            Runnable updateReceiptActions = () -> {
                ModuleRow selected = renderedTable.table().getSelectionModel().getSelectedItem();
                boolean draft = isUnconfirmedReceipt(selected);
                confirmButton.setDisable(!draft || !canConfirmInventoryWorkflow(module.id(), userRole));
                editButton.setDisable(!draft);
                cancelReceiptButton.setDisable(!draft);
            };
            selectedReceipt.addListener((observable, oldValue, newValue) -> updateReceiptActions.run());
            updateReceiptActions.run();
            confirmButton.setOnAction(event -> {
                ModuleRow selected = renderedTable.table().getSelectionModel().getSelectedItem();
                if (canConfirmInventoryWorkflow(module.id(), userRole)
                        && isUnconfirmedReceipt(selected) && confirmGoodsReceipt(selected)) {
                    runMutation(
                            () -> featureServices.inventoryWorkflowService()
                                    .confirmReceipt(authenticatedUserId, selected.id()),
                            () -> refreshRows(renderedTable, featureService, module));
                }
            });
            editButton.setOnAction(event -> {
                ModuleRow selected = renderedTable.table().getSelectionModel().getSelectedItem();
                if (!isUnconfirmedReceipt(selected)) {
                    return;
                }
                try {
                    InventoryWorkflowDAO.ReceiptDraft draft =
                            featureServices.inventoryWorkflowService().findReceiptDraft(selected.id());
                    createGoodsReceiptDialog(draft).ifPresent(request -> runMutation(
                            () -> featureServices.inventoryWorkflowService().updateReceiptDraft(
                                    authenticatedUserId, selected.id(), request),
                            () -> refreshRows(renderedTable, featureService, module)));
                } catch (IllegalArgumentException | IllegalStateException
                         | vn.ninhkieupharma.pharmacy.common.dao.DataAccessException exception) {
                    showInformation(Localization.text("Không thể sửa phiếu nhập"),
                            Localization.text(exception.getMessage()));
                }
            });
            cancelReceiptButton.setOnAction(event -> {
                ModuleRow selected = renderedTable.table().getSelectionModel().getSelectedItem();
                if (isUnconfirmedReceipt(selected) && confirmCancelReceiptDraft(selected)) {
                    runMutation(
                            () -> featureServices.inventoryWorkflowService()
                                    .cancelReceiptDraft(authenticatedUserId, selected.id()),
                            () -> refreshRows(renderedTable, featureService, module));
                }
            });
            Button detailButton = new Button(Localization.text("Xem lô hàng"));
            detailButton.getStyleClass().add("button-secondary");
            detailButton.disableProperty().bind(
                    renderedTable.table().getSelectionModel().selectedItemProperty().isNull());
            detailButton.setOnAction(event -> {
                ModuleRow selected = renderedTable.table().getSelectionModel().getSelectedItem();
                if (selected != null) {
                    showGoodsReceiptDetails(selected);
                }
            });
            actions.getChildren().addAll(
                    receiveButton, confirmButton, editButton, cancelReceiptButton, detailButton);
        } else if ("UC-14".equals(module.id())) {
            Button stocktakeButton = new Button(module.actionLabel());
            stocktakeButton.getStyleClass().add("button-primary");
            stocktakeButton.setOnAction(event -> createStocktakeDialog()
                    .ifPresent(request -> runMutation(
                            () -> featureServices.inventoryWorkflowService()
                                    .createStocktakeDraft(authenticatedUserId, request),
                            () -> refreshRows(renderedTable, featureService, module))));
            Button confirmButton = new Button(Localization.text("Xác nhận phiên"));
            confirmButton.getStyleClass().add("button-primary");
            confirmButton.setVisible(canConfirmInventoryWorkflow(module.id(), userRole));
            confirmButton.setManaged(canConfirmInventoryWorkflow(module.id(), userRole));
            Button editButton = new Button(Localization.text("Sửa phiên"));
            editButton.getStyleClass().add("button-secondary");
            Button cancelStocktakeButton = new Button(Localization.text("Hủy phiên"));
            cancelStocktakeButton.getStyleClass().add("button-secondary");
            Button detailsButton = new Button(Localization.text("Xem chi tiết"));
            detailsButton.getStyleClass().add("button-secondary");
            javafx.beans.value.ObservableValue<ModuleRow> selectedStocktake =
                    renderedTable.table().getSelectionModel().selectedItemProperty();
            Runnable updateStocktakeActions = () -> {
                ModuleRow selected = renderedTable.table().getSelectionModel().getSelectedItem();
                boolean draft = isUnconfirmedStocktake(selected);
                confirmButton.setDisable(!draft || !canConfirmInventoryWorkflow(module.id(), userRole));
                editButton.setDisable(!draft);
                cancelStocktakeButton.setDisable(!draft);
                detailsButton.setDisable(selected == null);
            };
            selectedStocktake.addListener((observable, oldValue, newValue) -> updateStocktakeActions.run());
            updateStocktakeActions.run();
            confirmButton.setOnAction(event -> {
                ModuleRow selected = renderedTable.table().getSelectionModel().getSelectedItem();
                if (canConfirmInventoryWorkflow(module.id(), userRole)
                        && isUnconfirmedStocktake(selected) && confirmStocktake(selected)) {
                    runMutation(
                            () -> featureServices.inventoryWorkflowService()
                                    .confirmStocktake(authenticatedUserId, selected.id()),
                            () -> refreshRows(renderedTable, featureService, module));
                }
            });
            editButton.setOnAction(event -> {
                ModuleRow selected = renderedTable.table().getSelectionModel().getSelectedItem();
                if (!isUnconfirmedStocktake(selected)) {
                    return;
                }
                try {
                    InventoryWorkflowDAO.StocktakeDraft draft =
                            featureServices.inventoryWorkflowService().findStocktakeDraft(selected.id());
                    createStocktakeDialog(draft).ifPresent(request -> runMutation(
                            () -> featureServices.inventoryWorkflowService().updateStocktakeDraft(
                                    authenticatedUserId, selected.id(), request),
                            () -> refreshRows(renderedTable, featureService, module)));
                } catch (IllegalArgumentException | IllegalStateException
                         | vn.ninhkieupharma.pharmacy.common.dao.DataAccessException exception) {
                    showInformation(Localization.text("Không thể sửa phiên kiểm kê"),
                            Localization.text(exception.getMessage()));
                }
            });
            cancelStocktakeButton.setOnAction(event -> {
                ModuleRow selected = renderedTable.table().getSelectionModel().getSelectedItem();
                if (isUnconfirmedStocktake(selected) && confirmCancelStocktake(selected)) {
                    runMutation(
                            () -> featureServices.inventoryWorkflowService()
                                    .cancelStocktakeDraft(authenticatedUserId, selected.id()),
                            () -> refreshRows(renderedTable, featureService, module));
                }
            });
            detailsButton.setOnAction(event -> {
                ModuleRow selected = renderedTable.table().getSelectionModel().getSelectedItem();
                if (selected != null) {
                    showStocktakeDetails(selected);
                }
            });
            actions.getChildren().addAll(
                    stocktakeButton, confirmButton, editButton, cancelStocktakeButton, detailsButton);
        } else if (canWriteModule(module.id(), userRole)) {
            Button addButton = new Button(module.actionLabel());
            addButton.getStyleClass().add("button-primary");
            if ("UC-02".equals(module.id())) {
                AvailabilityService availabilityService = (AvailabilityService) featureService;
                addButton.setOnAction(event -> createWeeklyAvailabilityDialog(
                        module, availabilityService, featureService, renderedTable));
                Button singleDayButton = new Button(Localization.text("Đăng ký một ngày"));
                singleDayButton.getStyleClass().add("button-secondary");
                singleDayButton.setOnAction(event -> editRow(module, featureService, null)
                        .ifPresent(values -> runMutation(
                                () -> featureService.create(module, values),
                                () -> refreshRows(renderedTable, featureService, module))));
                actions.getChildren().addAll(addButton, singleDayButton);
            } else if ("UC-16".equals(module.id())) {
                addButton.setOnAction(event -> editCustomerRow(
                        module, featureService, renderedTable, null));
                actions.getChildren().add(addButton);
            } else if ("UC-04".equals(module.id()) && userRole == UserRole.STAFF) {
                addButton.setOnAction(event -> createStaffAttendanceDialog(
                        module, featureService, renderedTable));
                actions.getChildren().add(addButton);
            } else {
                addButton.setOnAction(event -> editRow(module, featureService, null)
                        .ifPresent(values -> runMutation(() -> featureService.create(module, values),
                                () -> refreshRows(renderedTable, featureService, module))));
                actions.getChildren().add(addButton);
            }

            if (!"UC-01".equals(module.id())) {
                Button editButton = new Button(Localization.text("Chỉnh sửa"));
                editButton.getStyleClass().add("button-secondary");
                if (!"UC-13".equals(module.id())) {
                    editButton.disableProperty().bind(
                            renderedTable.table().getSelectionModel().selectedItemProperty().isNull());
                }
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
                    } else if ("UC-13".equals(module.id())) {
                        showInformation(
                                Localization.text("Chưa chọn vị trí"),
                                Localization.text("Hãy nhấp chọn một dòng trong bảng rồi bấm Chỉnh sửa."));
                    }
                });

                if ("UC-13".equals(module.id())) {
                    ComboBox<String> statusSelector = new ComboBox<>();
                    statusSelector.getStyleClass().add("button-secondary");
                    statusSelector.setPromptText(Localization.text("Trạng thái"));
                    statusSelector.getItems().addAll(
                            Localization.text("Đang sử dụng"),
                            Localization.text("Ngừng sử dụng"));
                    statusSelector.disableProperty().bind(
                            renderedTable.table().getSelectionModel().selectedItemProperty().isNull());
                    final boolean[] syncingStatus = {false};
                    renderedTable.table().getSelectionModel().selectedItemProperty()
                            .addListener((observable, oldValue, selected) -> {
                                syncingStatus[0] = true;
                                if (selected == null) {
                                    statusSelector.getSelectionModel().clearSelection();
                                } else {
                                    statusSelector.getSelectionModel().select(
                                            "INACTIVE".equalsIgnoreCase(selected.values().get(3)) ? 1 : 0);
                                }
                                syncingStatus[0] = false;
                            });
                    statusSelector.setOnAction(event -> {
                        if (syncingStatus[0]) {
                            return;
                        }
                        ModuleRow selected = renderedTable.table().getSelectionModel().getSelectedItem();
                        if (selected == null || statusSelector.getValue() == null) {
                            return;
                        }
                        String status = statusSelector.getSelectionModel().getSelectedIndex() == 0
                                ? "ACTIVE" : "INACTIVE";
                        String currentStatus = selected.values().get(3);
                        if (status.equalsIgnoreCase(currentStatus)) {
                            return;
                        }
                        List<String> updatedValues = new ArrayList<>(selected.values());
                        updatedValues.set(3, status);
                        runMutation(
                                () -> featureService.update(module, selected.id(), updatedValues),
                                () -> refreshRows(renderedTable, featureService, module));
                    });
                    actions.getChildren().addAll(editButton, statusSelector);
                    if (canDeleteModule(module.id(), userRole)) {
                        Button deleteLocationButton = new Button(Localization.text("Xóa vị trí"));
                        deleteLocationButton.getStyleClass().add("button-secondary");
                        deleteLocationButton.disableProperty().bind(
                                renderedTable.table().getSelectionModel().selectedItemProperty().isNull());
                        deleteLocationButton.setOnAction(event -> {
                            ModuleRow selected = renderedTable.table().getSelectionModel().getSelectedItem();
                            if (selected != null && confirmDeleteLocation(selected)) {
                                runMutation(() -> featureService.delete(module, selected.id()),
                                        () -> refreshRows(renderedTable, featureService, module));
                            }
                        });
                        actions.getChildren().add(deleteLocationButton);
                    }
                } else {
                    actions.getChildren().add(editButton);
                    if (canDeleteModule(module.id(), userRole)) {
                        Button deleteButton = new Button(Localization.text(
                                "UC-18".equals(module.id()) ? "Vô hiệu hóa"
                                        : "UC-16".equals(module.id()) ? "Lưu trữ"
                                        : "Xóa"));
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
                        actions.getChildren().add(deleteButton);
                    }
                }
                if ("UC-16".equals(module.id())) {
                    if (canDeleteModule(module.id(), userRole)) {
                        Button removeProfile = new Button(Localization.text("Xóa hồ sơ"));
                        removeProfile.getStyleClass().add("button-secondary");
                        removeProfile.disableProperty().bind(
                                renderedTable.table().getSelectionModel().selectedItemProperty().isNull());
                        removeProfile.setOnAction(event -> {
                            ModuleRow selected = renderedTable.table().getSelectionModel().getSelectedItem();
                            if (selected != null && confirmPermanentCustomerDeletion(selected)) {
                                runMutation(() -> featureServices.customerLoyaltyService()
                                                .deleteWithoutSales(selected.id()),
                                        () -> refreshRows(renderedTable, featureService, module));
                            }
                        });
                        actions.getChildren().add(removeProfile);
                    }
                    Button historyButton = new Button(Localization.text("Lịch sử mua và điểm"));
                    historyButton.getStyleClass().add("button-secondary");
                    historyButton.disableProperty().bind(
                            renderedTable.table().getSelectionModel().selectedItemProperty().isNull());
                    historyButton.setOnAction(event -> {
                        ModuleRow selected = renderedTable.table().getSelectionModel().getSelectedItem();
                        if (selected != null) {
                            showCustomerHistory(selected);
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
            page.getChildren().addAll(title, description);
            if (!"UC-10".equals(module.id())
                    && !"UC-11".equals(module.id())
                    && !"UC-15".equals(module.id())) {
                page.getChildren().add(createReportSummary(module, featureService));
            }
            page.getChildren().addAll(
                    filters, actions, createReportCharts(module, featureService), renderedTable.table());
        } else {
            String note = salesCustomerEntry != null
                    ? "Nhân viên lưu đơn Pending để có thể sửa/hủy; tồn kho và điểm chỉ cập nhật khi hoàn tất."
                    : "UC-17".equals(module.id())
                            ? "Quản trị viên chỉ xem hóa đơn và chi tiết; thao tác bán hàng dành cho nhân viên."
                    : canWriteModule(module.id(), userRole)
                            ? "Dữ liệu được đọc và ghi theo các bảng trong schema MySQL đã cung cấp."
                            : isWritableModule(module.id())
                                    ? "Nhân viên chỉ có thể xem và tìm kiếm; chỉ quản trị viên được thêm, sửa hoặc xóa."
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
            } else if (productFilters != null) {
                page.getChildren().addAll(title, description, supportNote,
                        actions, filters, renderedTable.table());
            } else {
                page.getChildren().addAll(title, description, supportNote, actions, renderedTable.table());
            }
        }
        return page;
    }

    private void createWeeklyAvailabilityDialog(
            ModuleDefinition module,
            AvailabilityService availabilityService,
            FeatureModuleService featureService,
            RenderedTable renderedTable) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(Localization.text(module.actionLabel()));
        dialog.setHeaderText(Localization.text(module.title()));
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);
        dialog.getDialogPane().getStyleClass().add("form-dialog");

        ComboBox<String> employee = new ComboBox<>();
        employee.getItems().setAll(availabilityService.findActiveEmployeeNames());
        employee.setPromptText(Localization.text("Chọn nhân viên"));
        employee.setMaxWidth(Double.MAX_VALUE);

        LocalDate currentMonday = LocalDate.now().with(
                TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        DatePicker weekDate = new DatePicker(currentMonday);
        weekDate.setConverter(new StringConverter<>() {
            @Override
            public String toString(LocalDate date) {
                return date == null ? "" : date.format(VIETNAMESE_DATE_FORMAT);
            }

            @Override
            public LocalDate fromString(String value) {
                return value == null || value.isBlank()
                        ? null : LocalDate.parse(value, VIETNAMESE_DATE_FORMAT);
            }
        });
        Label weekRange = new Label();
        weekRange.getStyleClass().add("helper-text");
        Runnable updateWeekRange = () -> {
            LocalDate selectedDate = weekDate.getValue();
            if (selectedDate == null) {
                weekRange.setText(Localization.text("Vui lòng chọn một ngày trong tuần."));
                return;
            }
            LocalDate start = selectedDate.with(
                    TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            LocalDate end = start.plusDays(6);
            weekRange.setText(Localization.text("Tuần được chọn: ")
                    + start.format(VIETNAMESE_DATE_FORMAT) + " – "
                    + end.format(VIETNAMESE_DATE_FORMAT));
        };
        weekDate.valueProperty().addListener((observable, oldValue, newValue) -> updateWeekRange.run());
        updateWeekRange.run();

        String[] dayNames = {
                "Thứ Hai", "Thứ Ba", "Thứ Tư", "Thứ Năm", "Thứ Sáu", "Thứ Bảy", "Chủ Nhật"
        };
        List<CheckBox> availableDays = new ArrayList<>();
        List<TextField> startTimes = new ArrayList<>();
        List<TextField> endTimes = new ArrayList<>();
        GridPane availabilityGrid = new GridPane();
        availabilityGrid.setHgap(12);
        availabilityGrid.setVgap(8);
        Label startHeading = new Label(Localization.text("Giờ bắt đầu"));
        Label endHeading = new Label(Localization.text("Giờ kết thúc"));
        availabilityGrid.add(startHeading, 1, 0);
        availabilityGrid.add(endHeading, 2, 0);
        for (int day = 0; day < dayNames.length; day++) {
            CheckBox available = new CheckBox(Localization.text(dayNames[day]));
            TextField start = new TextField("08:00");
            TextField end = new TextField("17:00");
            start.setPrefColumnCount(6);
            end.setPrefColumnCount(6);
            availableDays.add(available);
            startTimes.add(start);
            endTimes.add(end);
            availabilityGrid.add(available, 0, day + 1);
            availabilityGrid.add(start, 1, day + 1);
            availabilityGrid.add(end, 2, day + 1);
        }
        TextField note = new TextField();
        note.setPromptText(Localization.text("Ghi chú chung cho các ngày"));
        Label guidance = new Label(Localization.text(
                "Chọn ngày khả dụng, nhập giờ bắt đầu và giờ kết thúc. Chỉ lưu thời gian nhân viên có thể làm, không tạo ca phân công cửa hàng."));
        guidance.setWrapText(true);
        guidance.getStyleClass().add("helper-text");
        GridPane fields = new GridPane();
        fields.setHgap(16);
        fields.setVgap(12);
        fields.setPadding(new Insets(12, 0, 8, 0));
        fields.add(new Label(Localization.text("Nhân viên")), 0, 0);
        fields.add(employee, 1, 0);
        fields.add(new Label(Localization.text("Tuần bắt đầu")), 0, 1);
        fields.add(weekDate, 1, 1);
        fields.add(weekRange, 1, 2);
        fields.add(availabilityGrid, 0, 3, 2, 1);
        fields.add(note, 0, 4, 2, 1);
        GridPane.setHgrow(employee, Priority.ALWAYS);
        GridPane.setHgrow(weekDate, Priority.ALWAYS);
        VBox content = new VBox(12, fields, guidance);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setPrefWidth(640);
        dialog.setResultConverter(button -> button == ButtonType.OK ? ButtonType.OK : null);

        if (dialog.showAndWait().filter(ButtonType.OK::equals).isEmpty()) {
            return;
        }
        LocalDate selectedDate = weekDate.getValue();
        if (selectedDate == null) {
            showInformation(
                    Localization.text("Không thể đăng ký khả dụng"),
                    Localization.text("Vui lòng chọn một ngày trong tuần."));
            return;
        }
        if (employee.getValue() == null || employee.getValue().isBlank()) {
            showInformation(
                    Localization.text("Không thể đăng ký khả dụng"),
                    Localization.text("Vui lòng chọn nhân viên."));
            return;
        }
        LocalDate weekStart = selectedDate.with(
                TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        List<AvailabilitySlot> slots = new ArrayList<>();
        for (int day = 0; day < availableDays.size(); day++) {
            if (!availableDays.get(day).isSelected()) {
                continue;
            }
            try {
                slots.add(new AvailabilitySlot(
                        weekStart.plusDays(day),
                        LocalTime.parse(startTimes.get(day).getText().trim(), TIME_INPUT_FORMAT),
                        LocalTime.parse(endTimes.get(day).getText().trim(), TIME_INPUT_FORMAT),
                        note.getText().trim()));
            } catch (DateTimeParseException | IllegalArgumentException exception) {
                showInformation(
                        Localization.text("Không thể đăng ký khả dụng"),
                        Localization.text("Giờ phải theo định dạng H:mm; giờ bắt đầu phải sớm hơn giờ kết thúc."));
                return;
            }
        }
        int[] createdCount = new int[1];
        runMutation(
                () -> createdCount[0] = availabilityService.createWeeklyAvailability(
                        employee.getValue(), selectedDate, slots),
                () -> {
                    refreshRows(renderedTable, featureService, module);
                    showInformation(
                            Localization.text("Đăng ký khả dụng thành công"),
                            Localization.text("Đã đăng ký khả dụng cho ")
                                    + createdCount[0] + Localization.text(" ngày."));
                });
    }

    private void configureSalesReport(VBox page, ModuleDefinition module) {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        ComboBox<SalesReport.Period> period = new ComboBox<>(
                FXCollections.observableArrayList(SalesReport.Period.values()));
        period.setConverter(new StringConverter<SalesReport.Period>() {
            @Override
            public String toString(SalesReport.Period value) {
                return value == null ? "" : reportPeriodLabel(value);
            }

            @Override
            public SalesReport.Period fromString(String value) {
                return null;
            }
        });
        TextField year = new TextField(Integer.toString(today.getYear()));
        year.setPrefColumnCount(4);
        ComboBox<Integer> selection = new ComboBox<>();
        Label selectionLabel = new Label();
        VBox selectionField = new VBox(5, selectionLabel, selection);
        period.valueProperty().addListener((observable, previous, current) -> {
            boolean hasSelection = current != SalesReport.Period.YEAR;
            selectionField.setVisible(hasSelection);
            selectionField.setManaged(hasSelection);
            if (hasSelection) {
                boolean monthly = current == SalesReport.Period.MONTH;
                selectionLabel.setText(reportPeriodLabel(current));
                selection.getItems().setAll(IntStream.rangeClosed(1, monthly ? 12 : 4).boxed().toList());
                Integer initial = monthly ? today.getMonthValue() : (today.getMonthValue() + 2) / 3;
                selection.setValue(initial);
            }
        });
        period.setValue(SalesReport.Period.MONTH);

        Button generate = new Button(module.actionLabel());
        generate.getStyleClass().add("button-primary");
        Button export = new Button(Localization.text("Xuất báo cáo"));
        export.getStyleClass().add("button-secondary");
        Button print = new Button(Localization.text("In báo cáo"));
        print.getStyleClass().add("button-secondary");
        export.setDisable(true);
        print.setDisable(true);
        FlowPane filters = new FlowPane(12, 10,
                new VBox(5, new Label(Localization.text("Kỳ báo cáo")), period),
                new VBox(5, new Label(Localization.text("Năm")), year), selectionField,
                generate, export, print);
        filters.setAlignment(Pos.BOTTOM_LEFT);
        filters.getStyleClass().add("filter-bar");

        Label dateRange = new Label();
        dateRange.getStyleClass().add("section-title");
        FlowPane summary = new FlowPane(12, 8);
        summary.getStyleClass().add("demo-summary");
        FlowPane charts = new FlowPane(16, 16);
        charts.getStyleClass().add("report-chart-row");
        RenderedTable table = createTable(module, featureServices.salesService(), List.of());
        page.getChildren().addAll(filters, dateRange, summary, charts, table.table());

        Runnable refresh = () -> {
            String rawYear = year.getText().trim();
            if (!rawYear.matches("[0-9]{4}")) {
                showInformation(module.title(), Localization.text("Hãy nhập năm từ 1000 đến 9998."));
                return;
            }
            try {
                SalesReport.Period selectedPeriod = period.getValue();
                int bucket = selectedPeriod == SalesReport.Period.YEAR ? 1 : selection.getValue();
                SalesReport report = featureServices.salesService().salesReport(
                        selectedPeriod, Integer.parseInt(rawYear), bucket);
                SalesReport.Totals totals = report.totals();
                List<ModuleRow> reportRows = salesReportOutput(report, totals);
                String range = report.start() + " – " + report.endExclusive().minusDays(1);
                dateRange.setText(range);
                table.source().setAll(reportRows.subList(0, report.rows().size()));
                summary.getChildren().setAll(
                        summaryCard(Localization.text("Số đơn"), Long.toString(totals.orderCount())),
                        summaryCard(Localization.text("Doanh thu"), formatSaleMoney(totals.revenue())),
                        summaryCard(Localization.text("Giảm giá"), formatSaleMoney(totals.discount())),
                        summaryCard(Localization.text("Giá trị trung bình"), formatSaleMoney(totals.averageOrderValue())));
                updateSalesReportCharts(charts, report);
                String fileName = module.id() + "-" + report.start()
                        + "-" + report.endExclusive().minusDays(1) + ".csv";
                export.setOnAction(event -> exportReport(
                        module, reportRows, page.getScene().getWindow(), fileName));
                print.setOnAction(event -> printSalesReport(
                        module, range, reportRows, page.getScene().getWindow()));
                export.setDisable(false);
                print.setDisable(false);
            } catch (IllegalArgumentException | vn.ninhkieupharma.pharmacy.common.dao.DataAccessException exception) {
                showInformation(module.title(), Localization.text(exception.getMessage()));
            }
        };
        generate.setOnAction(event -> refresh.run());
        refresh.run();
    }

    private String reportPeriodLabel(SalesReport.Period period) {
        return Localization.text(switch (period) {
            case MONTH -> "Tháng";
            case QUARTER -> "Quý";
            case YEAR -> "Năm";
        });
    }

    static List<ModuleRow> salesReportOutput(SalesReport report, SalesReport.Totals totals) {
        List<ModuleRow> rows = new ArrayList<>();
        for (SalesReport.Row row : report.rows()) {
            String date = report.period() == SalesReport.Period.MONTH
                    ? row.start().toString() : java.time.YearMonth.from(row.start()).toString();
            rows.add(salesReportDisplayRow(new UUID(0, row.start().toEpochDay()), date, row.totals()));
        }
        rows.add(salesReportDisplayRow(new UUID(0, Long.MAX_VALUE), Localization.text("Tổng cộng"), totals));
        return List.copyOf(rows);
    }

    private static ModuleRow salesReportDisplayRow(UUID id, String period, SalesReport.Totals totals) {
        return new ModuleRow(id, List.of(period, Long.toString(totals.orderCount()),
                totals.revenue().toPlainString(), totals.discount().toPlainString(),
                totals.averageOrderValue().toPlainString()));
    }

    private void updateSalesReportCharts(FlowPane charts, SalesReport report) {
        boolean daily = report.period() == SalesReport.Period.MONTH;
        List<String> labels = report.rows().stream()
                .map(row -> Integer.toString(daily ? row.start().getDayOfMonth() : row.start().getMonthValue()))
                .toList();
        List<Number> counts = report.rows().stream()
                .<Number>map(row -> row.totals().orderCount()).toList();
        List<Number> revenue = report.rows().stream()
                .<Number>map(row -> row.totals().revenue().movePointLeft(6)).toList();
        String interval = daily ? "Ngày" : "Tháng";
        charts.getChildren().setAll(
                chartCard(Localization.text("Số lượng hóa đơn"), barChart(
                        Localization.text(interval), Localization.text("Số đơn"), labels,
                        seriesFromList(Localization.text("Hóa đơn"), counts))),
                chartCard(Localization.text("Doanh thu"), lineChart(
                        Localization.text(interval), Localization.text("Doanh thu (triệu ₫)"), labels,
                        seriesFromList(Localization.text("Doanh thu"), revenue))));
    }

    private void printSalesReport(
            ModuleDefinition module, String dateRange, List<ModuleRow> rows, javafx.stage.Window owner) {
        String title = Localization.text("In báo cáo");
        PrinterJob job = PrinterJob.createPrinterJob();
        if (job == null) {
            showInformation(title, Localization.text("Không tìm thấy máy in."));
            return;
        }
        if (!job.showPrintDialog(owner)) {
            job.cancelJob();
            return;
        }
        try {
            var layout = job.getJobSettings().getPageLayout();
            int perPage = Math.max(1, (int) ((layout.getPrintableHeight() - 110) / 22));
            int pageCount = (rows.size() + perPage - 1) / perPage;
            for (int index = 0; index < pageCount; index++) {
                int start = index * perPage;
                List<ModuleRow> pageRows = rows.subList(start, Math.min(start + perPage, rows.size()));
                GridPane printedTable = new GridPane();
                for (double width : new double[] { 21, 12, 22, 22, 23 }) {
                    ColumnConstraints column = new ColumnConstraints();
                    column.setPercentWidth(width);
                    printedTable.getColumnConstraints().add(column);
                }
                addPrintedReportRow(printedTable, 0, module.columns(), true);
                for (int row = 0; row < pageRows.size(); row++) {
                    List<String> values = new ArrayList<>(pageRows.get(row).values());
                    for (int column = 2; column < values.size(); column++) {
                        values.set(column, formatSaleMoney(new BigDecimal(values.get(column))));
                    }
                    addPrintedReportRow(printedTable, row + 1, values, start + row == rows.size() - 1);
                }
                Label heading = new Label(module.title());
                heading.setStyle("-fx-font-size: 16pt; -fx-font-weight: bold;");
                Label pageNumber = new Label(Localization.text("Trang") + " " + (index + 1) + "/" + pageCount);
                VBox printedPage = new VBox(8, heading, new Label(dateRange), printedTable, pageNumber);
                new Scene(printedPage, layout.getPrintableWidth(), layout.getPrintableHeight());
                printedPage.applyCss();
                printedPage.layout();
                if (!job.printPage(printedPage)) {
                    job.cancelJob();
                    showInformation(title, Localization.text("Không thể in báo cáo."));
                    return;
                }
            }
            if (!job.endJob()) {
                showInformation(title, Localization.text("Không thể in báo cáo."));
            }
        } catch (RuntimeException exception) {
            job.cancelJob();
            showInformation(title, Localization.text("Không thể in báo cáo."));
        }
    }

    private void addPrintedReportRow(GridPane table, int row, List<String> values, boolean bold) {
        table.getRowConstraints().add(new RowConstraints(22));
        for (int column = 0; column < values.size(); column++) {
            Label cell = new Label(values.get(column));
            cell.setStyle(bold ? "-fx-font-size: 9pt; -fx-font-weight: bold;" : "-fx-font-size: 9pt;");
            table.add(cell, column, row);
        }
    }

    private FlowPane createFilters(
            ModuleDefinition module,
            List<Node> filterInputs,
            FilteredList<ModuleRow> rows,
            TextField search) {
        FlowPane filters = new FlowPane(12, 8);
        filters.setAlignment(Pos.CENTER_LEFT);
        filters.getStyleClass().add("filter-bar");
        for (int index = 0; index < Math.min(module.formFields().size(), 2); index++) {
            TextField filter = new TextField();
            filter.setPromptText(module.formFields().get(index));
            filter.getStyleClass().add("filter-input");
            filterInputs.add(filter);
            filters.getChildren().add(filter);
        }
        Button applyButton = new Button(Localization.text("Áp dụng"));
        applyButton.getStyleClass().add("button-secondary");
        applyButton.setOnAction(event -> applyFilters(rows, search, filterInputs));
        filters.getChildren().add(applyButton);
        return filters;
    }

    private ReportFilters createReportFilters(ModuleDefinition module) {
        GridPane fieldsGrid = new GridPane();
        fieldsGrid.setHgap(12);
        fieldsGrid.setVgap(10);
        for (int index = 0; index < 3; index++) {
            ColumnConstraints column = new ColumnConstraints();
            column.setPercentWidth(100.0 / 3);
            column.setHgrow(Priority.ALWAYS);
            column.setFillWidth(true);
            fieldsGrid.getColumnConstraints().add(column);
        }

        List<Node> fields = new ArrayList<>();
        for (int index = 0; index < module.columns().size(); index++) {
            Control field;
            if (index == 1 && "UC-10".equals(module.id())) {
                field = createCatalogFilter("UC-07", 0, module.columns().get(index));
            } else if (index == 1 && "UC-11".equals(module.id())) {
                field = createCatalogFilter("UC-09", 1, module.columns().get(index));
            } else {
                TextField textField = new TextField();
                textField.setMinWidth(90);
                textField.setPrefWidth(160);
                field = textField;
            }
            field.setMaxWidth(Double.MAX_VALUE);
            fields.add(field);

            Label label = new Label(Localization.text(module.columns().get(index)));
            label.setWrapText(true);
            label.getStyleClass().add("overview-label");
            VBox fieldGroup = new VBox(5, label, field);
            GridPane.setHgrow(fieldGroup, Priority.ALWAYS);
            fieldsGrid.add(fieldGroup, index % 3, index / 3);
        }

        Button applyButton = new Button(Localization.text("Áp dụng"));
        applyButton.getStyleClass().add("button-secondary");
        HBox actions = new HBox(applyButton);
        actions.setAlignment(Pos.CENTER_RIGHT);

        VBox container = new VBox(10, fieldsGrid, actions);
        container.setMaxWidth(Double.MAX_VALUE);
        container.getStyleClass().add("filter-bar");
        return new ReportFilters(container, List.copyOf(fields), applyButton);
    }

    private ProductFilters createProductFilters(ModuleDefinition module, List<ModuleRow> products) {
        GridPane container = new GridPane();
        container.setHgap(10);
        container.setVgap(8);
        container.setMaxWidth(Double.MAX_VALUE);
        container.getStyleClass().add("filter-bar");
        for (int index = 0; index < 5; index++) {
            ColumnConstraints column = new ColumnConstraints();
            column.setPercentWidth(20);
            column.setHgrow(Priority.ALWAYS);
            column.setFillWidth(true);
            container.getColumnConstraints().add(column);
        }

        List<Integer> columnIndexes = new ArrayList<>();
        List<Node> fields = new ArrayList<>();
        Map<Integer, ComboBox<ProductFilterOption>> selectors = new LinkedHashMap<>();
        TextField minimumPrice = null;
        TextField maximumPrice = null;
        for (int index = 0; index < module.columns().size(); index++) {
            if (index == 5) {
                minimumPrice = createPriceFilterField("Giá bán từ (VNĐ)");
                maximumPrice = createPriceFilterField("Giá bán đến (VNĐ)");
                HBox priceRange = new HBox(8,
                        minimumPrice, new Label("–"), maximumPrice);
                priceRange.setAlignment(Pos.CENTER_LEFT);
                container.add(priceRange, 0, 1);
                continue;
            }
            if (index == 9) {
                continue;
            }
            Node field;
            if (List.of(2, 3, 4, 6, 7, 8).contains(index)) {
                ComboBox<ProductFilterOption> selector = createProductChoiceFilter(index, products);
                selectors.put(index, selector);
                field = selector;
            } else {
                field = createProductFilterField(module.columns().get(index));
            }
            fields.add(field);
            columnIndexes.add(index);
            int row = index < 5 ? 0 : 1;
            int column = index < 5 ? index : index - 5;
            container.add(field, column, row);
            GridPane.setHgrow(field, Priority.ALWAYS);
        }

        return new ProductFilters(container, List.copyOf(columnIndexes), List.copyOf(fields), Map.copyOf(selectors),
                minimumPrice, maximumPrice);
    }

    private ComboBox<ProductFilterOption> createProductChoiceFilter(
            int columnIndex, List<ModuleRow> products) {
        ComboBox<ProductFilterOption> filter = new ComboBox<>();
        filter.getItems().setAll(productFilterOptions(columnIndex, products));
        filter.setPromptText(productFilterFieldLabel(columnIndex));
        filter.setMinWidth(90);
        filter.setPrefWidth(140);
        filter.setMaxWidth(Double.MAX_VALUE);
        filter.getStyleClass().add("filter-input");
        filter.getSelectionModel().selectFirst();
        return filter;
    }

    private String productFilterFieldLabel(int columnIndex) {
        return Localization.text(switch (columnIndex) {
            case 2 -> "Danh mục";
            case 3 -> "Dạng bào chế";
            case 4 -> "Đơn vị";
            case 6 -> "Kê đơn";
            case 7 -> "Trạng thái";
            case 8 -> "Nhà cung cấp";
            default -> throw new IllegalArgumentException("Unsupported product filter column: " + columnIndex);
        });
    }

    private List<ProductFilterOption> productFilterOptions(int columnIndex, List<ModuleRow> products) {
        List<String> values = switch (columnIndex) {
            case 6 -> List.of("Yes", "No");
            case 7 -> List.of("ACTIVE", "INACTIVE");
            default -> products.stream()
                    .filter(row -> row.values().size() > columnIndex)
                    .map(row -> row.values().get(columnIndex))
                    .filter(value -> !value.isBlank())
                    .distinct()
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .toList();
        };
        List<ProductFilterOption> options = new ArrayList<>();
        options.add(new ProductFilterOption("", productFilterFieldLabel(columnIndex)));
        values.stream()
                .map(value -> new ProductFilterOption(value, Localization.productText(value)))
                .forEach(options::add);
        return List.copyOf(options);
    }

    private void refreshProductFilterOptions(ProductFilters filters, List<ModuleRow> products) {
        filters.selectors().forEach((columnIndex, selector) -> {
            ProductFilterOption current = selector.getValue();
            selector.getItems().setAll(productFilterOptions(columnIndex, products));
            ProductFilterOption matching = selector.getItems().stream()
                    .filter(option -> current != null && option.value().equals(current.value()))
                    .findFirst()
                    .orElse(selector.getItems().get(0));
            selector.getSelectionModel().select(matching);
        });
    }

    private TextField createProductFilterField(String prompt) {
        TextField field = new TextField();
        field.setPromptText(Localization.text(prompt));
        field.setMinWidth(90);
        field.setPrefWidth(140);
        field.getStyleClass().add("filter-input");
        return field;
    }

    private TextField createPriceFilterField(String prompt) {
        TextField field = new TextField();
        field.setPromptText(Localization.text(prompt));
        field.setMinWidth(40);
        field.setPrefWidth(100);
        field.getStyleClass().add("filter-input");
        field.setTextFormatter(new TextFormatter<String>(change ->
                change.getControlNewText().matches("[\\d.,]{0,20}") ? change : null));
        return field;
    }

    private void applyProductFilters(
            FilteredList<ModuleRow> rows,
            TextField search,
            ProductFilters filters) {
        BigDecimal minimumPrice = parseProductPriceBound(filters.minimumPrice().getText());
        BigDecimal maximumPrice = parseProductPriceBound(filters.maximumPrice().getText());

        List<String> columnQueries = new ArrayList<>(Collections.nCopies(10, ""));
        for (int index = 0; index < filters.fields().size(); index++) {
            int columnIndex = filters.columnIndexes().get(index);
            String value = productFilterValue(filters.fields().get(index));
            columnQueries.set(columnIndex, value);
        }
        BigDecimal capturedMinimumPrice = minimumPrice;
        BigDecimal capturedMaximumPrice = maximumPrice;
        rows.setPredicate(row -> matchesProductFilters(
                row, search.getText(), columnQueries, capturedMinimumPrice, capturedMaximumPrice));
    }

    private String productFilterValue(Node field) {
        if (field instanceof ComboBox<?> comboBox && comboBox.getValue() instanceof ProductFilterOption option) {
            return option.value();
        }
        return inputValue(field);
    }

    static BigDecimal parseProductPriceBound(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.replaceAll("\\s+", "");
        if (normalized.matches("\\d+(?:[.,]\\d{0,2})?")) {
            normalized = normalized.replace(',', '.');
        } else if (normalized.matches("\\d{1,3}(?:\\.\\d{3})+(?:,\\d{1,2})?")) {
            normalized = normalized.replace(".", "").replace(',', '.');
        } else if (normalized.matches("\\d{1,3}(?:,\\d{3})+(?:\\.\\d{1,2})?")) {
            normalized = normalized.replace(",", "");
        } else {
            return null;
        }
        return new BigDecimal(normalized);
    }

    static boolean matchesProductFilters(
            ModuleRow row,
            String searchQuery,
            List<String> columnQueries,
            BigDecimal minimumPrice,
            BigDecimal maximumPrice) {
        List<String> values = row.values();
        if (values.size() != 10 || columnQueries.size() != 10) {
            throw new IllegalArgumentException("Product filters require 10 product columns.");
        }

        String searchableRow = values.stream()
                .map(value -> value + " " + Localization.text(value))
                .reduce("", (left, right) -> left + " " + right)
                .toLowerCase(Locale.ROOT);
        if (!normalize(searchQuery).isBlank() && !searchableRow.contains(normalize(searchQuery))) {
            return false;
        }
        for (int index = 0; index < columnQueries.size(); index++) {
            String query = normalize(columnQueries.get(index));
            String cellValue = normalize(values.get(index) + " " + Localization.text(values.get(index))
                    + " " + Localization.productText(values.get(index)));
            if (!query.isBlank() && !cellValue.contains(query)) {
                return false;
            }
        }

        BigDecimal price = new BigDecimal(values.get(5));
        return (minimumPrice == null || price.compareTo(minimumPrice) >= 0)
                && (maximumPrice == null || price.compareTo(maximumPrice) <= 0);
    }

    private ComboBox<String> createCatalogFilter(String moduleId, int valueIndex, String prompt) {
        ModuleDefinition sourceModule = moduleDefinition(moduleId);
        FeatureModuleService sourceService = featureServices.forModule(sourceModule);
        List<String> options = sourceService.findAll(sourceModule).stream()
                .filter(row -> row.values().size() > valueIndex)
                .map(row -> row.values().get(valueIndex))
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
        ComboBox<String> filter = new ComboBox<>(FXCollections.observableArrayList(options));
        filter.setPromptText(Localization.text(prompt));
        filter.setMaxWidth(Double.MAX_VALUE);
        return filter;
    }

    private InventoryFilters createInventoryFilters() {
        DatePicker movementFrom = new DatePicker();
        movementFrom.setPromptText(Localization.text("Biến động từ ngày"));
        DatePicker movementTo = new DatePicker();
        movementTo.setPromptText(Localization.text("Đến ngày"));

        ComboBox<String> movementType = new ComboBox<>(FXCollections.observableArrayList(
                "ALL", "RECEIPT", "SALE", "ADJUSTMENT", "DAMAGED", "EXPIRED"));
        movementType.getSelectionModel().selectFirst();
        movementType.setConverter(new StringConverter<>() {
            @Override
            public String toString(String value) {
                return switch (value == null ? "ALL" : value) {
                    case "RECEIPT" -> Localization.text("Nhập hàng");
                    case "SALE" -> Localization.text("Bán hàng");
                    case "ADJUSTMENT" -> Localization.text("Điều chỉnh");
                    case "DAMAGED" -> Localization.text("Hư hỏng");
                    case "EXPIRED" -> Localization.text("Hết hạn");
                    default -> Localization.text("Tất cả biến động");
                };
            }

            @Override
            public String fromString(String value) {
                return value;
            }
        });

        ComboBox<String> expiryStatus = new ComboBox<>(FXCollections.observableArrayList(
                "ALL", "VALID", "EXPIRING", "EXPIRED", "NO_DATE"));
        expiryStatus.getSelectionModel().selectFirst();
        expiryStatus.setConverter(new StringConverter<>() {
            @Override
            public String toString(String value) {
                return switch (value == null ? "ALL" : value) {
                    case "VALID" -> Localization.text("Còn hạn trên 30 ngày");
                    case "EXPIRING" -> Localization.text("Sắp hết hạn trong 30 ngày");
                    case "EXPIRED" -> Localization.text("Đã hết hạn");
                    case "NO_DATE" -> Localization.text("Chưa có hạn sử dụng");
                    default -> Localization.text("Tất cả hạn sử dụng");
                };
            }

            @Override
            public String fromString(String value) {
                return value;
            }
        });

        ComboBox<String> location = new ComboBox<>();
        location.getItems().add("ALL");
        featureServices.inventoryWorkflowService().findLocations().stream()
                .map(InventoryWorkflowDAO.LocationOption::toString)
                .forEach(location.getItems()::add);
        location.getSelectionModel().selectFirst();
        location.setConverter(new StringConverter<>() {
            @Override
            public String toString(String value) {
                return "ALL".equals(value) ? Localization.text("Tất cả vị trí")
                        : value == null ? "" : value;
            }

            @Override
            public String fromString(String value) {
                return value;
            }
        });

        Button applyButton = new Button(Localization.text("Áp dụng bộ lọc"));
        applyButton.getStyleClass().add("button-secondary");
        FlowPane filters = new FlowPane(10, 8,
                movementFrom, movementTo, movementType, expiryStatus, location, applyButton);
        filters.setAlignment(Pos.CENTER_LEFT);
        filters.getStyleClass().add("filter-bar");
        return new InventoryFilters(
                new VBox(filters), movementFrom, movementTo, movementType, expiryStatus, location, applyButton);
    }

    private void applyInventoryFilters(
            FilteredList<ModuleRow> rows, TextField search, InventoryFilters filters) {
        String query = normalize(search.getText());
        LocalDate movementFrom = filters.movementFrom().getValue();
        LocalDate movementTo = filters.movementTo().getValue();
        if (movementFrom != null && movementTo != null && movementFrom.isAfter(movementTo)) {
            showInformation(
                    Localization.text("Bộ lọc"),
                    Localization.text("Ngày bắt đầu không được sau ngày kết thúc."));
            return;
        }
        String movementType = filters.movementType().getValue();
        String expiryStatus = filters.expiryStatus().getValue();
        String location = filters.location().getValue();
        LocalDate today = LocalDate.now();
        rows.setPredicate(row -> {
            List<String> values = row.values();
            return matchesInventoryRow(
                    values, query, movementFrom, movementTo, movementType, expiryStatus, location, today);
        });
    }

    static boolean matchesInventoryRow(
            List<String> values,
            String query,
            LocalDate movementFrom,
            LocalDate movementTo,
            String movementType,
            String expiryStatus,
            String location,
            LocalDate today) {
        if (values == null || values.size() < 7) {
            return false;
        }
        String searchableText = String.join(" ", values);
        searchableText += " " + Localization.movementTypeText(values.get(2));
        String searchable = normalize(searchableText);
        String normalizedQuery = normalize(query);
        if (!normalizedQuery.isBlank() && !searchable.contains(normalizedQuery)) {
            return false;
        }

        LocalDate movementDate = parseInventoryDateSafe(values.get(0));
        if (movementDate != null
                && ((movementFrom != null && movementDate.isBefore(movementFrom))
                || (movementTo != null && movementDate.isAfter(movementTo)))) {
            return false;
        }
        if (movementType != null && !"ALL".equals(movementType)
                && !matchesMovementType(movementType, values.get(2))) {
            return false;
        }
        if (location != null && !"ALL".equals(location)
                && !normalize(location).equals(normalize(values.get(6)))) {
            return false;
        }

        LocalDate expiryDate = parseInventoryExpiry(values.get(5));
        return switch (expiryStatus == null ? "ALL" : expiryStatus) {
            case "VALID" -> expiryDate != null && expiryDate.isAfter(today.plusDays(30));
            case "EXPIRING" -> expiryDate != null && expiryDate.isAfter(today)
                    && !expiryDate.isAfter(today.plusDays(30));
            case "EXPIRED" -> expiryDate != null && !expiryDate.isAfter(today);
            case "NO_DATE" -> expiryDate == null;
            default -> true;
        };
    }

    private static LocalDate parseInventoryDateSafe(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String date = value.substring(0, Math.min(10, value.length()));
        try {
            return LocalDate.parse(date);
        } catch (java.time.format.DateTimeParseException ignored) {
            try {
                return LocalDate.parse(date,
                        java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            } catch (java.time.format.DateTimeParseException invalidFormat) {
                return null;
            }
        }
    }

    private static LocalDate parseInventoryExpiry(String value) {
        if (value == null || value.isBlank()
                || "—".equals(value.trim()) || "-".equals(value.trim())) {
            return null;
        }
        return parseInventoryDateSafe(value);
    }

    private LocalDate parseInventoryDate(String value) {
        String date = value.substring(0, Math.min(10, value.length()));
        try {
            return LocalDate.parse(date);
        } catch (java.time.format.DateTimeParseException ignored) {
            return LocalDate.parse(date, java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        }
    }

    private void exportReport(
            ModuleDefinition module, List<ModuleRow> rows, javafx.stage.Window owner) {
        exportReport(module, rows, owner, module.id() + "-" + LocalDate.now() + ".csv");
    }

    private void exportReport(
            ModuleDefinition module, List<ModuleRow> rows, javafx.stage.Window owner, String fileName) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(Localization.text("Xuất báo cáo"));
        chooser.setInitialFileName(fileName);
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(Localization.text("CSV"), "*.csv"));
        java.io.File selectedFile = chooser.showSaveDialog(owner);
        if (selectedFile == null) {
            return;
        }
        Path file = selectedFile.toPath();
        if (file == null) {
            return;
        }
        if (!file.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".csv")) {
            file = file.resolveSibling(file.getFileName() + ".csv");
        }
        try {
            writeCsvReport(file, module, List.copyOf(rows));
            showInformation(Localization.text("Xuất báo cáo"),
                    Localization.text("Đã xuất báo cáo thành công.") + "\n" + file);
        } catch (IOException exception) {
            showInformation(Localization.text("Xuất báo cáo"),
                    Localization.text("Không thể xuất báo cáo.") + "\n" + exception.getMessage());
        }
    }

    static void writeCsvReport(Path file, ModuleDefinition module, List<ModuleRow> rows)
            throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            writer.write("\uFEFF");
            writer.write(module.columns().stream().map(PageRenderer::csvValue).toList()
                    .stream().collect(java.util.stream.Collectors.joining(",")));
            writer.newLine();
            for (ModuleRow row : rows) {
                writer.write(row.values().stream()
                        .map(PageRenderer::csvValue)
                        .collect(java.util.stream.Collectors.joining(",")));
                writer.newLine();
            }
        }
    }

    private static String csvValue(String value) {
        String safe = value == null ? "" : value;
        if (safe.indexOf(',') >= 0 || safe.indexOf('"') >= 0
                || safe.indexOf('\n') >= 0 || safe.indexOf('\r') >= 0) {
            return "\"" + safe.replace("\"", "\"\"") + "\"";
        }
        return safe;
    }

    private static boolean matchesMovementType(String selectedType, String displayedType) {
        return switch (selectedType) {
            case "RECEIPT" -> displayedType.equals("RECEIPT")
                    || displayedType.equals("Nhập hàng") || displayedType.equals("Goods Receipt");
            case "SALE" -> displayedType.equals("SALE")
                    || displayedType.equals("Bán hàng") || displayedType.equals("Sale");
            case "ADJUSTMENT" -> displayedType.equals("ADJUSTMENT")
                    || displayedType.equals("Điều chỉnh") || displayedType.equals("Adjustment");
            case "DAMAGED" -> displayedType.equals("DAMAGED")
                    || displayedType.equals("Hư hỏng") || displayedType.equals("Damaged");
            case "EXPIRED" -> displayedType.equals("EXPIRED")
                    || displayedType.equals("Hết hạn") || displayedType.equals("Expired");
            default -> true;
        };
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

        FlowPane textFields = new FlowPane(10, 8, code, name, status);
        FlowPane datesAndApply = new FlowPane(10, 8, accrualStartFrom, redemptionEndTo, applyButton);
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
        FlowPane filters = new FlowPane(10, 8, status, applyButton);
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

    private static String normalize(String value) {
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
            case 2 -> new BigDecimal(value).stripTrailingZeros().toPlainString() + "%";
            case 7 -> formatPromotionMoney(new BigDecimal(value), true);
            case 8 -> formatPromotionTiers(value);
            case 9 -> "ACTIVE".equalsIgnoreCase(value)
                    ? Localization.text("Đang áp dụng")
                    : Localization.text("Đã vô hiệu hóa");
            default -> value;
        };
    }

    private String formatPromotionTiers(String encodedTiers) {
        if (encodedTiers.isBlank()) {
            return "";
        }
        List<String> formattedTiers = new ArrayList<>();
        for (String encoded : encodedTiers.split(";")) {
            String[] parts = encoded.split(":", -1);
            BigDecimal thresholdVnd = new BigDecimal(parts[0]);
            String threshold = formatPromotionMoney(thresholdVnd, false);
            String rate = new BigDecimal(parts[1]).stripTrailingZeros().toPlainString();
            formattedTiers.add(threshold + " → " + rate + "%");
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
            ModuleDefinition module, FeatureModuleService featureService, List<ModuleRow> rows) {
        TableView<ModuleRow> table = new TableView<>();
        table.getStyleClass().add("data-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        table.setMinHeight(240);
        table.setPrefHeight(360);
        for (int index = 0; index < module.columns().size(); index++) {
            final int columnIndex = index;
            TableColumn<ModuleRow, String> column = new TableColumn<>(module.columns().get(index));
            double columnWidth = reportColumnWidth(module.id(), index);
            if (columnWidth > 0) {
                column.setMinWidth(columnWidth);
                column.setPrefWidth(columnWidth);
            }
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
                if ("UC-08".equals(module.id()) && columnIndex == 5 && !value.isBlank()) {
                    value = formatSaleMoney(new BigDecimal(value));
                }
                if ("UC-08".equals(module.id())) {
                    value = Localization.productText(value);
                }
                if ("UC-15".equals(module.id()) && columnIndex == 2) {
                    value = Localization.movementTypeText(value);
                }
                if ("UC-19".equals(module.id()) && columnIndex >= 2) {
                    value = formatSaleMoney(new BigDecimal(value));
                }
                value = formatDisplayDate(value);
                return new javafx.beans.property.ReadOnlyStringWrapper(Localization.text(value));
            });
            table.getColumns().add(column);
        }

        ObservableList<ModuleRow> source = FXCollections.observableArrayList(rows);
        FilteredList<ModuleRow> filteredRows = new FilteredList<>(source, row -> true);
        table.setItems(filteredRows);
        if ("UC-12".equals(module.id()) || "UC-13".equals(module.id())
                || "UC-14".equals(module.id()) || "UC-15".equals(module.id())) {
            table.setRowFactory(view -> {
                javafx.scene.control.TableRow<ModuleRow> row =
                        new javafx.scene.control.TableRow<>();
                row.setOnMouseClicked(event -> {
                    if (row.isEmpty()) {
                        return;
                    }
                    table.getSelectionModel().select(row.getItem());
                    if (event.getButton() == javafx.scene.input.MouseButton.PRIMARY
                            && event.getClickCount() == 2) {
                        if ("UC-12".equals(module.id())) {
                            showGoodsReceiptDetails(row.getItem());
                        } else if ("UC-14".equals(module.id())) {
                            showStocktakeDetails(row.getItem());
                        } else if ("UC-15".equals(module.id())) {
                            showInventoryMovementDetails(row.getItem());
                        } else {
                            if ("UC-13".equals(module.id())) {
                                editRow(module, featureService, row.getItem())
                                        .ifPresent(values -> runMutation(
                                                () -> featureService.update(module, row.getItem().id(), values),
                                                () -> source.setAll(featureService.findAll(module))));
                            }
                        }
                    }
                });
                return row;
            });
        }
        Label emptyState = new Label(Localization.text("Không tìm thấy dữ liệu phù hợp."));
        emptyState.getStyleClass().add("table-empty-state");
        emptyState.setWrapText(true);
        table.setPlaceholder(emptyState);
        VBox.setVgrow(table, Priority.ALWAYS);

        RenderedTable result = new RenderedTable(table, source, filteredRows);
        return result;
    }

    private double reportColumnWidth(String moduleId, int columnIndex) {
        double[] widths = switch (moduleId) {
            case "UC-10" -> new double[] {100, 160, 220, 100, 210};
            case "UC-11" -> new double[] {105, 180, 125, 100, 200};
            default -> new double[0];
        };
        return columnIndex < widths.length ? widths[columnIndex] : 0;
    }

    private Node createReportSummary(ModuleDefinition module, FeatureModuleService featureService) {
        FlowPane summary = new FlowPane(12, 8);
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
                if (employees != null && !employees.isEmpty()) {
                    List<String> names = new ArrayList<>();
                    List<Number> hours = new ArrayList<>();
                    List<Number> shifts = new ArrayList<>();

                    for (ModuleRow row : employees) {
                        List<String> vals = row.values();
                        if (vals == null || vals.size() < 4) {
                            continue;
                        }
                        names.add(vals.get(0) != null ? vals.get(0) : "N/A");

                        // Lọc lấy phần số sạch cho Giờ thực tế (tránh lỗi khi có chữ "giờ", "hrs" hoặc null)
                        String rawHour = vals.get(3) == null ? "0" : vals.get(3).replaceAll("[^0-9.]", "");
                        hours.add(rawHour.isEmpty() ? 0.0 : parseDecimal(rawHour));

                        // Lọc lấy phần số sạch cho Số ca (tránh lỗi Integer.parseInt khi có ".0" hoặc chữ)
                        String rawShift = vals.get(2) == null ? "0" : vals.get(2).replaceAll("[^0-9.]", "");
                        int shiftCount = rawShift.isEmpty() ? 0 : (int) Math.round(parseDecimal(rawShift));
                        shifts.add(shiftCount);
                    }

                    if (!names.isEmpty()) {
                        charts.getChildren().add(chartCard("Giờ làm theo nhân viên", barChart(
                                "Nhân viên", "Số giờ", names, seriesFromList("Thực tế", hours))));
                        charts.getChildren().add(chartCard("Phân bổ số ca", pieChart(
                                "Ca làm", seriesPairs(names, shifts))));
                    }
                }
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
                TrendChartData data = createTrendChartData(featureService.findAll(module), 0, 1);
                charts.getChildren().add(chartCard("Biểu đồ cột: Số mặt hàng theo quý và ngành hàng",
                        barChart("Quý", "Số mặt hàng", data.periods(), data.series())));
                charts.getChildren().add(chartCard("Biểu đồ đường: Xu hướng ngành hàng theo quý",
                        lineChart("Quý", "Số mặt hàng", data.periods(), data.series())));
                charts.getChildren().add(chartCard("Biểu đồ miền: Ngành hàng theo quý",
                        areaChart("Quý", "Số mặt hàng", data.periods(), data.series())));
                charts.getChildren().add(chartCard("Biểu đồ hình bánh: Tỷ trọng ngành hàng",
                        pieChart("Danh mục", seriesPairs(data.dimensions(), data.latestPeriodTotals()))));
            }
            case "UC-11" -> {
                TrendChartData data = createTrendChartData(featureService.findAll(module), 0, 1);
                charts.getChildren().add(chartCard("Biểu đồ cột: Số mặt hàng theo tháng và nhà cung cấp",
                        barChart("Tháng", "Số mặt hàng", data.periods(), data.series())));
                charts.getChildren().add(chartCard("Biểu đồ đường: Xu hướng độ phủ nhà cung cấp",
                        lineChart("Tháng", "Số mặt hàng", data.periods(), data.series())));
                charts.getChildren().add(chartCard("Biểu đồ miền: Cung ứng theo tháng",
                        areaChart("Tháng", "Số mặt hàng", data.periods(), data.series())));
                charts.getChildren().add(chartCard("Biểu đồ hình bánh: Tỷ trọng nhà cung cấp",
                        pieChart("Nhà cung cấp", seriesPairs(data.dimensions(), data.latestPeriodTotals()))));
            }
            case "UC-15" -> {
                List<ModuleRow> movementRows = featureService.findAll(module);
                Map<String, Double> quantitiesByType = new LinkedHashMap<>();
                Map<String, Integer> countsByType = new LinkedHashMap<>();
                for (ModuleRow row : movementRows) {
                    String type = Localization.movementTypeText(row.values().get(2));
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
            default -> throw new IllegalArgumentException(
                    "Chưa cấu hình biểu đồ cho báo cáo " + module.id());
        }
        return charts;
    }

    static TrendChartData createTrendChartData(
            List<ModuleRow> rows, int periodIndex, int dimensionIndex) {
        Map<String, Map<String, Integer>> countsByPeriod = new TreeMap<>();
        for (ModuleRow row : rows) {
            if (row.values().size() < 3) {
                throw new IllegalArgumentException("Trend report rows require period, dimension, and count values.");
            }
            String period = row.values().get(periodIndex);
            String dimension = row.values().get(dimensionIndex);
            int count = parseReportProductCount(row.values().get(2));
            countsByPeriod.computeIfAbsent(period, ignored -> new LinkedHashMap<>())
                    .merge(dimension, count, Integer::sum);
        }

        List<String> periods = List.copyOf(countsByPeriod.keySet());
        List<String> dimensions = countsByPeriod.values().stream()
                .flatMap(counts -> counts.keySet().stream())
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
        Map<String, List<Number>> series = new LinkedHashMap<>();
        List<Number> latestPeriodTotals = new ArrayList<>();
        for (String dimension : dimensions) {
            List<Number> values = periods.stream()
                    .map(period -> (Number) countsByPeriod.get(period).getOrDefault(dimension, 0))
                    .toList();
            series.put(dimension, values);
            int latestCount = periods.isEmpty()
                    ? 0
                    : countsByPeriod.get(periods.get(periods.size() - 1)).getOrDefault(dimension, 0);
            latestPeriodTotals.add(latestCount);
        }
        return new TrendChartData(
                periods, Collections.unmodifiableMap(series), dimensions, List.copyOf(latestPeriodTotals));
    }

    private static int parseReportProductCount(String value) {
        String count = value.trim().split("\\s+", 2)[0];
        try {
            return Integer.parseInt(count);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Expected a numeric report count: " + value, exception);
        }
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
        xAxis.setTickLabelRotation(categories.size() > 5 ? 35 : 0);
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
        xAxis.setTickLabelRotation(categories.size() > 5 ? 35 : 0);
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
        xAxis.setTickLabelRotation(categories.size() > 5 ? 35 : 0);
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
                    .map(value -> value + " " + Localization.text(value))
                    .reduce("", (left, right) -> left + " " + right)
                    .toLowerCase(Locale.ROOT);
            return (query.isBlank() || searchableRow.contains(query))
                    && criteria.stream().allMatch(searchableRow::contains);
        });
    }

    private void applyReportFilters(
            FilteredList<ModuleRow> rows, TextField search, ReportFilters filters) {
        List<String> columnQueries = filters.fields().stream()
                .map(this::inputValue)
                .toList();
        rows.setPredicate(row -> matchesReportFilters(row, search.getText(), columnQueries));
    }

    static boolean matchesReportFilters(
            ModuleRow row, String searchQuery, List<String> columnQueries) {
        List<String> values = row.values();
        if (values.size() != columnQueries.size()) {
            throw new IllegalArgumentException("Report filters must match the report column count.");
        }

        String searchableRow = values.stream()
                .map(value -> value + " " + Localization.text(value))
                .reduce("", (left, right) -> left + " " + right)
                .toLowerCase(Locale.ROOT);
        if (!normalize(searchQuery).isBlank() && !searchableRow.contains(normalize(searchQuery))) {
            return false;
        }
        for (int index = 0; index < columnQueries.size(); index++) {
            String query = normalize(columnQueries.get(index));
            String value = normalize(values.get(index) + " " + Localization.text(values.get(index)));
            if (!query.isBlank() && !value.contains(query)) {
                return false;
            }
        }
        return true;
    }

    private void applyStatusFilters(
            FilteredList<ModuleRow> rows, TextField search, ComboBox<String> statusFilter) {
        String query = search.getText() == null
                ? "" : search.getText().trim().toLowerCase(Locale.ROOT);
        String status = statusFilter.getValue();
        rows.setPredicate(row -> {
            String searchableRow = row.values().stream()
                    .map(Localization::text)
                    .reduce("", (left, right) -> left + " " + right)
                    .toLowerCase(Locale.ROOT);
            return (query.isBlank() || searchableRow.contains(query))
                    && (status == null || Localization.text("Tất cả trạng thái").equals(status)
                    || row.values().size() > 4 && status.equals(Localization.text(row.values().get(4))));
        });
    }

    private boolean isUnconfirmedReceipt(ModuleRow receipt) {
        return receipt != null && receipt.values().size() > 4
                && "Chưa xác nhận".equals(receipt.values().get(4));
    }

    private boolean isUnconfirmedStocktake(ModuleRow stocktake) {
        return stocktake != null && stocktake.values().size() > 4
                && "Chưa xác nhận".equals(stocktake.values().get(4));
    }

    private boolean confirmGoodsReceipt(ModuleRow receipt) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(Localization.text("Xác nhận phiếu nhập"));
        alert.setHeaderText(receipt.values().get(0));
        alert.setContentText(Localization.text(
                "Xác nhận phiếu sẽ cập nhật tồn kho. Sau khi xác nhận, không thể sửa hoặc xóa phiếu."));
        alert.getButtonTypes().setAll(ButtonType.CANCEL, ButtonType.OK);
        return alert.showAndWait().filter(ButtonType.OK::equals).isPresent();
    }

    private boolean confirmCancelReceiptDraft(ModuleRow receipt) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(Localization.text("Hủy phiếu nhập"));
        alert.setHeaderText(receipt.values().get(0));
        alert.setContentText(Localization.text(
                "Phiếu chưa xác nhận sẽ được chuyển sang trạng thái đã hủy và vẫn được lưu trong lịch sử. Tồn kho không thay đổi."));
        alert.getButtonTypes().setAll(ButtonType.CANCEL, ButtonType.OK);
        return alert.showAndWait().filter(ButtonType.OK::equals).isPresent();
    }

    private boolean confirmStocktake(ModuleRow stocktake) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(Localization.text("Xác nhận phiên kiểm kê"));
        alert.setHeaderText(stocktake.values().get(0));
        alert.setContentText(Localization.text(
                "Xác nhận sẽ điều chỉnh tồn kho theo số đếm. Sau đó không thể sửa hoặc xóa phiên."));
        alert.getButtonTypes().setAll(ButtonType.CANCEL, ButtonType.OK);
        return alert.showAndWait().filter(ButtonType.OK::equals).isPresent();
    }

    private boolean confirmCancelStocktake(ModuleRow stocktake) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(Localization.text("Hủy phiên kiểm kê"));
        alert.setHeaderText(stocktake.values().get(0));
        alert.setContentText(Localization.text(
                "Phiên chưa xác nhận sẽ được chuyển sang trạng thái đã hủy và vẫn được lưu trong lịch sử. Tồn kho không thay đổi."));
        alert.getButtonTypes().setAll(ButtonType.CANCEL, ButtonType.OK);
        return alert.showAndWait().filter(ButtonType.OK::equals).isPresent();
    }

    private String inputValue(Node node) {
        if (node instanceof TextInputControl textInput) {
            return textInput.getText().trim();
        }
        if (node instanceof ComboBox<?> comboBox) {
            Object value = comboBox.getValue();
            return value == null ? "" : value.toString().trim();
        }
        return "";
    }

    private void refreshRows(
            RenderedTable renderedTable,
            FeatureModuleService featureService,
            ModuleDefinition module) {
        renderedTable.source().setAll(featureService.findAll(module));
    }

    private void showGoodsReceiptDetails(ModuleRow receipt) {
        List<InventoryWorkflowDAO.ReceiptDetail> details;
        try {
            details = featureServices.inventoryWorkflowService().findReceiptDetails(receipt.id());
        } catch (IllegalArgumentException | IllegalStateException
                 | vn.ninhkieupharma.pharmacy.common.dao.DataAccessException exception) {
            showInformation(
                    Localization.text("Không thể xem lô hàng"),
                    Localization.text(exception.getMessage()));
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(Localization.text("Chi tiết lô hàng nhập"));
        dialog.setHeaderText(Localization.text(
                "Phiếu " + receipt.values().get(0) + " · " + receipt.values().get(1)));
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().getStyleClass().add("form-dialog");
        dialog.getDialogPane().setPrefWidth(980);

        TableView<InventoryWorkflowDAO.ReceiptDetail> table = new TableView<>();
        table.getStyleClass().add("data-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setItems(FXCollections.observableArrayList(details));
        table.setPrefHeight(Math.min(420, 90 + details.size() * 42));

        addReceiptDetailColumn(table, "Mã sản phẩm", detail -> detail.productCode(), 110);
        addReceiptDetailColumn(table, "Tên sản phẩm", detail -> detail.productName(), 180);
        addReceiptDetailColumn(table, "Đơn vị", detail -> detail.unitName(), 80);
        addReceiptDetailColumn(table, "Mã lô", detail -> detail.batchCode(), 110);
        addReceiptDetailColumn(table, "Hạn sử dụng",
                detail -> detail.expiryDate() == null ? "Không có" : detail.expiryDate().toString(), 115);
        addReceiptDetailColumn(table, "Số lượng",
                detail -> detail.quantity().stripTrailingZeros().toPlainString(), 90);
        addReceiptDetailColumn(table, "Đơn giá",
                detail -> formatSaleMoney(detail.unitCost()), 110);
        addReceiptDetailColumn(table, "Vị trí kho",
                detail -> detail.locationCode() + " · " + detail.locationName(), 170);

        Label emptyState = new Label(Localization.text("Phiếu nhập này chưa có lô hàng."));
        emptyState.getStyleClass().add("table-empty-state");
        table.setPlaceholder(emptyState);

        VBox content = new VBox(12, table);
        content.setPadding(new Insets(12, 0, 8, 0));
        dialog.getDialogPane().setContent(content);
        dialog.showAndWait();
    }

    private void addReceiptDetailColumn(
            TableView<InventoryWorkflowDAO.ReceiptDetail> table,
            String title,
            java.util.function.Function<InventoryWorkflowDAO.ReceiptDetail, String> value,
            double width) {
        TableColumn<InventoryWorkflowDAO.ReceiptDetail, String> column =
                new TableColumn<>(Localization.text(title));
        column.setCellValueFactory(cell ->
                new javafx.beans.property.ReadOnlyStringWrapper(
                        Localization.text(value.apply(cell.getValue()))));
        column.setPrefWidth(width);
        table.getColumns().add(column);
    }

    private void showStocktakeDetails(ModuleRow stocktake) {
        List<InventoryWorkflowDAO.StocktakeDetail> details;
        try {
            details = featureServices.inventoryWorkflowService().findStocktakeDetails(stocktake.id());
        } catch (IllegalArgumentException | IllegalStateException
                 | vn.ninhkieupharma.pharmacy.common.dao.DataAccessException exception) {
            showInformation(
                    Localization.text("Không thể xem phiên kiểm kê"),
                    Localization.text(exception.getMessage()));
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(Localization.text("Chi tiết kiểm kê"));
        dialog.setHeaderText(Localization.text(
                "Phiên " + stocktake.values().get(0)
                        + " · " + stocktake.values().get(1)));
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().getStyleClass().add("form-dialog");
        dialog.getDialogPane().setPrefWidth(1040);

        GridPane summary = new GridPane();
        summary.setHgap(18);
        summary.setVgap(8);
        addCustomerField(summary, 0, "Ngày kiểm kê", new Label(stocktake.values().get(2)));
        addCustomerField(summary, 1, "Chênh lệch", new Label(stocktake.values().get(3)));
        addCustomerField(summary, 2, "Trạng thái", new Label(
                Localization.text(stocktake.values().get(4))));
        if (stocktake.values().size() > 5 && !stocktake.values().get(5).isBlank()) {
            Label note = new Label(stocktake.values().get(5));
            note.setWrapText(true);
            note.setMaxWidth(Double.MAX_VALUE);
            addCustomerField(summary, 3, "Ghi chú", note);
        }

        TableView<InventoryWorkflowDAO.StocktakeDetail> table = new TableView<>();
        table.getStyleClass().add("data-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setItems(FXCollections.observableArrayList(details));
        table.setPrefHeight(Math.min(420, 90 + details.size() * 42));

        addStocktakeDetailColumn(table, "Mã sản phẩm", detail -> detail.productCode(), 110);
        addStocktakeDetailColumn(table, "Tên sản phẩm", detail -> detail.productName(), 180);
        addStocktakeDetailColumn(table, "Đơn vị", detail -> detail.unitName(), 70);
        addStocktakeDetailColumn(table, "Mã lô", detail -> detail.batchCode(), 100);
        addStocktakeDetailColumn(table, "Hạn sử dụng",
                detail -> detail.expiryDate() == null ? "Không có" : detail.expiryDate().toString(), 115);
        addStocktakeDetailColumn(table, "Tồn hệ thống",
                detail -> detail.expectedQuantity().stripTrailingZeros().toPlainString(), 100);
        addStocktakeDetailColumn(table, "Tồn đếm",
                detail -> detail.countedQuantity().stripTrailingZeros().toPlainString(), 90);
        addStocktakeDetailColumn(table, "Chênh lệch",
                detail -> formatSignedQuantity(detail.differenceQuantity()), 90);
        addStocktakeDetailColumn(table, "Vị trí",
                detail -> detail.locationCode() + " · " + detail.locationName(), 170);

        Label emptyState = new Label(Localization.text("Phiên kiểm kê này chưa có chi tiết lô hàng."));
        emptyState.getStyleClass().add("table-empty-state");
        table.setPlaceholder(emptyState);

        VBox content = new VBox(12, summary, table);
        content.setPadding(new Insets(12, 0, 8, 0));
        dialog.getDialogPane().setContent(content);
        dialog.showAndWait();
    }

    private void showInventoryMovementDetails(ModuleRow movement) {
        List<String> values = movement.values();

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(Localization.text("Chi tiết biến động kho"));
        dialog.setHeaderText(Localization.text(values.get(1)));
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().getStyleClass().add("form-dialog");
        dialog.getDialogPane().setPrefWidth(720);

        GridPane fields = new GridPane();
        fields.setHgap(16);
        fields.setVgap(12);
        addCustomerField(fields, 0, "Thời điểm", new Label(Localization.text(values.get(0))));
        addCustomerField(fields, 1, "Sản phẩm / lô", new Label(Localization.text(values.get(1))));
        addCustomerField(fields, 2, "Loại biến động",
                new Label(Localization.movementTypeText(values.get(2))));
        addCustomerField(fields, 3, "Số lượng", new Label(Localization.text(values.get(3))));
        Label explanation = new Label(Localization.text(
                values.get(4).isBlank() ? "Không có ghi chú" : values.get(4)));
        explanation.setWrapText(true);
        addCustomerField(fields, 4, "Diễn giải", explanation);
        addCustomerField(fields, 5, "Hạn sử dụng",
                new Label(Localization.text("—".equals(values.get(5))
                        ? "Không có hạn sử dụng" : values.get(5))));
        addCustomerField(fields, 6, "Vị trí kho", new Label(Localization.text(values.get(6))));

        VBox content = new VBox(12, fields);
        content.setPadding(new Insets(12, 0, 8, 0));
        dialog.getDialogPane().setContent(content);
        dialog.showAndWait();
    }

    private String formatSignedQuantity(BigDecimal quantity) {
        String formatted = quantity.stripTrailingZeros().toPlainString();
        if (quantity.signum() > 0) {
            return "+" + formatted;
        }
        return formatted;
    }

    private void addStocktakeDetailColumn(
            TableView<InventoryWorkflowDAO.StocktakeDetail> table,
            String title,
            java.util.function.Function<InventoryWorkflowDAO.StocktakeDetail, String> value,
            double width) {
        TableColumn<InventoryWorkflowDAO.StocktakeDetail, String> column =
                new TableColumn<>(Localization.text(title));
        column.setCellValueFactory(cell ->
                new javafx.beans.property.ReadOnlyStringWrapper(
                        Localization.text(value.apply(cell.getValue()))));
        column.setPrefWidth(width);
        table.getColumns().add(column);
    }

    private java.util.Optional<InventoryWorkflowDAO.ReceiptRequest> createGoodsReceiptDialog() {
        return createGoodsReceiptDialog(null);
    }

    private java.util.Optional<InventoryWorkflowDAO.ReceiptRequest> createGoodsReceiptDialog(
            InventoryWorkflowDAO.ReceiptDraft initialDraft) {
        InventoryWorkflowService service = featureServices.inventoryWorkflowService();
        List<InventoryWorkflowDAO.SupplierOption> suppliers = service.findSuppliers();
        List<InventoryWorkflowDAO.ProductOption> products = service.findProducts();
        List<InventoryWorkflowDAO.LocationOption> locations = service.findLocations();
        if (suppliers.isEmpty() || products.isEmpty() || locations.isEmpty()) {
            showInformation(Localization.text("Chưa thể nhập hàng"),
                    Localization.text("Cần có nhà cung cấp, sản phẩm đang hoạt động và vị trí kho đang sử dụng."));
            return java.util.Optional.empty();
        }

        Dialog<InventoryWorkflowDAO.ReceiptRequest> dialog = new Dialog<>();
        dialog.setTitle(Localization.text(initialDraft == null ? "Tạo phiếu nhập" : "Sửa phiếu nhập"));
        dialog.setHeaderText(Localization.text(initialDraft == null
                ? "Nhập hàng vào kho" : "Chỉnh sửa phiếu chưa xác nhận"));
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);
        dialog.getDialogPane().getStyleClass().add("form-dialog");
        dialog.getDialogPane().setPrefWidth(1040);

        ComboBox<InventoryWorkflowDAO.SupplierOption> supplier = new ComboBox<>(
                FXCollections.observableArrayList(suppliers));
        if (initialDraft == null) {
            supplier.getSelectionModel().selectFirst();
        } else {
            supplier.getItems().stream()
                    .filter(option -> option.id() == initialDraft.supplierId())
                    .findFirst()
                    .ifPresent(supplier.getSelectionModel()::select);
        }
        supplier.setMaxWidth(Double.MAX_VALUE);
        Node supplierSelector = searchableInventorySelector(
                supplier, "Tìm nhà cung cấp...", 460);
        DatePicker receivedDate = new DatePicker(
                initialDraft == null ? LocalDate.now() : initialDraft.receivedDate());
        TextArea note = new TextArea();
        note.setPromptText(Localization.text("Số hóa đơn nhà cung cấp / ghi chú"));
        note.setPrefRowCount(2);
        note.setWrapText(true);
        note.getStyleClass().add("text-input");
        if (initialDraft != null && initialDraft.note() != null) {
            note.setText(initialDraft.note());
        }

        GridPane header = new GridPane();
        header.setHgap(16);
        header.setVgap(10);
        addCustomerField(header, 0, "Nhà cung cấp", supplierSelector);
        addCustomerField(header, 1, "Ngày nhập", receivedDate);
        addCustomerField(header, 2, "Số hóa đơn NCC / Ghi chú", note);

        VBox lineRows = new VBox(8);
        ScrollPane lineScroll = new ScrollPane(lineRows);
        lineScroll.setFitToWidth(true);
        lineScroll.setPrefViewportHeight(260);
        List<ReceiptLineInput> lines = new ArrayList<>();
        Button addLine = new Button(Localization.text("Thêm mặt hàng"));
        addLine.getStyleClass().add("button-secondary");
        addLine.setOnAction(event -> addReceiptLine(lines, lineRows, products));
        if (initialDraft == null) {
            addReceiptLine(lines, lineRows, products);
        } else {
            for (InventoryWorkflowDAO.ReceiptLine line : initialDraft.lines()) {
                addReceiptLine(lines, lineRows, products, line);
            }
        }

        VBox content = new VBox(12,
                header,
                new Label(Localization.text(
                        "Phiếu mới sẽ ở trạng thái chưa xác nhận và chưa làm thay đổi tồn kho.")),
                new Label(Localization.text(
                        "Mỗi dòng nhập lưu sản phẩm, mã lô, hạn sử dụng, vị trí, số lượng và giá nhập.")),
                lineScroll,
                addLine);
        content.setPadding(new Insets(10, 0, 0, 0));
        dialog.getDialogPane().setContent(content);

        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.addEventFilter(ActionEvent.ACTION, event -> {
            try {
                buildReceiptRequest(supplier, receivedDate, note, lines);
            } catch (IllegalArgumentException exception) {
                showInformation(Localization.text("Thông tin phiếu nhập chưa hợp lệ"),
                        Localization.text(exception.getMessage()));
                event.consume();
            }
        });
        dialog.setResultConverter(button -> button == ButtonType.OK
                ? buildReceiptRequest(supplier, receivedDate, note, lines) : null);
        return dialog.showAndWait();
    }

    private <T> VBox searchableInventorySelector(ComboBox<T> combo, String prompt, double width) {
        TextField search = new TextField();
        search.setPromptText(Localization.text(prompt));
        search.setPrefWidth(width);
        search.getStyleClass().add("text-input");

        FilteredList<T> filteredItems = new FilteredList<>(combo.getItems(), item -> true);
        combo.setItems(filteredItems);
        combo.setPrefWidth(width);
        combo.setMaxWidth(Double.MAX_VALUE);
        search.textProperty().addListener((observable, oldValue, newValue) -> {
            String query = normalize(newValue);
            filteredItems.setPredicate(item ->
                    query.isBlank() || normalize(String.valueOf(item)).contains(query));
            T selected = combo.getValue();
            if (selected != null && !filteredItems.contains(selected)) {
                combo.getSelectionModel().clearSelection();
            }
            if (query.isBlank() || filteredItems.isEmpty()) {
                combo.hide();
            } else {
                combo.show();
            }
        });
        VBox selector = new VBox(4, search, combo);
        selector.setPrefWidth(width);
        selector.setMinWidth(0);
        return selector;
    }

    private void addReceiptLine(
            List<ReceiptLineInput> lines,
            VBox lineRows,
            List<InventoryWorkflowDAO.ProductOption> products) {
        addReceiptLine(lines, lineRows, products, null);
    }

    private void addReceiptLine(
            List<ReceiptLineInput> lines,
            VBox lineRows,
            List<InventoryWorkflowDAO.ProductOption> products,
            InventoryWorkflowDAO.ReceiptLine initialLine) {
        ComboBox<InventoryWorkflowDAO.ProductOption> product =
                new ComboBox<>(FXCollections.observableArrayList(products));
        product.setPromptText(Localization.text("Sản phẩm"));
        Node productSelector = searchableInventorySelector(product, "Tìm sản phẩm...", 220);
        List<InventoryWorkflowDAO.LocationOption> locations =
                featureServices.inventoryWorkflowService().findLocations();
        ComboBox<InventoryWorkflowDAO.LocationOption> location =
                new ComboBox<>(FXCollections.observableArrayList(locations));
        if (initialLine == null) {
            location.getSelectionModel().selectFirst();
        } else {
            location.getItems().stream()
                    .filter(option -> option.id() == initialLine.locationId())
                    .findFirst()
                    .ifPresent(location.getSelectionModel()::select);
        }
        Node locationSelector = searchableInventorySelector(location, "Tìm kệ / vị trí...", 150);
        TextField batchCode = new TextField();
        batchCode.setPromptText(Localization.text("Mã lô"));
        batchCode.setPrefWidth(110);
        DatePicker expiryDate = new DatePicker();
        expiryDate.setPromptText(Localization.text("Hạn dùng"));
        expiryDate.setPrefWidth(140);
        TextField quantity = new TextField();
        quantity.setPromptText(Localization.text("Số lượng"));
        quantity.setPrefWidth(90);
        TextField unitCost = new TextField();
        unitCost.setPromptText(Localization.text("Giá nhập"));
        unitCost.setPrefWidth(100);
        if (initialLine != null) {
            product.getItems().stream()
                    .filter(option -> option.id() == initialLine.productId())
                    .findFirst()
                    .ifPresent(product.getSelectionModel()::select);
            batchCode.setText(initialLine.batchCode());
            expiryDate.setValue(initialLine.expiryDate());
            quantity.setText(initialLine.quantity().stripTrailingZeros().toPlainString());
            unitCost.setText(initialLine.unitCost().stripTrailingZeros().toPlainString());
        }
        Button remove = new Button(Localization.text("Bỏ"));
        HBox row = new HBox(
                8, productSelector, batchCode, expiryDate, locationSelector, quantity, unitCost, remove);
        row.setAlignment(Pos.CENTER_LEFT);
        ReceiptLineInput line = new ReceiptLineInput(
                product, batchCode, expiryDate, location, quantity, unitCost, row);
        remove.setOnAction(event -> {
            lines.remove(line);
            lineRows.getChildren().remove(row);
        });
        lines.add(line);
        lineRows.getChildren().add(row);
    }

    private InventoryWorkflowDAO.ReceiptRequest buildReceiptRequest(
            ComboBox<InventoryWorkflowDAO.SupplierOption> supplier,
            DatePicker receivedDate,
            TextArea note,
            List<ReceiptLineInput> lines) {
        if (supplier.getValue() == null || receivedDate.getValue() == null || lines.isEmpty()) {
            throw new IllegalArgumentException("Chọn nhà cung cấp, ngày nhập và nhập ít nhất một mặt hàng.");
        }
        if (receivedDate.getValue().isAfter(LocalDate.now()) || note.getText().length() > 255) {
            throw new IllegalArgumentException("Ngày nhập không được trong tương lai; ghi chú tối đa 255 ký tự.");
        }
        List<InventoryWorkflowDAO.ReceiptLine> requestLines = new ArrayList<>();
        java.util.Set<String> batchKeys = new java.util.HashSet<>();
        for (ReceiptLineInput line : lines) {
            if (line.product().getValue() == null || line.location().getValue() == null
                    || line.batchCode().getText().isBlank() || line.batchCode().getText().trim().length() > 50) {
                throw new IllegalArgumentException("Mỗi dòng phải có sản phẩm, vị trí và mã lô.");
            }
            BigDecimal quantity = parseInventoryDecimal(line.quantity().getText(), "Số lượng");
            BigDecimal unitCost = parseInventoryDecimal(line.unitCost().getText(), "Giá nhập");
            if (quantity.signum() <= 0 || unitCost.signum() < 0
                    || line.expiryDate().getValue() != null
                    && line.expiryDate().getValue().isBefore(receivedDate.getValue())) {
                throw new IllegalArgumentException(
                        "Số lượng phải lớn hơn 0, giá nhập không âm và hạn dùng không trước ngày nhập.");
            }
            if (!isValidInventoryQuantity(quantity)) {
                throw new IllegalArgumentException(
                        "Số lượng tối đa 9 chữ số phần nguyên và 3 chữ số thập phân.");
            }
            if (!isValidInventoryMoney(unitCost)) {
                throw new IllegalArgumentException(
                        "Giá nhập tối đa 10 chữ số phần nguyên và 2 chữ số thập phân.");
            }
            String batchKey = line.product().getValue().id() + ":" + line.location().getValue().id() + ":"
                    + line.batchCode().getText().trim().toLowerCase(Locale.ROOT);
            if (!batchKeys.add(batchKey)) {
                throw new IllegalArgumentException("Không được nhập trùng sản phẩm, vị trí và mã lô.");
            }
            requestLines.add(new InventoryWorkflowDAO.ReceiptLine(
                    line.product().getValue().id(),
                    line.location().getValue().id(),
                    line.batchCode().getText().trim(),
                    line.expiryDate().getValue(),
                    quantity, unitCost));
        }
        return new InventoryWorkflowDAO.ReceiptRequest(
                supplier.getValue().id(), receivedDate.getValue(), note.getText().trim(), requestLines);
    }

    private java.util.Optional<InventoryWorkflowDAO.StocktakeRequest> createStocktakeDialog() {
        return createStocktakeDialog(null);
    }

    private java.util.Optional<InventoryWorkflowDAO.StocktakeRequest> createStocktakeDialog(
            InventoryWorkflowDAO.StocktakeDraft initialDraft) {
        InventoryWorkflowService service = featureServices.inventoryWorkflowService();
        List<InventoryWorkflowDAO.LocationOption> locations = service.findLocations();
        if (locations.isEmpty()) {
            showInformation(Localization.text("Chưa thể kiểm kê"),
                    Localization.text("Hãy tạo và kích hoạt vị trí kho trước khi kiểm kê."));
            return java.util.Optional.empty();
        }

        Dialog<InventoryWorkflowDAO.StocktakeRequest> dialog = new Dialog<>();
        dialog.setTitle(Localization.text(initialDraft == null ? "Tạo phiên kiểm kê" : "Sửa phiên kiểm kê"));
        dialog.setHeaderText(Localization.text(initialDraft == null
                ? "Ghi nhận tồn thực tế theo từng lô" : "Chỉnh sửa phiên chưa xác nhận"));
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);
        dialog.getDialogPane().getStyleClass().add("form-dialog");
        dialog.getDialogPane().setPrefWidth(900);

        ComboBox<InventoryWorkflowDAO.LocationOption> location = new ComboBox<>(
                FXCollections.observableArrayList(locations));
        if (initialDraft == null) {
            location.getSelectionModel().selectFirst();
        } else {
            location.getItems().stream()
                    .filter(option -> option.id() == initialDraft.locationId())
                    .findFirst()
                    .ifPresent(location.getSelectionModel()::select);
        }
        DatePicker stocktakeDate = new DatePicker(
                initialDraft == null ? LocalDate.now() : initialDraft.stocktakeDate());
        TextArea note = new TextArea();
        note.setPromptText(Localization.text("Ghi chú kiểm kê"));
        note.setPrefRowCount(2);
        note.setWrapText(true);
        if (initialDraft != null && initialDraft.note() != null) {
            note.setText(initialDraft.note());
        }
        GridPane header = new GridPane();
        header.setHgap(16);
        header.setVgap(10);
        addCustomerField(header, 0, "Vị trí kho", location);
        addCustomerField(header, 1, "Ngày kiểm kê", stocktakeDate);
        addCustomerField(header, 2, "Ghi chú", note);

        VBox batchRows = new VBox(6);
        Map<Long, TextField> countedQuantities = new java.util.LinkedHashMap<>();
        Map<Long, BigDecimal> savedCounts = initialDraft == null
                ? Map.of()
                : initialDraft.counts().stream().collect(java.util.stream.Collectors.toMap(
                        InventoryWorkflowDAO.StocktakeCount::batchId,
                        InventoryWorkflowDAO.StocktakeCount::countedQuantity));
        Label batchStatus = new Label();
        Runnable reloadBatches = () -> {
            batchRows.getChildren().clear();
            countedQuantities.clear();
            batchRows.getChildren().add(new HBox(12,
                    inventoryColumnHeader("Sản phẩm / lô", 360),
                    inventoryColumnHeader("Hạn dùng", 110),
                    inventoryColumnHeader("Tồn hệ thống", 120),
                    inventoryColumnHeader("Đếm thực tế", 120)));
            List<InventoryWorkflowDAO.BatchOption> batches = location.getValue() == null
                    ? List.of() : service.findBatches(location.getValue().id());
            for (InventoryWorkflowDAO.BatchOption batch : batches) {
                BigDecimal savedCount = initialDraft != null
                        && location.getValue().id() == initialDraft.locationId()
                        ? savedCounts.get(batch.id()) : null;
                TextField counted = new TextField((savedCount == null
                        ? batch.quantityOnHand() : savedCount).stripTrailingZeros().toPlainString());
                counted.setPromptText(Localization.text("Số lượng thực tế"));
                counted.setPrefWidth(120);
                countedQuantities.put(batch.id(), counted);
                batchRows.getChildren().add(new HBox(12,
                        inventoryColumnValue(batch.productCode() + " · " + batch.productName()
                                + " · " + batch.batchCode(), 360),
                        inventoryColumnValue(batch.expiryDate() == null ? "—" : batch.expiryDate().toString(), 110),
                        inventoryColumnValue(batch.quantityOnHand().stripTrailingZeros().toPlainString(), 120),
                        counted));
            }
            batchStatus.setText(batches.isEmpty()
                    ? Localization.text("Vị trí này chưa có lô tồn kho để kiểm kê.")
                    : Localization.text(
                            "Phiên chưa xác nhận sẽ chưa thay đổi tồn kho; xác nhận để áp dụng chênh lệch."));
        };
        location.valueProperty().addListener((observable, oldValue, newValue) -> reloadBatches.run());
        reloadBatches.run();

        ScrollPane batchScroll = new ScrollPane(batchRows);
        batchScroll.setFitToWidth(true);
        batchScroll.setPrefViewportHeight(340);
        VBox content = new VBox(10, header,
                new Label(Localization.text(
                        "Phiên mới ở trạng thái chưa xác nhận; tồn kho chỉ đổi sau khi xác nhận.")),
                batchStatus, batchScroll);
        content.setPadding(new Insets(10, 0, 0, 0));
        dialog.getDialogPane().setContent(content);

        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.addEventFilter(ActionEvent.ACTION, event -> {
            try {
                buildStocktakeRequest(location, stocktakeDate, note, countedQuantities);
            } catch (IllegalArgumentException exception) {
                showInformation(Localization.text("Thông tin kiểm kê chưa hợp lệ"),
                        Localization.text(exception.getMessage()));
                event.consume();
            }
        });
        dialog.setResultConverter(button -> button == ButtonType.OK
                ? buildStocktakeRequest(location, stocktakeDate, note, countedQuantities) : null);
        return dialog.showAndWait();
    }

    private InventoryWorkflowDAO.StocktakeRequest buildStocktakeRequest(
            ComboBox<InventoryWorkflowDAO.LocationOption> location,
            DatePicker stocktakeDate,
            TextArea note,
            Map<Long, TextField> countedQuantities) {
        if (location.getValue() == null || stocktakeDate.getValue() == null || countedQuantities.isEmpty()) {
            throw new IllegalArgumentException("Chọn vị trí có lô tồn kho và ngày kiểm kê.");
        }
        if (stocktakeDate.getValue().isAfter(LocalDate.now()) || note.getText().length() > 255) {
            throw new IllegalArgumentException("Ngày kiểm kê không được trong tương lai; ghi chú tối đa 255 ký tự.");
        }
        List<InventoryWorkflowDAO.StocktakeCount> counts = countedQuantities.entrySet().stream()
                .map(entry -> {
                    BigDecimal quantity = parseInventoryDecimal(entry.getValue().getText(), "Số lượng thực tế");
                    if (quantity.signum() < 0) {
                        throw new IllegalArgumentException("Số lượng kiểm kê không được âm.");
                    }
                    return new InventoryWorkflowDAO.StocktakeCount(entry.getKey(), quantity);
                })
                .toList();
        return new InventoryWorkflowDAO.StocktakeRequest(
                location.getValue().id(), stocktakeDate.getValue(), note.getText().trim(), counts);
    }

    private java.util.Optional<InventoryLossInput> createInventoryLossDialog() {
        InventoryWorkflowService service = featureServices.inventoryWorkflowService();
        List<InventoryWorkflowDAO.BatchOption> batches = service.findBatches(null).stream()
                .filter(batch -> batch.quantityOnHand().signum() > 0)
                .toList();
        if (batches.isEmpty()) {
            showInformation(Localization.text("Không có lô tồn để điều chỉnh"),
                    Localization.text("Nhập hàng trước khi ghi nhận hư hỏng hoặc hết hạn."));
            return java.util.Optional.empty();
        }

        Dialog<InventoryLossInput> dialog = new Dialog<>();
        dialog.setTitle(Localization.text("Ghi nhận hư hỏng / hết hạn"));
        dialog.setHeaderText(Localization.text("Số lượng được trừ khỏi tồn kho và ghi vào nhật ký."));
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);
        dialog.getDialogPane().getStyleClass().add("form-dialog");

        ComboBox<InventoryWorkflowDAO.BatchOption> batch = new ComboBox<>(
                FXCollections.observableArrayList(batches));
        batch.setMaxWidth(Double.MAX_VALUE);
        batch.getSelectionModel().selectFirst();
        batch.setConverter(new StringConverter<>() {
            @Override
            public String toString(InventoryWorkflowDAO.BatchOption value) {
                return value == null ? "" : value.label();
            }

            @Override
            public InventoryWorkflowDAO.BatchOption fromString(String value) {
                return null;
            }
        });
        ComboBox<InventoryWorkflowDAO.LossType> lossType = new ComboBox<>(
                FXCollections.observableArrayList(InventoryWorkflowDAO.LossType.values()));
        lossType.getSelectionModel().selectFirst();
        lossType.setConverter(new StringConverter<>() {
            @Override
            public String toString(InventoryWorkflowDAO.LossType value) {
                return value == InventoryWorkflowDAO.LossType.EXPIRED
                        ? Localization.text("Hết hạn") : Localization.text("Hư hỏng");
            }

            @Override
            public InventoryWorkflowDAO.LossType fromString(String value) {
                return null;
            }
        });
        TextField quantity = new TextField();
        quantity.setPromptText(Localization.text("Số lượng cần trừ"));
        TextArea note = new TextArea();
        note.setPromptText(Localization.text("Lý do / ghi chú (không bắt buộc)"));
        note.setPrefRowCount(3);
        note.setWrapText(true);

        GridPane fields = new GridPane();
        fields.setHgap(16);
        fields.setVgap(12);
        addCustomerField(fields, 0, "Lô hàng", batch);
        addCustomerField(fields, 1, "Loại biến động", lossType);
        addCustomerField(fields, 2, "Số lượng", quantity);
        addCustomerField(fields, 3, "Ghi chú", note);
        dialog.getDialogPane().setContent(fields);

        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.addEventFilter(ActionEvent.ACTION, event -> {
            try {
                buildInventoryLossInput(batch, lossType, quantity, note);
            } catch (IllegalArgumentException exception) {
                showInformation(Localization.text("Thông tin điều chỉnh chưa hợp lệ"),
                        Localization.text(exception.getMessage()));
                event.consume();
            }
        });
        dialog.setResultConverter(button -> button == ButtonType.OK
                ? buildInventoryLossInput(batch, lossType, quantity, note) : null);
        return dialog.showAndWait();
    }

    private InventoryLossInput buildInventoryLossInput(
            ComboBox<InventoryWorkflowDAO.BatchOption> batch,
            ComboBox<InventoryWorkflowDAO.LossType> lossType,
            TextField quantity,
            TextArea note) {
        if (batch.getValue() == null || lossType.getValue() == null) {
            throw new IllegalArgumentException("Chọn lô hàng và loại biến động.");
        }
        BigDecimal parsedQuantity = parseInventoryDecimal(quantity.getText(), "Số lượng");
        if (parsedQuantity.signum() <= 0 || parsedQuantity.compareTo(batch.getValue().quantityOnHand()) > 0) {
            throw new IllegalArgumentException("Số lượng phải lớn hơn 0 và không vượt quá tồn hiện tại.");
        }
        if (note.getText().length() > 255) {
            throw new IllegalArgumentException("Ghi chú điều chỉnh không được vượt quá 255 ký tự.");
        }
        if (lossType.getValue() == InventoryWorkflowDAO.LossType.EXPIRED
                && (batch.getValue().expiryDate() == null
                || batch.getValue().expiryDate().isAfter(LocalDate.now()))) {
            throw new IllegalArgumentException("Chỉ ghi nhận hết hạn cho lô đã đến hạn sử dụng.");
        }
        return new InventoryLossInput(
                batch.getValue().id(), lossType.getValue(), parsedQuantity, note.getText().trim());
    }

    private BigDecimal parseInventoryDecimal(String raw, String field) {
        try {
            return new BigDecimal(raw == null ? "" : raw.trim().replace(",", "."));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(field + " phải là số hợp lệ.");
        }
    }

    private boolean isValidInventoryQuantity(BigDecimal quantity) {
        BigDecimal normalized = quantity.stripTrailingZeros();
        return normalized.scale() <= 3
                && normalized.precision() - normalized.scale() <= 9;
    }

    private boolean isValidInventoryMoney(BigDecimal amount) {
        BigDecimal normalized = amount.stripTrailingZeros();
        return normalized.scale() <= 2
                && normalized.precision() - normalized.scale() <= 10;
    }

    private Label inventoryColumnHeader(String text, double width) {
        Label label = new Label(Localization.text(text));
        label.setPrefWidth(width);
        label.getStyleClass().add("field-label");
        return label;
    }

    private Label inventoryColumnValue(String text, double width) {
        Label label = new Label(text);
        label.setPrefWidth(width);
        label.setWrapText(true);
        return label;
    }

    private java.util.Optional<List<String>> createLocationDialog(ModuleRow existingRow) {
        List<String> oldValues = existingRow == null
                ? List.of("", "", "", "ACTIVE")
                : existingRow.values();
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(Localization.text(existingRow == null
                ? "Thêm vị trí" : "Chỉnh sửa vị trí"));
        dialog.setHeaderText(Localization.text(existingRow == null
                ? "Tạo vị trí kho" : "Cập nhật vị trí kho"));
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);
        dialog.getDialogPane().getStyleClass().add("form-dialog");

        TextField code = new TextField(oldValues.get(0));
        code.setPromptText(Localization.text("Mã vị trí"));
        code.getStyleClass().add("text-input");
        TextField name = new TextField(oldValues.get(1));
        name.setPromptText(Localization.text("Tên vị trí"));
        name.getStyleClass().add("text-input");
        TextArea description = new TextArea(oldValues.get(2));
        description.setPromptText(Localization.text("Mô tả"));
        description.setPrefRowCount(3);
        description.setWrapText(true);
        description.getStyleClass().add("text-input");
        ComboBox<String> status = new ComboBox<>();
        status.getItems().addAll(
                Localization.text("Đang sử dụng"), Localization.text("Ngừng sử dụng"));
        status.getSelectionModel().select(
                "INACTIVE".equalsIgnoreCase(oldValues.get(3)) ? 1 : 0);

        GridPane fields = new GridPane();
        fields.setHgap(16);
        fields.setVgap(12);
        fields.setPadding(new Insets(12, 0, 8, 0));
        addCustomerField(fields, 0, "Mã vị trí", code);
        addCustomerField(fields, 1, "Tên vị trí", name);
        addCustomerField(fields, 2, "Mô tả", description);
        addCustomerField(fields, 3, "Trạng thái", status);

        VBox content = new VBox(12, fields);
        content.setPrefWidth(620);
        dialog.getDialogPane().setContent(content);
        dialog.setResultConverter(button -> button == ButtonType.OK ? ButtonType.OK : null);
        return dialog.showAndWait()
                .filter(ButtonType.OK::equals)
                .map(ignored -> List.of(
                        code.getText().trim(),
                        name.getText().trim(),
                        description.getText().trim(),
                        status.getSelectionModel().getSelectedIndex() == 0 ? "ACTIVE" : "INACTIVE"));
    }

    private java.util.Optional<List<String>> editRow(
            ModuleDefinition module,
            FeatureModuleService featureService,
            ModuleRow existingRow) {
        return editRow(module, featureService, existingRow, null);
    }

    private java.util.Optional<List<String>> editRow(
            ModuleDefinition module,
            FeatureModuleService featureService,
            ModuleRow existingRow,
            String currentEmployeeName) {
        if ("UC-01".equals(module.id())) {
            return createEmployeeAccountDialog(module);
        }
        if ("UC-13".equals(module.id())) {
            return createLocationDialog(existingRow);
        }
        if ("UC-18".equals(module.id())) {
            return createPromotionDialog(
                    module, (PromotionsLoyaltyService) featureService, existingRow);
        }
        if ("UC-08".equals(module.id())) {
            return createProductDialog(module, existingRow);
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
        List<java.util.function.Supplier<String>> valueReaders = new ArrayList<>();
        List<String> fieldLabels = "UC-04".equals(module.id())
                ? module.formFields() : module.columns();
        for (int index = 0; index < fieldLabels.size(); index++) {
            Label label = new Label(fieldLabels.get(index));
            label.getStyleClass().add("field-label");
            Node input;
            if ("UC-03".equals(module.id()) && index == 3) {
                ComboBox<String> statusInput = new ComboBox<>();
                statusInput.getItems().setAll("PLANNED", "COMPLETED", "CANCELLED");
                statusInput.setValue(existingRow == null ? "PLANNED" : existingRow.values().get(index));
                statusInput.getStyleClass().add("text-input");
                statusInput.setMaxWidth(Double.MAX_VALUE);
                input = statusInput;
                valueReaders.add(statusInput::getValue);
            } else if ("UC-09".equals(module.id()) && index == 5) {
                ComboBox<String> status = new ComboBox<>(FXCollections.observableArrayList(
                        Localization.text("Đang hoạt động"), Localization.text("Ngừng hoạt động")));
                status.setMaxWidth(Double.MAX_VALUE);
                status.getSelectionModel().select(existingRow != null
                        && "INACTIVE".equalsIgnoreCase(existingRow.values().get(index)) ? 1 : 0);
                input = status;
                valueReaders.add(() ->
                        status.getSelectionModel().getSelectedIndex() == 0 ? "ACTIVE" : "INACTIVE");
            } else {
                TextField textField = new TextField();
                textField.setPromptText(fieldLabels.get(index));
                textField.getStyleClass().add("text-input");
                if (existingRow != null) {
                    String value = Localization.text(existingRow.values().get(index));
                    if ("UC-04".equals(module.id()) && index >= 2) {
                        value = formatDisplayDate(value);
                    }
                    textField.setText(value);
                }
                input = textField;
                valueReaders.add(() -> textField.getText().trim());
            }
            if ("UC-04".equals(module.id()) && userRole == UserRole.STAFF
                    && existingRow == null && index == 0) {
                if (input instanceof TextField textField) {
                    textField.setText(currentEmployeeName);
                }
                input.setDisable(true);
            }
            if ("UC-04".equals(module.id()) && existingRow != null && index < 2) {
                input.setDisable(true);
            }
            fields.add(label, 0, index);
            fields.add(input, 1, index);
            GridPane.setHgrow(input, Priority.ALWAYS);
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
                .map(ignored -> valueReaders.stream().map(java.util.function.Supplier::get).toList())
                .map(values -> preserveUnchangedValues(existingRow, values));
    }

    private void createStaffAttendanceDialog(
            ModuleDefinition module,
            FeatureModuleService featureService,
            RenderedTable renderedTable) {
        try {
            String employeeName = ((AttendanceService) featureService).findCurrentEmployeeName();
            editRow(module, featureService, null, employeeName)
                    .ifPresent(values -> runMutation(
                            () -> featureService.create(module, values),
                            () -> refreshRows(renderedTable, featureService, module)));
        } catch (IllegalArgumentException | IllegalStateException | SecurityException
                 | java.util.NoSuchElementException
                 | vn.ninhkieupharma.pharmacy.common.dao.DataAccessException exception) {
            showInformation(Localization.text("Không thể cập nhật dữ liệu"),
                    Localization.text(exception.getMessage()));
        } catch (UnsupportedOperationException exception) {
            showInformation(Localization.text("Thao tác chưa hỗ trợ"),
                    Localization.text(exception.getMessage()));
        }
    }

    private java.util.Optional<List<String>> createProductDialog(
            ModuleDefinition module,
            ModuleRow existingRow) {
        List<String> oldValues = existingRow == null ? List.of() : existingRow.values();
        TextField code = productTextField(module, oldValues, 0, "");
        TextField name = productTextField(module, oldValues, 1, "");
        TextField dosageForm = productTextField(module, oldValues, 3, "");
        TextField unit = productTextField(module, oldValues, 4, "");
        TextField price = productTextField(module, oldValues, 5, "");
        TextField minimumAge = productTextField(module, oldValues, 9, "0");

        ComboBox<String> category = new ComboBox<>();
        category.setMaxWidth(Double.MAX_VALUE);
        category.setPromptText(Localization.text("Chọn danh mục"));
        List<String> categoryNames = new ArrayList<>();
        ModuleDefinition categoriesModule = moduleDefinition("UC-07");
        for (ModuleRow row : featureServices.forModule(categoriesModule).findAll(categoriesModule)) {
            if (row.values().size() >= 3 && isActiveOption(row.values().get(2))) {
                categoryNames.add(row.values().get(0));
            }
        }
        String oldCategory = productValue(oldValues, 2);
        if (!oldCategory.isBlank() && !categoryNames.contains(oldCategory)) {
            categoryNames.add(oldCategory);
        }
        category.getItems().setAll(categoryNames);
        category.getSelectionModel().select(oldCategory);

        List<String> selectedSuppliers = new ArrayList<>();
        String oldSupplierValue = productValue(oldValues, 8);
        if (!oldSupplierValue.isBlank()) {
            for (String supplier : oldSupplierValue.split(",")) {
                String trimmed = supplier.trim();
                if (!trimmed.isEmpty() && !selectedSuppliers.contains(trimmed)) {
                    selectedSuppliers.add(trimmed);
                }
            }
        }
        MenuButton suppliers = new MenuButton();
        suppliers.setMaxWidth(Double.MAX_VALUE);
        suppliers.getStyleClass().add("supplier-multiselect");
        ModuleDefinition suppliersModule = moduleDefinition("UC-09");
        List<ModuleRow> supplierRows =
                featureServices.forModule(suppliersModule).findAll(suppliersModule);
        List<String> supplierNames = new ArrayList<>();
        for (ModuleRow row : supplierRows) {
            if (row.values().size() >= 6
                    && (isActiveOption(row.values().get(5)) || selectedSuppliers.contains(row.values().get(1)))) {
                supplierNames.add(row.values().get(1));
            }
        }
        for (String selected : selectedSuppliers) {
            if (!supplierNames.contains(selected)) {
                supplierNames.add(selected);
            }
        }
        Runnable updateSupplierLabel = () -> suppliers.setText(selectedSuppliers.isEmpty()
                ? Localization.text("Chọn nhà cung cấp")
                : Localization.productText(String.join(", ", selectedSuppliers)));
        for (String supplierName : supplierNames) {
            CheckMenuItem item = new CheckMenuItem(Localization.productText(supplierName));
            item.setSelected(selectedSuppliers.contains(supplierName));
            item.setOnAction(event -> {
                if (item.isSelected()) {
                    if (!selectedSuppliers.contains(supplierName)) {
                        selectedSuppliers.add(supplierName);
                    }
                } else {
                    selectedSuppliers.remove(supplierName);
                }
                updateSupplierLabel.run();
            });
            suppliers.getItems().add(item);
        }
        updateSupplierLabel.run();

        ComboBox<String> prescription = new ComboBox<>(FXCollections.observableArrayList(
                Localization.text("Có"), Localization.text("Không")));
        prescription.setMaxWidth(Double.MAX_VALUE);
        prescription.getSelectionModel().select(
                "YES".equalsIgnoreCase(productValue(oldValues, 6))
                        || "TRUE".equalsIgnoreCase(productValue(oldValues, 6)) ? 0 : 1);
        ComboBox<String> status = new ComboBox<>(FXCollections.observableArrayList(
                Localization.text("Đang hoạt động"), Localization.text("Ngừng hoạt động")));
        status.setMaxWidth(Double.MAX_VALUE);
        status.getSelectionModel().select(
                "INACTIVE".equalsIgnoreCase(productValue(oldValues, 7)) ? 1 : 0);

        GridPane fields = new GridPane();
        fields.setHgap(16);
        fields.setVgap(12);
        fields.setPadding(new Insets(12, 0, 8, 0));
        Node[] inputs = {code, name, category, dosageForm, unit, price, prescription, status, suppliers, minimumAge};
        for (int index = 0; index < inputs.length; index++) {
            Label label = new Label(Localization.text(module.formFields().get(index)));
            label.getStyleClass().add("field-label");
            fields.add(label, 0, index);
            fields.add(inputs[index], 1, index);
            GridPane.setHgrow(inputs[index], Priority.ALWAYS);
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(Localization.text(existingRow == null
                ? module.actionLabel() : "Chỉnh sửa") + (existingRow == null ? "" : " · " + module.title()));
        dialog.setHeaderText(module.title());
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);
        dialog.getDialogPane().getStyleClass().add("form-dialog");
        dialog.getDialogPane().setContent(fields);
        dialog.getDialogPane().setPrefWidth(680);
        dialog.setResultConverter(button -> button == ButtonType.OK ? ButtonType.OK : null);
        return dialog.showAndWait()
                .filter(ButtonType.OK::equals)
                .map(ignored -> List.of(
                        code.getText().trim(),
                        name.getText().trim(),
                        category.getValue() == null ? "" : category.getValue(),
                        dosageForm.getText().trim(),
                        unit.getText().trim(),
                        price.getText().trim(),
                        prescription.getSelectionModel().getSelectedIndex() == 0 ? "Yes" : "No",
                        status.getSelectionModel().getSelectedIndex() == 0 ? "ACTIVE" : "INACTIVE",
                        String.join(", ", selectedSuppliers),
                        minimumAge.getText().trim()));
    }

    private TextField productTextField(
            ModuleDefinition module, List<String> values, int index, String defaultValue) {
        TextField field = new TextField(values.size() > index ? values.get(index) : defaultValue);
        field.getStyleClass().add("text-input");
        field.setPromptText(Localization.text(module.formFields().get(index)));
        return field;
    }

    private String productValue(List<String> values, int index) {
        return values.size() > index ? values.get(index) : "";
    }

    private boolean isActiveOption(String value) {
        return "ACTIVE".equalsIgnoreCase(value)
                || "Active".equalsIgnoreCase(value)
                || "Đang hoạt động".equalsIgnoreCase(value)
                || "Đang hợp tác".equalsIgnoreCase(value);
    }

    private static ModuleDefinition moduleDefinition(String moduleId) {
        return ModuleCatalog.MODULES.stream()
                .filter(module -> module.id().equals(moduleId))
                .findFirst()
                .orElseThrow();
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
        blocked.setDisable(userRole == UserRole.STAFF);
        archived.setDisable(userRole == UserRole.STAFF);
        String oldStatus = oldValues.isEmpty() ? "ACTIVE" : oldValues.get(6);
        blocked.setSelected(oldStatus.equals("BLOCKED") || oldStatus.equals("ARCHIVED_BLOCKED"));
        archived.setSelected(oldStatus.equals("ARCHIVED") || oldStatus.equals("ARCHIVED_BLOCKED"));
        TextField reason = customerTextField(oldValues, 7);
        reason.setPromptText(Localization.text("Bắt buộc nếu hạn chế bán"));
        reason.setDisable(userRole == UserRole.STAFF);

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
                } else if (userRole == UserRole.STAFF
                        || values.subList(6, 8).equals(selectedRow.values().subList(6, 8))) {
                    featureServices.customerLoyaltyService().updateProfile(selectedRow.id(), values);
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
            case "BLOCKED" -> Localization.text("Hạn chế bán");
            case "ARCHIVED" -> Localization.text("Đã lưu trữ");
            case "ARCHIVED_BLOCKED" -> Localization.text("Đã lưu trữ · hạn chế bán");
            default -> Localization.text("Đang hoạt động");
        };
    }

    private void showCustomerHistory(ModuleRow customer) {
        CustomerLoyaltyService customerService = featureServices.customerLoyaltyService();
        List<ModuleRow> purchases;
        List<ModuleRow> loyalty;
        try {
            purchases = customerService.findPurchaseHistory(customer.id());
            loyalty = customerService.findLoyaltyHistory(customer.id());
        } catch (vn.ninhkieupharma.pharmacy.common.dao.DataAccessException | UnsupportedOperationException exception) {
            showInformation(Localization.text("Không thể tải lịch sử khách hàng"),
                    Localization.text(exception.getMessage()));
            return;
        }

        Tab purchasesTab = new Tab(Localization.text("Lịch sử mua"), createCustomerHistoryTable(
                List.of("Mã hóa đơn", "Ngày giao dịch", "Tổng tiền", "Trạng thái"),
                purchases, "Khách hàng chưa có giao dịch.", false));
        Tab loyaltyTab = new Tab(Localization.text("Lịch sử điểm"), createCustomerHistoryTable(
                List.of("Mã hóa đơn", "Ngày giao dịch", "Loại giao dịch", "Điểm thay đổi"),
                loyalty, "Khách hàng chưa có giao dịch điểm.", true));
        TabPane tabs = new TabPane(purchasesTab, loyaltyTab);
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(Localization.text("Lịch sử mua và điểm"));
        dialog.setHeaderText(customer.values().get(1));
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().getStyleClass().add("form-dialog");
        dialog.getDialogPane().setContent(tabs);
        dialog.getDialogPane().setPrefSize(760, 420);
        dialog.showAndWait();
    }

    private TableView<ModuleRow> createCustomerHistoryTable(
            List<String> columns, List<ModuleRow> rows, String emptyMessage, boolean loyaltyHistory) {
        TableView<ModuleRow> historyTable = new TableView<>(FXCollections.observableArrayList(rows));
        historyTable.getStyleClass().add("data-table");
        historyTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        Locale locale = Localization.isEnglish() ? Locale.US : Locale.forLanguageTag("vi-VN");
        NumberFormat numberFormat = NumberFormat.getNumberInstance(locale);
        for (int index = 0; index < columns.size(); index++) {
            final int columnIndex = index;
            TableColumn<ModuleRow, String> column = new TableColumn<>(Localization.text(columns.get(index)));
            column.setCellValueFactory(cell -> {
                String value = cell.getValue().values().get(columnIndex);
                if (loyaltyHistory) {
                    if (columnIndex == 2) {
                        value = Localization.text(value);
                    } else if (columnIndex == 3) {
                        long change = Long.parseLong(value);
                        value = (change > 0 ? "+" : "") + numberFormat.format(change);
                    }
                } else if (columnIndex == 2) {
                    value = numberFormat.format(new BigDecimal(value)) + " ₫";
                } else if (columnIndex == 3) {
                    value = switch (value) {
                        case "PENDING" -> Localization.text("Đang chờ");
                        case "COMPLETED" -> Localization.text("Hoàn tất");
                        case "VOIDED" -> Localization.text("Đã hủy");
                        default -> value;
                    };
                }
                return new javafx.beans.property.ReadOnlyStringWrapper(Localization.text(value));
            });
            historyTable.getColumns().add(column);
        }
        Label emptyState = new Label(Localization.text(emptyMessage));
        emptyState.getStyleClass().add("table-empty-state");
        historyTable.setPlaceholder(emptyState);
        return historyTable;
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
        BigDecimal redemptionVnd = new BigDecimal(oldValues.isEmpty() ? "1000" : oldValues.get(7));
        TextField redemptionValue = promotionMoneyInput(redemptionVnd);

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
            addTierInput(tierInputs, tiers, BigDecimal.ZERO, "1");
        } else if (!oldValues.get(8).isBlank()) {
            for (String encoded : oldValues.get(8).split(";")) {
                String[] parts = encoded.split(":", -1);
                addTierInput(tierInputs, tiers, new BigDecimal(parts[0]), parts[1]);
            }
        }
        Button addTier = new Button(Localization.text("Thêm bậc"));
        addTier.getStyleClass().add("button-secondary");
        addTier.setOnAction(event -> addTierInput(tierInputs, tiers, null, ""));
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
            VBox container, List<TierInput> tiers, BigDecimal thresholdVnd, String rateValue) {
        TextField threshold = promotionMoneyInput(thresholdVnd);
        threshold.setPromptText(Localization.text("Ngưỡng hóa đơn tối thiểu")
                + (Localization.isEnglish() ? " (USD)" : " (VNĐ)"));
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

    private TextField promotionMoneyInput(BigDecimal amountVnd) {
        TextField field = new TextField(amountVnd == null ? "" : formatInputMoney(amountVnd));
        field.setUserData(amountVnd);
        field.getStyleClass().add("text-input");
        return field;
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
        BigDecimal redemptionVnd = promotionInputVnd(redemptionValue, 8);
        List<String> encodedTiers = new ArrayList<>();
        for (TierInput tier : tiers) {
            BigDecimal thresholdVnd = promotionInputVnd(tier.threshold(), 2);
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

    private BigDecimal promotionInputVnd(TextField input, int scale) {
        if (input.getUserData() instanceof BigDecimal previousVnd
                && formatInputMoney(previousVnd).equals(input.getText().trim())) {
            return previousVnd;
        }
        BigDecimal displayAmount = parseDecimalInput(input);
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

    static String formatDisplayDate(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        try {
            LocalDateTime dateTime;
            try {
                dateTime = LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            } catch (DateTimeParseException ignored) {
                dateTime = LocalDateTime.parse(value, ISO_DATE_TIME_WITH_SPACE);
            }
            DateTimeFormatter outputFormat = value.length() > 16
                    ? DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
                    : VIETNAMESE_DATE_TIME_FORMAT;
            return dateTime.format(outputFormat);
        } catch (DateTimeParseException ignored) {
            try {
                return LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE)
                        .format(VIETNAMESE_DATE_FORMAT);
            } catch (DateTimeParseException notAnIsoDate) {
                return value;
            }
        }
    }

    private boolean confirmDelete(ModuleDefinition module) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        boolean promotion = "UC-18".equals(module.id());
        boolean customer = "UC-16".equals(module.id());
        alert.setTitle(Localization.text(promotion ? "Xác nhận vô hiệu hóa"
                : customer ? "Xác nhận lưu trữ"
                : "Xác nhận xóa"));
        alert.setHeaderText(module.title());
        alert.setContentText(Localization.text(promotion
                ? "Bạn có chắc muốn vô hiệu hóa chương trình đang chọn?"
                : customer ? "Hồ sơ được lưu trữ, lịch sử mua hàng và điểm vẫn được giữ."
                        : "Bạn có chắc muốn xóa dòng dữ liệu đang chọn?"));
        alert.getButtonTypes().setAll(ButtonType.CANCEL, ButtonType.OK);
        return alert.showAndWait().filter(ButtonType.OK::equals).isPresent();
    }

    private boolean confirmDeleteLocation(ModuleRow location) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(Localization.text("Xác nhận xóa vị trí"));
        alert.setHeaderText(location.values().get(0) + " · " + location.values().get(1));
        alert.setContentText(Localization.text(
                "Chỉ xóa được vị trí không còn hàng tồn và không có dữ liệu lô hàng, phiếu nhập hoặc kiểm kê tham chiếu."));
        alert.getButtonTypes().setAll(ButtonType.CANCEL, ButtonType.OK);
        return alert.showAndWait().filter(ButtonType.OK::equals).isPresent();
    }

    private boolean confirmPermanentCustomerDeletion(ModuleRow customer) {
        ButtonType delete = new ButtonType(Localization.text("Xóa hồ sơ"), ButtonBar.ButtonData.OK_DONE);
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, Localization.text(
                "Hồ sơ chưa có đơn hàng sẽ bị xóa vĩnh viễn cùng thông tin dị ứng."),
                delete, ButtonType.CANCEL);
        alert.setTitle(Localization.text("Xác nhận xóa"));
        alert.setHeaderText(customer.values().get(1));
        ((Button) alert.getDialogPane().lookupButton(delete)).setDefaultButton(false);
        ((Button) alert.getDialogPane().lookupButton(ButtonType.CANCEL)).setDefaultButton(true);
        return alert.showAndWait().filter(delete::equals).isPresent();
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
        } catch (IllegalArgumentException | IllegalStateException | SecurityException | java.util.NoSuchElementException
                 | vn.ninhkieupharma.pharmacy.common.dao.DataAccessException exception) {
            showInformation(Localization.text("Không thể cập nhật dữ liệu"),
                    Localization.text(exception.getMessage()));
        } catch (UnsupportedOperationException exception) {
            showInformation(Localization.text("Thao tác chưa hỗ trợ"),
                    Localization.text(exception.getMessage()));
        }
    }

    private static boolean isWritableModule(String moduleId) {
        return List.of("UC-01", "UC-02", "UC-03", "UC-04", "UC-07", "UC-08", "UC-09", "UC-13", "UC-16", "UC-18")
                .contains(moduleId);
    }

    static boolean canWriteModule(String moduleId, UserRole role) {
        return role != null && isWritableModule(moduleId)
                && (role == UserRole.ADMIN
                || role == UserRole.STAFF
                && !List.of("UC-03", "UC-07", "UC-08", "UC-09", "UC-13", "UC-18").contains(moduleId));
    }

    static boolean canConfirmInventoryWorkflow(String moduleId, UserRole role) {
        return role == UserRole.ADMIN && List.of("UC-12", "UC-14").contains(moduleId);
    }

    static boolean canDeleteModule(String moduleId, UserRole role) {
        return role == UserRole.ADMIN && isWritableModule(moduleId);
    }

    private SalesCustomerEntry createSalesCustomerEntry(
            ModuleDefinition module, FeatureModuleService featureService, RenderedTable renderedTable) {
        CustomerLoyaltyService customerService = featureServices.customerLoyaltyService();
        SalesService salesService = featureServices.salesService();
        List<SaleProduct> loadedProducts;
        try {
            loadedProducts = salesService.findSaleProducts();
        } catch (vn.ninhkieupharma.pharmacy.common.dao.DataAccessException exception) {
            loadedProducts = List.of();
            showInformation(Localization.text("Không thể tải sản phẩm bán"),
                    exception.getMessage());
        }
        List<SaleProduct> saleProducts = loadedProducts;
        ObservableList<ModuleRow> customers = FXCollections.observableArrayList();
        FilteredList<ModuleRow> filteredCustomers = new FilteredList<>(customers, customer -> true);
        ModuleRow[] selectedCustomer = {null};
        SalePendingOrder[] activePendingSale = {null};
        SimpleBooleanProperty hasSelectedCustomer = new SimpleBooleanProperty(false);
        SimpleBooleanProperty hasPendingSale = new SimpleBooleanProperty(false);
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
            }
            filteredCustomers.setPredicate(customer -> matchesSalesCustomerQuery(customer, query));
            showCustomerSuggestions(suggestionsPopup, customerSearch, suggestions, filteredCustomers, query);
        });
        suggestions.setOnMouseClicked(event -> {
            ModuleRow highlighted = suggestions.getSelectionModel().getSelectedItem();
            if (highlighted != null) {
                selectSalesCustomer(customerService, highlighted, customers, selectedCustomer,
                        hasSelectedCustomer, updatingSearch, customerSearch, suggestionsPopup, suggestions,
                        customerDetails, customerStatus);
            }
        });
        suggestions.setOnKeyPressed(event -> {
            if (event.getCode() == javafx.scene.input.KeyCode.ENTER) {
                ModuleRow highlighted = suggestions.getSelectionModel().getSelectedItem();
                if (highlighted != null) {
                    selectSalesCustomer(customerService, highlighted, customers, selectedCustomer,
                            hasSelectedCustomer, updatingSearch, customerSearch, suggestionsPopup, suggestions,
                            customerDetails, customerStatus);
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
                updatingSearch, customerSearch, suggestionsPopup,
                customerDetails, customerStatus));
        Button addCustomer = new Button(Localization.text("Thêm khách hàng"));
        addCustomer.getStyleClass().add("button-secondary");
        addCustomer.setOnAction(event -> editSalesCustomer(
                customerService, null, customers, selectedCustomer, hasSelectedCustomer, updatingSearch,
                customerSearch, suggestionsPopup, suggestions, customerDetails, customerStatus));
        Button editCustomer = new Button(Localization.text("Sửa thông tin khách"));
        editCustomer.getStyleClass().add("button-secondary");
        editCustomer.disableProperty().bind(hasSelectedCustomer.not());
        editCustomer.setOnAction(event -> {
            if (selectedCustomer[0] != null) {
                editSalesCustomer(customerService, selectedCustomer[0], customers, selectedCustomer,
                        hasSelectedCustomer, updatingSearch, customerSearch, suggestionsPopup, suggestions,
                        customerDetails, customerStatus);
            }
        });
        FlowPane customerActions = new FlowPane(10, 8, addCustomer, editCustomer, refreshCustomers);
        customerActions.setAlignment(Pos.CENTER_LEFT);
        customerSection.getChildren().addAll(
                customerHeading, customerStep, customerSearch,
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
        Runnable refreshCheckout = () -> {
            refreshCheckoutPreview(saleLines, summary.subtotal());
            summary.stockStatus().setText("");
        };
        Runnable refreshAllergyWarnings = () -> {
            updateSaleAllergyWarnings(
                    salesService, selectedCustomer[0], saleLines, summary.status());
            summary.requestedPoints().setDisable(selectedCustomer[0] == null);
            refreshCheckout.run();
        };
        VBox medicationSection = createPrescriptionItemsSection(
                saleProducts, saleLines, refreshAllergyWarnings);
        VBox saleItems = (VBox) medicationSection.getChildren().get(2);
        summary.completeButton().disableProperty().bind(hasPendingSale.not());
        summary.cancelPendingButton().disableProperty().bind(hasPendingSale.not());
        customerDetails.textProperty().addListener((observable, oldValue, newValue) ->
                refreshAllergyWarnings.run());
        refreshAllergyWarnings.run();

        Runnable resetOrder = () -> {
            activePendingSale[0] = null;
            hasPendingSale.set(false);
            customerSearch.clear();
            customerStatus.setText("");
            clearSalesForm(saleLines, saleItems, prescriptionCode, prescriptionDate,
                    doctor, clinic, diagnosis, summary, refreshAllergyWarnings);
            summary.status().setText("");
        };
        summary.newOrderButton().setOnAction(event -> resetOrder.run());

        summary.checkStockButton().setOnAction(event -> {
            Label feedback = summary.stockStatus();
            try {
                var shortages = salesService.previewSaleStock(buildSaleLines(saleLines));
                feedback.getStyleClass().setAll(shortages.isEmpty() ? "helper-text" : "form-error");
                feedback.setText(shortages.isEmpty()
                        ? Localization.text("Đủ tồn ở các lô còn hạn cho số ngày điều trị đã nhập.")
                        : shortages.stream().map(shortage -> shortage.productName() + " · "
                                + Localization.text("Cần ") + shortage.requested().stripTrailingZeros().toPlainString()
                                + ", " + Localization.text("có ") + shortage.available().stripTrailingZeros().toPlainString()
                                + " · " + Localization.text("Lô cần còn hạn đến ") + shortage.requiredExpiry())
                                .collect(java.util.stream.Collectors.joining("\n")));
            } catch (IllegalArgumentException | IllegalStateException | UnsupportedOperationException
                     | vn.ninhkieupharma.pharmacy.common.dao.DataAccessException exception) {
                feedback.setText(exception.getMessage());
                feedback.getStyleClass().setAll("form-error");
            }
        });

        summary.savePendingButton().setOnAction(event -> {
            try {
                SalePendingOrder existing = activePendingSale[0];
                SaleOrderRequest request = buildSaleOrderRequest(
                        selectedCustomer[0], saleLines, prescriptionCode, prescriptionDate,
                        doctor, clinic, diagnosis, summary, existing);
                SalePendingOrder pending = existing == null
                        ? salesService.createPendingSale(authenticatedUserId, request)
                        : salesService.updatePendingSale(authenticatedUserId, existing.id(), existing.revision(), request);
                activePendingSale[0] = pending;
                hasPendingSale.set(true);
                summary.status().setText(Localization.text("Đã lưu đơn Pending ") + pending.saleCode());
                summary.status().getStyleClass().setAll("helper-text");
            } catch (IllegalArgumentException | IllegalStateException
                     | vn.ninhkieupharma.pharmacy.common.dao.DataAccessException exception) {
                showInformation(Localization.text("Không thể lưu đơn đang chờ"),
                        Localization.text(exception.getMessage()));
                return;
            }
            refreshSalesRows(renderedTable, featureService, module);
        });

        summary.completeButton().setOnAction(event -> {
            SalePendingOrder pending = activePendingSale[0];
            if (pending == null) {
                showInformation(Localization.text("Không thể hoàn tất đơn"),
                        Localization.text("Hãy lưu đơn Pending trước khi hoàn tất."));
                return;
            }
            SaleReceipt receipt;
            try {
                SaleOrderRequest request = buildSaleOrderRequest(
                        selectedCustomer[0], saleLines, prescriptionCode, prescriptionDate,
                        doctor, clinic, diagnosis, summary, pending);
                SaleQuote quote = salesService.quotePendingSale(
                        authenticatedUserId, pending.id(), pending.revision(), request);
                if (!quote.warnings().isEmpty() && !confirmAllergyWarnings(quote.warnings())) {
                    return;
                }
                if (!confirmSalePayment(quote.amounts(), summary.section().getScene().getWindow())) {
                    return;
                }
                receipt = salesService.completePendingSale(
                        authenticatedUserId, pending.id(), pending.revision(), request, quote);
            } catch (IllegalArgumentException | IllegalStateException
                     | vn.ninhkieupharma.pharmacy.common.dao.DataAccessException exception) {
                showInformation(Localization.text("Không thể hoàn tất đơn"),
                        Localization.text(exception.getMessage()));
                return;
            }
            resetOrder.run();
            String receiptDetails = formatSaleReceipt(receipt);
            summary.status().setText(receiptDetails);
            summary.status().getStyleClass().setAll("helper-text");
            showInformation(Localization.text("Đã hoàn tất đơn hàng"), receiptDetails);
            refreshSalesRows(renderedTable, featureService, module);
            refreshSalesCustomers(customerService, customers, selectedCustomer, hasSelectedCustomer,
                    updatingSearch, customerSearch, suggestionsPopup, customerDetails, customerStatus);
        });

        summary.cancelPendingButton().setOnAction(event -> {
            SalePendingOrder pending = activePendingSale[0];
            if (pending == null || !confirmCancelPendingSale()) {
                return;
            }
            try {
                salesService.cancelPendingSale(authenticatedUserId, pending.id(), pending.revision());
            } catch (IllegalArgumentException | IllegalStateException
                     | vn.ninhkieupharma.pharmacy.common.dao.DataAccessException exception) {
                showInformation(Localization.text("Không thể hủy đơn đang chờ"),
                        Localization.text(exception.getMessage()));
                return;
            }
            resetOrder.run();
            summary.status().setText(Localization.text("Đơn Pending đã được hủy."));
            summary.status().getStyleClass().setAll("helper-text");
            refreshSalesRows(renderedTable, featureService, module);
        });

        Runnable loadSelectedPending = () -> {
            ModuleRow selected = renderedTable.table().getSelectionModel().getSelectedItem();
            if (selected == null || selected.values().size() < 6
                    || !"PENDING".equalsIgnoreCase(selected.values().get(5))) {
                showInformation(Localization.text("Chưa chọn đơn Pending"),
                        Localization.text("Hãy chọn một đơn đang chờ trong bảng."));
                return;
            }
            try {
                SalePendingOrder pending = salesService.findPendingSale(selected.id());
                UUID customerId = pending.request().customerId().orElse(null);
                customerSearch.clear();
                refreshSalesCustomers(customerService, customers, selectedCustomer, hasSelectedCustomer,
                        updatingSearch, customerSearch, suggestionsPopup, customerDetails, customerStatus);
                ModuleRow customer = customers.stream()
                        .filter(candidate -> candidate.id().equals(customerId))
                        .findFirst()
                        .orElse(null);
                if (customer != null) {
                    selectSalesCustomer(customerService, customer, customers, selectedCustomer,
                            hasSelectedCustomer, updatingSearch, customerSearch, suggestionsPopup, suggestions,
                            customerDetails, customerStatus);
                }
                activePendingSale[0] = pending;
                hasPendingSale.set(true);
                SaleOrderRequest request = pending.request();
                int lineCount = request.lines().size();
                while (saleLines.size() > lineCount) {
                    CheckoutLineControls removed = saleLines.remove(saleLines.size() - 1);
                    saleItems.getChildren().remove(removed.row());
                }
                while (saleLines.size() < lineCount) {
                    addCheckoutLine(saleProducts, saleLines, saleItems, refreshAllergyWarnings);
                }
                boolean missingProduct = false;
                for (int index = 0; index < lineCount; index++) {
                    SaleLineRequest requestLine = request.lines().get(index);
                    CheckoutLineControls controls = saleLines.get(index);
                    SaleProduct product = saleProducts.stream()
                            .filter(candidate -> candidate.id().equals(requestLine.productId()))
                            .findFirst()
                            .orElse(null);
                    controls.product().setValue(product);
                    missingProduct |= product == null;
                    controls.quantity().setText(requestLine.quantity().stripTrailingZeros().toPlainString());
                    controls.dosage().setText(requestLine.dosageInstructions() == null
                            ? "" : requestLine.dosageInstructions());
                    controls.treatmentDays().setText(Integer.toString(requestLine.treatmentDays()));
                }
                prescriptionCode.setText(request.prescriptionCode() == null ? "" : request.prescriptionCode());
                prescriptionDate.setValue(request.prescriptionDate());
                doctor.setText(request.prescribingDoctor() == null ? "" : request.prescribingDoctor());
                clinic.setText(request.clinic() == null ? "" : request.clinic());
                diagnosis.setText(request.diagnosis() == null ? "" : request.diagnosis());
                summary.note().setText(request.note() == null ? "" : request.note());
                boolean legacyRedemption = request.requestedPoints() == SaleOrderRequest.REDEEM_MAXIMUM;
                summary.requestedPoints().setText(legacyRedemption ? "" : Long.toString(request.requestedPoints()));
                summary.requestedPoints().setPromptText(Localization.text(legacyRedemption
                        ? "Đơn cũ dùng tối đa. Nhập 0 để bỏ đổi điểm."
                        : "Nhập số điểm khách muốn đổi"));
                summary.status().setText(Localization.text("Đang chỉnh sửa đơn Pending ")
                        + pending.saleCode());
                summary.status().getStyleClass().setAll("helper-text");
                refreshAllergyWarnings.run();
                if (selectedCustomer[0] == null || missingProduct) {
                    summary.status().setText(Localization.text(
                            "Khách hàng hoặc sản phẩm trong đơn không còn khả dụng. Hãy chọn lại hoặc hủy đơn."));
                    summary.status().getStyleClass().setAll("form-error");
                }
            } catch (IllegalArgumentException | IllegalStateException
                     | vn.ninhkieupharma.pharmacy.common.dao.DataAccessException exception) {
                showInformation(Localization.text("Không thể tải đơn Pending"),
                        Localization.text(exception.getMessage()));
            }
        };

        form.getChildren().addAll(
                customerSection, prescriptionSection, medicationSection, summary.section());
        refreshSalesCustomers(customerService, customers, selectedCustomer, hasSelectedCustomer,
                updatingSearch, customerSearch, suggestionsPopup,
                customerDetails, customerStatus);
        return new SalesCustomerEntry(form, loadSelectedPending);
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

    private void refreshSalesRows(RenderedTable table, FeatureModuleService service, ModuleDefinition module) {
        try {
            refreshRows(table, service, module);
        } catch (vn.ninhkieupharma.pharmacy.common.dao.DataAccessException exception) {
            showInformation(Localization.text("Không thể làm mới danh sách đơn"), exception.getMessage());
        }
    }

    private void refreshSalesCustomers(
            CustomerLoyaltyService customerService,
            ObservableList<ModuleRow> customers,
            ModuleRow[] selectedCustomer,
            SimpleBooleanProperty hasSelectedCustomer,
            boolean[] updatingSearch,
            TextField customerSearch,
            Popup suggestionsPopup,
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
        Label heading = new Label(Localization.text("4. Ưu đãi và lưu đơn"));
        heading.getStyleClass().add("section-title");
        TextArea notes = new TextArea();
        notes.setPromptText(Localization.text("Ghi chú thêm cho đơn thuốc hoặc khách hàng"));
        notes.setPrefRowCount(2);
        notes.setWrapText(true);
        notes.getStyleClass().add("text-input");
        Label pointsLabel = new Label(Localization.text("Điểm muốn đổi"));
        TextField requestedPoints = new TextField("0");
        requestedPoints.setPromptText(Localization.text("Nhập số điểm khách muốn đổi"));
        requestedPoints.setAccessibleText(pointsLabel.getText());
        Label pointsLimit = new Label(Localization.text(
                "Tiền giảm từ điểm tối đa bằng 50% tiền hàng ban đầu."));
        pointsLimit.getStyleClass().add("helper-text");
        Label subtotal = new Label(Localization.text("Tạm tính: 0 ₫"));
        subtotal.getStyleClass().add("invoice-total");
        Label status = new Label(Localization.text(
                "Ưu đãi cố định đủ điều kiện tốt nhất sẽ được áp dụng tự động."));
        status.setWrapText(true);
        status.getStyleClass().add("helper-text");
        Button savePending = new Button(Localization.text("Lưu đơn Pending"));
        savePending.getStyleClass().add("button-primary");
        Button completeOrder = new Button(Localization.text("Tính tiền và xác nhận"));
        completeOrder.getStyleClass().add("button-primary");
        Button cancelPending = new Button(Localization.text("Hủy đơn Pending"));
        cancelPending.getStyleClass().add("button-secondary");
        Button newOrder = new Button(Localization.text("Đơn mới"));
        newOrder.getStyleClass().add("button-secondary");
        Button checkStock = new Button(Localization.text("Kiểm tra tồn theo lô"));
        checkStock.getStyleClass().add("button-secondary");
        Label stockStatus = new Label();
        stockStatus.setWrapText(true);
        stockStatus.managedProperty().bind(stockStatus.textProperty().isNotEmpty());
        stockStatus.visibleProperty().bind(stockStatus.managedProperty());
        FlowPane actions = new FlowPane(10, 8, checkStock, savePending, completeOrder, cancelPending, newOrder);
        section.getChildren().addAll(heading, subtotal, pointsLabel, requestedPoints, pointsLimit, notes,
                status, actions, stockStatus);
        return new CheckoutSummary(
                section, requestedPoints, notes, subtotal, status, stockStatus, checkStock,
                savePending, completeOrder, cancelPending, newOrder);
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
        FlowPane row = new FlowPane(8, 8);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setMaxWidth(Double.MAX_VALUE);
        product.setPrefWidth(280);
        dosage.setPrefWidth(210);
        quantity.setPrefWidth(90);
        treatmentDays.setPrefWidth(100);
        row.getChildren().addAll(product, dosage, quantity, treatmentDays, remove);
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
            BigDecimal quantity = parseCheckoutQuantity(line.quantity().getText());
            int treatmentDays;
            try {
                treatmentDays = Integer.parseInt(line.treatmentDays().getText().trim());
                if (treatmentDays < 1 || treatmentDays > 3650) {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException(
                        Localization.text("Số ngày điều trị phải là số nguyên từ 1 đến 3650."), exception);
            }
            lines.add(new SaleLineRequest(
                    product.id(), quantity, line.dosage().getText().trim(), treatmentDays));
        }
        return List.copyOf(lines);
    }

    static BigDecimal parseCheckoutQuantity(String value) {
        String text = value.trim().replace(',', '.');
        if (text.matches("(?:[0-9]{1,9}(?:\\.[0-9]{1,3})?|\\.[0-9]{1,3})")) {
            BigDecimal quantity = new BigDecimal(text);
            if (quantity.signum() > 0) {
                return quantity;
            }
        }
        throw new IllegalArgumentException(Localization.text(
                "Số lượng phải lớn hơn 0, có tối đa 9 chữ số nguyên và 3 chữ số thập phân."));
    }

    private SaleOrderRequest buildSaleOrderRequest(
            ModuleRow customer,
            List<CheckoutLineControls> lines,
            TextField prescriptionCode,
            DatePicker prescriptionDate,
            TextField doctor,
            TextField clinic,
            TextField diagnosis,
            CheckoutSummary summary,
            SalePendingOrder existing) {
        if (customer == null) {
            throw new IllegalArgumentException(Localization.text("Hãy chọn hồ sơ khách hàng trước khi lưu đơn."));
        }
        try {
            prescriptionDate.commitValue();
        } catch (java.time.DateTimeException exception) {
            throw new IllegalArgumentException(Localization.text("Ngày kê đơn không hợp lệ."), exception);
        }
        return new SaleOrderRequest(
                Optional.of(customer.id()),
                buildSaleLines(lines),
                prescriptionCode.getText().trim(),
                prescriptionDate.getValue(),
                doctor.getText().trim(),
                clinic.getText().trim(),
                diagnosis.getText().trim(),
                summary.note().getText().trim(),
                readRequestedPoints(summary.requestedPoints().getText(), existing),
                List.of());
    }

    private static long readRequestedPoints(String input, SalePendingOrder existing) {
        String text = input.trim();
        if (text.isEmpty()) {
            return existing != null && existing.request().requestedPoints() == SaleOrderRequest.REDEEM_MAXIMUM
                    ? SaleOrderRequest.REDEEM_MAXIMUM : 0;
        }
        try {
            long points = Long.parseLong(text);
            if (points >= 0 && points <= SaleOrderRequest.MAX_POINT_BALANCE) {
                return points;
            }
        } catch (NumberFormatException exception) {
            // Report the accepted range below.
        }
        throw new IllegalArgumentException(Localization.text("Số điểm muốn đổi phải là số nguyên không âm hợp lệ."));
    }

    private void clearSalesForm(
            List<CheckoutLineControls> lines,
            VBox saleItems,
            TextField prescriptionCode,
            DatePicker prescriptionDate,
            TextField doctor,
            TextField clinic,
            TextField diagnosis,
            CheckoutSummary summary,
            Runnable refreshAllergyWarnings) {
        while (lines.size() > 1) {
            CheckoutLineControls removed = lines.remove(lines.size() - 1);
            saleItems.getChildren().remove(removed.row());
        }
        if (!lines.isEmpty()) {
            CheckoutLineControls line = lines.get(0);
            line.product().getSelectionModel().clearSelection();
            line.quantity().setText("1");
            line.dosage().clear();
            line.treatmentDays().setText("1");
        }
        prescriptionCode.clear();
        prescriptionDate.setValue(null);
        doctor.clear();
        clinic.clear();
        diagnosis.clear();
        summary.note().clear();
        summary.requestedPoints().setText("0");
        summary.requestedPoints().setPromptText(Localization.text("Nhập số điểm khách muốn đổi"));
        refreshAllergyWarnings.run();
    }

    private boolean confirmCancelPendingSale() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(Localization.text("Xác nhận hủy đơn"));
        alert.setHeaderText(Localization.text("Hủy đơn Pending"));
        alert.setContentText(Localization.text(
                "Đơn đang chờ sẽ chuyển sang trạng thái đã hủy và không thể chỉnh sửa."));
        alert.getButtonTypes().setAll(ButtonType.CANCEL, ButtonType.OK);
        return alert.showAndWait().filter(ButtonType.OK::equals).isPresent();
    }

    private void showSaleDetails(SalesService service, ModuleRow sale) {
        try {
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle(Localization.text("Chi tiết đơn hàng"));
            dialog.setHeaderText(sale.values().get(0));
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
            dialog.getDialogPane().getStyleClass().add("form-dialog");
            TableView<ModuleRow> details = new TableView<>();
            details.getStyleClass().add("data-table");
            details.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
            List<String> columns = List.of(
                    "Mã sản phẩm", "Sản phẩm", "Đơn vị", "Số lượng",
                    "Đơn giá", "Thành tiền", "Hướng dẫn", "Ngày điều trị");
            for (int index = 0; index < columns.size(); index++) {
                final int columnIndex = index;
                TableColumn<ModuleRow, String> column = new TableColumn<>(Localization.text(columns.get(index)));
                column.setCellValueFactory(cell -> {
                    String value = cell.getValue().values().get(columnIndex);
                    if (columnIndex == 4 || columnIndex == 5) {
                        value = NumberFormat.getNumberInstance(Localization.isEnglish()
                                        ? Locale.US : Locale.forLanguageTag("vi-VN"))
                                .format(new BigDecimal(value)) + " ₫";
                    }
                    return new javafx.beans.property.ReadOnlyStringWrapper(Localization.text(value));
                });
                details.getColumns().add(column);
            }
            details.setItems(FXCollections.observableArrayList(service.findSaleDetails(sale.id())));
            details.setPlaceholder(new Label(Localization.text("Đơn hàng chưa có dòng sản phẩm.")));
            dialog.getDialogPane().setContent(details);
            dialog.getDialogPane().setPrefSize(1000, 480);
            dialog.showAndWait();
        } catch (IllegalArgumentException | IllegalStateException
                 | vn.ninhkieupharma.pharmacy.common.dao.DataAccessException exception) {
            showInformation(Localization.text("Không thể tải chi tiết đơn hàng"),
                    Localization.text(exception.getMessage()));
        }
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
                BigDecimal quantity = parseCheckoutQuantity(line.quantity().getText());
                subtotal = subtotal.add(product.sellingPrice().multiply(quantity).setScale(2, RoundingMode.HALF_UP));
            } catch (IllegalArgumentException ignored) {
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
            Label status) {
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
            status.getStyleClass().setAll("helper-text");
        } catch (vn.ninhkieupharma.pharmacy.common.dao.DataAccessException exception) {
            status.setText(exception.getMessage());
            status.getStyleClass().setAll("form-error");
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

    private boolean confirmSalePayment(SaleReceipt amounts, javafx.stage.Window owner) {
        ButtonType confirm = new ButtonType(Localization.text("Đã thu tiền"), ButtonBar.ButtonData.OK_DONE);
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, formatSaleReceipt(amounts), confirm, ButtonType.CANCEL);
        alert.setTitle(Localization.text("Xác nhận thanh toán"));
        alert.setHeaderText(Localization.text("Số tiền cần thu: ") + formatSaleMoney(amounts.total()));
        alert.initOwner(owner);
        ((Button) alert.getDialogPane().lookupButton(confirm)).setDefaultButton(false);
        ((Button) alert.getDialogPane().lookupButton(ButtonType.CANCEL)).setDefaultButton(true);
        return alert.showAndWait().filter(confirm::equals).isPresent();
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
                        customerDetails, customerStatus);
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
        workspace.setFillHeight(true);
        VBox catalog = new VBox(12);
        catalog.getStyleClass().add("dashboard-panel");
        catalog.setMinWidth(420);
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
        invoice.setPrefWidth(320);
        invoice.setMinWidth(300);
        invoice.setMaxWidth(360);
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
