package vn.ninhkieupharma.pharmacy.features.common.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import vn.ninhkieupharma.pharmacy.NinhKieuPharmaApp;
import vn.ninhkieupharma.pharmacy.auth.controller.LoginController;
import vn.ninhkieupharma.pharmacy.auth.service.AuthenticationService;
import vn.ninhkieupharma.pharmacy.features.common.model.ModuleRow;
import vn.ninhkieupharma.pharmacy.features.common.service.FeatureServices;
import vn.ninhkieupharma.pharmacy.features.common.model.ModuleDefinition;
import vn.ninhkieupharma.pharmacy.auth.model.UserRole;
import vn.ninhkieupharma.pharmacy.features.common.service.ModuleCatalog;
import vn.ninhkieupharma.pharmacy.features.common.service.Localization;
import vn.ninhkieupharma.pharmacy.features.common.view.PageRenderer;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

public class MainController {
    @FXML
    private BorderPane root;
    @FXML
    private VBox navigation;
    @FXML
    private StackPane contentHost;
    @FXML
    private Label breadcrumbLabel;
    @FXML
    private Label userNameLabel;
    @FXML
    private Label roleLabel;
    @FXML
    private Label brandCaptionLabel;
    @FXML
    private Label demoBadgeLabel;
    @FXML
    private Button logoutButton;
    @FXML
    private Button languageButton;

    private UserRole role;
    private String username;
    private Button selectedButton;
    private String selectedModuleId;
    private FeatureServices featureServices;
    private AuthenticationService authenticationService;

    public void setSession(
            String username, UserRole role, FeatureServices featureServices,
            AuthenticationService authenticationService) {
        this.username = username;
        this.role = role;
        this.featureServices = Objects.requireNonNull(featureServices);
        this.authenticationService = Objects.requireNonNull(authenticationService);
        userNameLabel.setText(username);
        roleLabel.setText(role.displayName());
        applyLanguage();
        buildNavigation();
        showDashboard();
    }

    @FXML
    private void handleToggleLanguage() {
        Localization.toggleLanguage();
        applyLanguage();
        buildNavigation();
        if (selectedModuleId == null) {
            showDashboard();
        } else {
            ModuleCatalog.MODULES.stream()
                    .filter(module -> module.id().equals(selectedModuleId))
                    .findFirst()
                    .ifPresent(module -> showModule(module, findNavigationButton(module.id())));
        }
    }

    @FXML
    private void handleLogout() {
        try {
            FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(
                    NinhKieuPharmaApp.class.getResource(
                            "/vn/ninhkieupharma/pharmacy/view/login.fxml")));
            Parent login = loader.load();
            loader.<LoginController>getController().setServices(featureServices, authenticationService);
            Stage stage = (Stage) root.getScene().getWindow();
            SceneFactory.setLoginScene(stage, login);
        } catch (IOException exception) {
            throw new IllegalStateException(
                    Localization.text("Không thể mở màn hình đăng nhập."), exception);
        }
    }

    private void buildNavigation() {
        navigation.getChildren().clear();
        Button dashboardButton = navigationButton("Tổng quan");
        dashboardButton.setOnAction(event -> showDashboard());
        navigation.getChildren().add(dashboardButton);

        for (String group : ModuleCatalog.GROUPS) {
            List<ModuleDefinition> groupModules = ModuleCatalog.MODULES.stream()
                    .filter(module -> module.group().equals(group))
                    .filter(module -> role == UserRole.ADMIN || module.staffAccessible())
                    .toList();
            if (groupModules.isEmpty()) {
                continue;
            }
            Label groupLabel = new Label(Localization.text(group).toUpperCase(
                    Localization.isEnglish() ? java.util.Locale.ENGLISH : java.util.Locale.forLanguageTag("vi-VN")));
            groupLabel.getStyleClass().add("nav-group-label");
            navigation.getChildren().add(groupLabel);
            for (ModuleDefinition module : groupModules) {
                Button button = navigationButton(module.id() + "  " + Localization.text(module.title()));
                button.setOnAction(event -> showModule(module, button));
                navigation.getChildren().add(button);
            }
        }
    }

    private Button navigationButton(String text) {
        Button button = new Button(text);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setAlignment(Pos.CENTER_LEFT);
        button.getStyleClass().add("nav-button");
        return button;
    }

    private void showModule(ModuleDefinition module, Button button) {
        if (role == UserRole.STAFF && !module.staffAccessible()) {
            showAccessDenied();
            return;
        }
        selectedModuleId = module.id();
        setSelected(button);
        ModuleDefinition localizedModule = Localization.localize(module);
        breadcrumbLabel.setText(localizedModule.title());
        contentHost.getChildren().setAll(scrollable(
                new PageRenderer(featureServices).render(localizedModule)));
    }

    private void showDashboard() {
        selectedModuleId = null;
        setSelected(null);
        breadcrumbLabel.setText(Localization.text("Tổng quan"));
        contentHost.getChildren().setAll(scrollable(createDashboard()));
    }

    private ScrollPane scrollable(Node content) {
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.getStyleClass().add("page-scroll");
        return scrollPane;
    }

    private void setSelected(Button button) {
        if (selectedButton != null) {
            selectedButton.getStyleClass().remove("nav-button-selected");
        }
        selectedButton = button;
        if (button != null && !button.getStyleClass().contains("nav-button-selected")) {
            button.getStyleClass().add("nav-button-selected");
        }
    }

    private Node createDashboard() {
        if (role == UserRole.STAFF) {
            return createStaffDashboard();
        }
        VBox page = new VBox(24);
        page.getStyleClass().add("page");
        page.setPadding(new Insets(30, 36, 36, 36));

        VBox heading = new VBox(8);
        Label title = new Label(Localization.text("Xin chào, ") + username);
        title.getStyleClass().add("page-title");
        Label subtitle = new Label(Localization.text("Tổng quan hoạt động nhà thuốc Ninh Kiều"));
        subtitle.getStyleClass().add("page-description");
        heading.getChildren().addAll(title, subtitle);

        HBox cards = new HBox(16);
        cards.getChildren().addAll(
                overviewCard(Localization.text("NHÂN SỰ HOẠT ĐỘNG"),
                        activeEmployeeCount() + Localization.text(" nhân viên"),
                        Localization.text("Hồ sơ đang hoạt động")),
                overviewCard(Localization.text("SẢN PHẨM"),
                        featureServices.forModule(ModuleCatalog.MODULES.get(7))
                                .findAll(ModuleCatalog.MODULES.get(7)).size()
                                + Localization.text(" mặt hàng"),
                        Localization.text("Danh mục đang lưu trong MySQL")),
                overviewCard(Localization.text("BIẾN ĐỘNG KHO"),
                        inventoryMovementQuantity() + Localization.text(" đơn vị"),
                        Localization.text("Tổng biến động đọc từ MySQL")),
                overviewCard(Localization.text("DOANH THU GHI NHẬN"),
                        formatCurrency(recordedRevenue()),
                        invoiceCount() + Localization.text(" hóa đơn đã ghi nhận")));

        VBox workspace = new VBox(14);
        workspace.getStyleClass().add("dashboard-panel");
        Label sectionTitle = new Label(Localization.text("Truy cập nhanh"));
        sectionTitle.getStyleClass().add("section-title");
        Label sectionDescription = new Label(
                Localization.text("Chọn nhóm nghiệp vụ để xem và cập nhật dữ liệu đang lưu trong MySQL."));
        sectionDescription.getStyleClass().add("page-description");
        HBox quickLinks = new HBox(12);
        for (String group : ModuleCatalog.GROUPS) {
            ModuleCatalog.MODULES.stream()
                    .filter(module -> module.group().equals(group))
                    .filter(module -> role == UserRole.ADMIN || module.staffAccessible())
                    .findFirst()
                    .ifPresent(module -> {
                        Button link = new Button(Localization.text(module.title()) + "  →");
                        link.getStyleClass().add("button-secondary");
                        link.setOnAction(event -> showModule(module, null));
                        quickLinks.getChildren().add(link);
                    });
        }
        workspace.getChildren().addAll(sectionTitle, sectionDescription, quickLinks);

        VBox statusPanel = new VBox(10);
        statusPanel.getStyleClass().add("empty-dashboard");
        Label statusTitle = new Label(Localization.text("Kết nối MySQL đang hoạt động"));
        statusTitle.getStyleClass().add("section-title");
        Label statusText = new Label(
                Localization.text("Ứng dụng chỉ kết nối tới schema đã cài đặt; không tạo bảng hoặc thêm dữ liệu nghiệp vụ mẫu."));
        statusText.setWrapText(true);
        statusText.getStyleClass().add("page-description");
        statusPanel.getChildren().addAll(statusTitle, statusText);

        VBox recentActivity = new VBox(14);
        recentActivity.getStyleClass().add("dashboard-panel");
        Label recentTitle = new Label(Localization.text("Hóa đơn gần đây"));
        recentTitle.getStyleClass().add("section-title");
        Label recentDescription = new Label(Localization.text("Hóa đơn gần đây được đọc từ MySQL."));
        recentDescription.getStyleClass().add("page-description");
        recentActivity.getChildren().addAll(recentTitle, recentDescription, recentSalesTable());
        page.getChildren().addAll(heading, cards, workspace, recentActivity, statusPanel);
        return page;
    }

    private Node createStaffDashboard() {
        VBox page = new VBox(24);
        page.getStyleClass().add("page");
        page.setPadding(new Insets(30, 36, 36, 36));

        VBox heading = new VBox(8);
        Label title = new Label(Localization.text("Xin chào, ") + username);
        title.getStyleClass().add("page-title");
        Label subtitle = new Label(Localization.text("Không gian làm việc của nhân viên"));
        subtitle.getStyleClass().add("page-description");
        heading.getChildren().addAll(title, subtitle);

        VBox workspace = new VBox(14);
        workspace.getStyleClass().add("dashboard-panel");
        Label sectionTitle = new Label(Localization.text("Chức năng công việc"));
        sectionTitle.getStyleClass().add("section-title");
        Label sectionDescription = new Label(Localization.text(
                "Các chức năng được cấp quyền cho tài khoản nhân viên của bạn."));
        sectionDescription.getStyleClass().add("page-description");

        FlowPane quickLinks = new FlowPane(12, 12);
        for (ModuleDefinition module : ModuleCatalog.MODULES) {
            if (!module.staffAccessible()) {
                continue;
            }
            Button link = new Button(module.id() + "  " + Localization.text(module.title()) + "  →");
            link.getStyleClass().add("button-secondary");
            link.setOnAction(event -> showModule(module, findNavigationButton(module.id())));
            quickLinks.getChildren().add(link);
        }
        workspace.getChildren().addAll(sectionTitle, sectionDescription, quickLinks);

        VBox accountPanel = new VBox(10);
        accountPanel.getStyleClass().add("empty-dashboard");
        Label accountTitle = new Label(Localization.text("Tài khoản nhân viên"));
        accountTitle.getStyleClass().add("section-title");
        Label accountText = new Label(Localization.text(
                "Bạn đang đăng nhập với quyền Nhân viên. Các báo cáo quản trị và quản lý tài khoản không hiển thị trong giao diện này."));
        accountText.setWrapText(true);
        accountText.getStyleClass().add("page-description");
        accountPanel.getChildren().addAll(accountTitle, accountText);

        VBox connectionPanel = new VBox(10);
        connectionPanel.getStyleClass().add("empty-dashboard");
        Label connectionTitle = new Label(Localization.text("Kết nối MySQL đang hoạt động"));
        connectionTitle.getStyleClass().add("section-title");
        Label connectionText = new Label(Localization.text(
                "Dữ liệu được đọc và cập nhật trực tiếp theo quyền của từng chức năng."));
        connectionText.setWrapText(true);
        connectionText.getStyleClass().add("page-description");
        connectionPanel.getChildren().addAll(connectionTitle, connectionText);

        page.getChildren().addAll(heading, workspace, accountPanel, connectionPanel);
        return page;
    }

    private void showAccessDenied() {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.WARNING);
        alert.setTitle(Localization.text("Không đủ quyền truy cập"));
        alert.setHeaderText(Localization.text("Chức năng này chỉ dành cho quản trị viên."));
        alert.setContentText(Localization.text(
                "Hãy liên hệ quản trị viên nếu bạn cần được cấp thêm quyền."));
        alert.showAndWait();
    }

    private TableView<java.util.List<String>> recentSalesTable() {
        TableView<java.util.List<String>> table = new TableView<>();
        table.getStyleClass().add("data-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        String[] columnNames = {"Mã hóa đơn", "Thời điểm", "Khách hàng", "Tổng tiền"};
        int[] sourceColumns = {0, 1, 2, 4};
        for (int index = 0; index < columnNames.length; index++) {
            final int sourceIndex = sourceColumns[index];
            TableColumn<java.util.List<String>, String> column =
                    new TableColumn<>(Localization.text(columnNames[index]));
            column.setCellValueFactory(cell ->
                    new javafx.beans.property.ReadOnlyStringWrapper(cell.getValue().get(sourceIndex)));
            table.getColumns().add(column);
        }
        List<List<String>> rows = featureServices.forModule(ModuleCatalog.MODULES.get(16))
                .findAll(ModuleCatalog.MODULES.get(16)).stream()
                .map(ModuleRow::values)
                .limit(3)
                .toList();
        table.setItems(FXCollections.observableArrayList(rows.stream()
                .map(row -> row.stream().map(Localization::text).toList())
                .toList()));
        table.setPrefHeight(170);
        return table;
    }

    private Button findNavigationButton(String moduleId) {
        return navigation.getChildren().stream()
                .filter(Button.class::isInstance)
                .map(Button.class::cast)
                .filter(button -> button.getText().startsWith(moduleId + "  "))
                .findFirst()
                .orElse(null);
    }

    private void applyLanguage() {
        brandCaptionLabel.setText(Localization.text("QUẢN LÝ NHÀ THUỐC"));
        demoBadgeLabel.setText(Localization.text("MYSQL"));
        logoutButton.setText(Localization.text("Đăng xuất"));
        languageButton.setText(Localization.languageButtonText());
        roleLabel.setText(role.displayName());
        if (root.getScene() != null && root.getScene().getWindow() instanceof Stage stage) {
            stage.setTitle("Ninh Kieu Pharma | "
                    + Localization.text("Quản lý vận hành nhà thuốc"));
        }
    }

    private VBox overviewCard(String name, String range, String description) {
        VBox card = new VBox(12);
        card.getStyleClass().add("overview-card");
        Label category = new Label(name);
        category.getStyleClass().add("overview-label");
        Label number = new Label(range);
        number.getStyleClass().add("overview-value");
        Label detail = new Label(description);
        detail.setWrapText(true);
        detail.getStyleClass().add("overview-detail");
        card.getChildren().addAll(category, number, detail);
        HBox.setHgrow(card, javafx.scene.layout.Priority.ALWAYS);
        return card;
    }

    private int activeEmployeeCount() {
        ModuleDefinition employees = ModuleCatalog.MODULES.get(0);
        return (int) featureServices.forModule(employees).findAll(employees).stream()
                .filter(row -> row.values().stream()
                        .anyMatch(value -> value.contains("Đang hoạt động")
                                || value.equalsIgnoreCase("Active")
                                || value.equalsIgnoreCase("ACTIVE")))
                .count();
    }

    private long inventoryMovementQuantity() {
        ModuleDefinition movements = ModuleCatalog.MODULES.get(14);
        return Math.round(featureServices.forModule(movements).findAll(movements).stream()
                .mapToDouble(row -> Double.parseDouble(row.values().get(3).replace(',', '.')))
                .sum());
    }

    private long recordedRevenue() {
        ModuleDefinition invoices = ModuleCatalog.MODULES.get(16);
        return featureServices.forModule(invoices).findAll(invoices).stream()
                .mapToLong(row -> parseAmount(row.values().get(4)))
                .sum();
    }

    private int invoiceCount() {
        ModuleDefinition invoices = ModuleCatalog.MODULES.get(16);
        return featureServices.forModule(invoices).findAll(invoices).size();
    }

    private long parseAmount(String value) {
        String digits = value.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) {
            throw new IllegalArgumentException("Invoice total must contain a numeric amount: " + value);
        }
        try {
            return Long.parseLong(digits);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invoice total is outside the supported range: " + value,
                    exception);
        }
    }

    private String formatCurrency(long amount) {
        java.util.Locale locale = Localization.isEnglish()
                ? java.util.Locale.US : java.util.Locale.forLanguageTag("vi-VN");
        return java.text.NumberFormat.getNumberInstance(locale).format(amount) + " ₫";
    }
}

final class SceneFactory {
    private SceneFactory() {
    }

    static void setLoginScene(Stage stage, Parent login) {
        javafx.scene.Scene scene = new javafx.scene.Scene(login, 1280, 800);
        scene.getStylesheets().add(Objects.requireNonNull(
                NinhKieuPharmaApp.class.getResource(
                        "/vn/ninhkieupharma/pharmacy/styles/app.css")).toExternalForm());
        stage.setScene(scene);
    }
}
