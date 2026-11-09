package vn.ninhkieupharma.pharmacy.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import vn.ninhkieupharma.pharmacy.NinhKieuPharmaApp;
import vn.ninhkieupharma.pharmacy.model.UserRole;

import java.io.IOException;
import java.util.Objects;

public class LoginController {
    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private ComboBox<UserRole> roleComboBox;
    @FXML
    private Label errorLabel;

    @FXML
    private void initialize() {
        roleComboBox.getItems().setAll(UserRole.values());
        roleComboBox.getSelectionModel().select(UserRole.ADMIN);
    }

    @FXML
    private void handleLogin() {
        if (usernameField.getText().isBlank() || passwordField.getText().isBlank()) {
            errorLabel.setText("Vui lòng nhập tên đăng nhập và mật khẩu để tiếp tục.");
            errorLabel.setVisible(true);
            errorLabel.setManaged(true);
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(NinhKieuPharmaApp.class.getResource(
                    "/vn/ninhkieupharma/pharmacy/view/main.fxml"));
            Scene scene = new Scene(loader.load(), 1440, 900);
            scene.getStylesheets().add(Objects.requireNonNull(
                    NinhKieuPharmaApp.class.getResource(
                            "/vn/ninhkieupharma/pharmacy/styles/app.css")).toExternalForm());

            MainController controller = loader.getController();
            controller.setSession(usernameField.getText().trim(), roleComboBox.getValue());

            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Ninh Kieu Pharma | Quản lý nhà thuốc");
        } catch (IOException exception) {
            errorLabel.setText("Không thể mở giao diện chính: " + exception.getMessage());
            errorLabel.setVisible(true);
            errorLabel.setManaged(true);
        }
    }
}
