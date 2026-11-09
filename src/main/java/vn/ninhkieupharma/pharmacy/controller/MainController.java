package vn.ninhkieupharma.pharmacy.controller;

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
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import vn.ninhkieupharma.pharmacy.NinhKieuPharmaApp;
import vn.ninhkieupharma.pharmacy.model.ModuleDefinition;
import vn.ninhkieupharma.pharmacy.model.UserRole;
import vn.ninhkieupharma.pharmacy.service.DemoData;
import vn.ninhkieupharma.pharmacy.service.ModuleCatalog;
import vn.ninhkieupharma.pharmacy.view.PageRenderer;

import java.io.IOException;
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

    private UserRole role;
    private String username;
    private Button selectedButton;

    public void setSession(String username, UserRole role) {
        this.username = username;
        this.role = role;
        userNameLabel.setText(username);
        roleLabel.setText(role.displayName());
        buildNavigation();
        showDashboard();
    }

    @FXML
    private void handleLogout() {
        try {
            Parent login = FXMLLoader.load(Objects.requireNonNull(
                    NinhKieuPharmaApp.class.getResource(
                            "/vn/ninhkieupharma/pharmacy/view/login.fxml")));
            Stage stage = (Stage) root.getScene().getWindow();
            SceneFactory.setLoginScene(stage, login);
        } catch (IOException exception) {
            throw new IllegalStateException("Không thể mở màn hình đăng nhập.", exception);
        }
    }

    private void buildNavigation() {
        navigation.getChildren().clear();
        Button dashboardButton = navigationButton("Tổng quan");
        dashboardButton.setOnAction(event -> showDashboard());
        navigation.getChildren().add(dashboardButton);

        for (String group : ModuleCatalog.GROUPS) {
            Label groupLabel = new Label(group.toUpperCase());
            groupLabel.getStyleClass().add("nav-group-label");
            navigation.getChildren().add(groupLabel);
            for (ModuleDefinition module : ModuleCatalog.MODULES) {
                if (!module.group().equals(group)
                        || role == UserRole.STAFF && !module.staffAccessible()) {
                    continue;
                }
                Button button = navigationButton(module.id() + "  " + module.title());
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
        setSelected(button);
        breadcrumbLabel.setText(module.title());
        contentHost.getChildren().setAll(scrollable(new PageRenderer().render(module)));
    }

    private void showDashboard() {
        setSelected(null);
        breadcrumbLabel.setText("Tổng quan");
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
        VBox page = new VBox(24);
        page.getStyleClass().add("page");
        page.setPadding(new Insets(30, 36, 36, 36));

        VBox heading = new VBox(8);
        Label title = new Label("Xin chào, " + username);
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Tổng quan hoạt động nhà thuốc Ninh Kiều");
        subtitle.getStyleClass().add("page-description");
        heading.getChildren().addAll(title, subtitle);

        HBox cards = new HBox(16);
        cards.getChildren().addAll(
                overviewCard("NHÂN SỰ HOẠT ĐỘNG", DemoData.activeEmployees() + " nhân viên",
                        "Trong 5 hồ sơ mẫu"),
                overviewCard("SẢN PHẨM", DemoData.totalProducts() + " mặt hàng",
                        "Thuộc 5 danh mục minh họa"),
                overviewCard("TỒN KHO", DemoData.totalStock() + " đơn vị",
                        "Tổng tồn của 6 mặt hàng mẫu"),
                overviewCard("DOANH THU HÔM NAY", DemoData.todayRevenue(),
                        "Theo 42 hóa đơn minh họa"));

        VBox workspace = new VBox(14);
        workspace.getStyleClass().add("dashboard-panel");
        Label sectionTitle = new Label("Truy cập nhanh");
        sectionTitle.getStyleClass().add("section-title");
        Label sectionDescription = new Label(
                "Chọn nhóm nghiệp vụ để xem bảng và báo cáo với dữ liệu mẫu.");
        sectionDescription.getStyleClass().add("page-description");
        HBox quickLinks = new HBox(12);
        for (String group : ModuleCatalog.GROUPS) {
            ModuleCatalog.MODULES.stream()
                    .filter(module -> module.group().equals(group))
                    .filter(module -> role == UserRole.ADMIN || module.staffAccessible())
                    .findFirst()
                    .ifPresent(module -> {
                        Button link = new Button(module.title() + "  →");
                        link.getStyleClass().add("button-secondary");
                        link.setOnAction(event -> showModule(module, null));
                        quickLinks.getChildren().add(link);
                    });
        }
        workspace.getChildren().addAll(sectionTitle, sectionDescription, quickLinks);

        VBox statusPanel = new VBox(10);
        statusPanel.getStyleClass().add("empty-dashboard");
        Label statusTitle = new Label("Đang sử dụng dữ liệu mẫu");
        statusTitle.getStyleClass().add("section-title");
        Label statusText = new Label(
                "Dữ liệu minh họa phục vụ xem giao diện, không phải dữ liệu nhà thuốc thật. "
                        + "Thêm, xóa, thanh toán và chỉnh sửa hiện không lưu vào cơ sở dữ liệu.");
        statusText.setWrapText(true);
        statusText.getStyleClass().add("page-description");
        statusPanel.getChildren().addAll(statusTitle, statusText);

        VBox recentActivity = new VBox(14);
        recentActivity.getStyleClass().add("dashboard-panel");
        Label recentTitle = new Label("Hóa đơn gần đây");
        recentTitle.getStyleClass().add("section-title");
        Label recentDescription = new Label("Các giao dịch dưới đây đều là dữ liệu mẫu.");
        recentDescription.getStyleClass().add("page-description");
        recentActivity.getChildren().addAll(recentTitle, recentDescription, recentSalesTable());
        page.getChildren().addAll(heading, cards, workspace, recentActivity, statusPanel);
        return page;
    }

    private TableView<java.util.List<String>> recentSalesTable() {
        TableView<java.util.List<String>> table = new TableView<>();
        table.getStyleClass().add("data-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        String[] columnNames = {"Mã hóa đơn", "Thời điểm", "Khách hàng", "Tổng tiền"};
        int[] sourceColumns = {0, 1, 2, 4};
        for (int index = 0; index < columnNames.length; index++) {
            final int sourceIndex = sourceColumns[index];
            TableColumn<java.util.List<String>, String> column = new TableColumn<>(columnNames[index]);
            column.setCellValueFactory(cell ->
                    new javafx.beans.property.ReadOnlyStringWrapper(cell.getValue().get(sourceIndex)));
            table.getColumns().add(column);
        }
        table.setItems(FXCollections.observableArrayList(
                DemoData.rowsFor("UC-17").subList(0, 3)));
        table.setPrefHeight(170);
        return table;
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
