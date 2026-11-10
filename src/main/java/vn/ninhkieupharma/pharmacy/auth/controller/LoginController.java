package vn.ninhkieupharma.pharmacy.auth.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import vn.ninhkieupharma.pharmacy.NinhKieuPharmaApp;
import vn.ninhkieupharma.pharmacy.auth.model.User;
import vn.ninhkieupharma.pharmacy.auth.service.AuthenticationService;
import vn.ninhkieupharma.pharmacy.features.common.controller.MainController;
import vn.ninhkieupharma.pharmacy.features.common.service.FeatureServices;
import vn.ninhkieupharma.pharmacy.features.common.service.Localization;

import java.io.IOException;
import java.util.Objects;

public class LoginController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private TextField visiblePasswordField;
    @FXML private Button passwordVisibilityButton;
    @FXML private Label errorLabel;
    @FXML private Label brandSubtitleLabel;
    @FXML private Label staffBenefitLabel;
    @FXML private Label inventoryBenefitLabel;
    @FXML private Label salesBenefitLabel;
    @FXML private Label sampleNoteLabel;
    @FXML private Label welcomeLabel;
    @FXML private Label loginSubtitleLabel;
    @FXML private Label usernameLabel;
    @FXML private Label passwordLabel;
    @FXML private Label helperLabel;
    @FXML private Button loginButton;
    @FXML private Button languageButton;

    private FeatureServices featureServices;
    private AuthenticationService authenticationService;
    @FXML
    private void initialize() {
        visiblePasswordField.textProperty().bindBidirectional(passwordField.textProperty());
        applyLanguage();
    }

    public void setServices(FeatureServices featureServices, AuthenticationService authenticationService) {
        this.featureServices = Objects.requireNonNull(featureServices);
        this.authenticationService = Objects.requireNonNull(authenticationService);
        applyLanguage();
    }

    @FXML
    private void handleToggleLanguage() {
        Localization.toggleLanguage();
        applyLanguage();
        hideError();
    }

    @FXML
    private void handleTogglePasswordVisibility() {
        boolean showPassword = !visiblePasswordField.isVisible();
        visiblePasswordField.setVisible(showPassword);
        visiblePasswordField.setManaged(showPassword);
        passwordField.setVisible(!showPassword);
        passwordField.setManaged(!showPassword);
        passwordVisibilityButton.setText(Localization.text(showPassword ? "Ẩn" : "Hiện"));
        (showPassword ? visiblePasswordField : passwordField).requestFocus();
    }

    @FXML
    private void handleLogin() {
        hideError();
        try {
            User user = authenticationService.authenticate(usernameField.getText(), passwordField.getText())
                    .orElseThrow(() -> new IllegalArgumentException(
                            Localization.text("Tên đăng nhập hoặc mật khẩu không chính xác.")));
            openMainScreen(user);
        } catch (RuntimeException exception) {
            showError(Localization.text(exception.getMessage()));
        } catch (IOException exception) {
            showError(Localization.text("Không thể mở giao diện chính: ") + exception.getMessage());
        }
    }

    private void openMainScreen(User user) throws IOException {
        FXMLLoader loader = new FXMLLoader(NinhKieuPharmaApp.class.getResource(
                "/vn/ninhkieupharma/pharmacy/view/main.fxml"));
        Scene scene = new Scene(loader.load(), 1440, 900);
        scene.getStylesheets().add(Objects.requireNonNull(
                NinhKieuPharmaApp.class.getResource(
                        "/vn/ninhkieupharma/pharmacy/styles/app.css")).toExternalForm());

        MainController controller = loader.getController();
        controller.setSession(user.username(), user.role(), featureServices, authenticationService);

        Stage stage = (Stage) usernameField.getScene().getWindow();
        stage.setScene(scene);
        stage.setTitle("Ninh Kieu Pharma | "
                + Localization.text("Quản lý vận hành nhà thuốc"));
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void hideError() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    private void applyLanguage() {
        brandSubtitleLabel.setText(Localization.text("Hệ thống quản lý vận hành nhà thuốc"));
        staffBenefitLabel.setText(Localization.text("Quản lý nhân sự và ca làm"));
        inventoryBenefitLabel.setText(Localization.text("Theo dõi sản phẩm và tồn kho"));
        salesBenefitLabel.setText(Localization.text("Bán hàng và chăm sóc khách hàng"));
        sampleNoteLabel.setText(Localization.text(
                "Dữ liệu nghiệp vụ được lưu trong MySQL trên máy chủ cục bộ."));
        welcomeLabel.setText(Localization.text("Chào mừng trở lại"));
        loginSubtitleLabel.setText(Localization.text("Đăng nhập để tiếp tục làm việc"));
        usernameLabel.setText(Localization.text("Tên đăng nhập"));
        passwordLabel.setText(Localization.text("Mật khẩu"));
        passwordVisibilityButton.setText(Localization.text(
                visiblePasswordField.isVisible() ? "Ẩn" : "Hiện"));
        helperLabel.setText(Localization.text(
                "Tài khoản quản trị: admin_demo · Đặt mật khẩu lần đầu qua PHARMACY_SAMPLE_ADMIN_PASSWORD."));
        usernameField.setPromptText(Localization.text("Nhập tên đăng nhập"));
        passwordField.setPromptText(Localization.text("Nhập mật khẩu"));
        loginButton.setText(Localization.text("Đăng nhập"));
        languageButton.setText(Localization.languageButtonText());

        if (usernameField.getScene() != null
                && usernameField.getScene().getWindow() instanceof Stage stage) {
            stage.setTitle("Ninh Kieu Pharma | "
                    + Localization.text("Quản lý vận hành nhà thuốc"));
        }
    }
}
