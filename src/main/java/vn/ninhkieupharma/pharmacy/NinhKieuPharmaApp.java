package vn.ninhkieupharma.pharmacy;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import vn.ninhkieupharma.pharmacy.database.DatabaseConnection;
import vn.ninhkieupharma.pharmacy.auth.dao.UserDAOImpl;
import vn.ninhkieupharma.pharmacy.auth.service.AuthenticationService;
import vn.ninhkieupharma.pharmacy.features.common.dao.DataAccessException;
import vn.ninhkieupharma.pharmacy.features.common.dao.RelationalFeatureModuleDAO;
import vn.ninhkieupharma.pharmacy.features.common.service.FeatureServices;
import vn.ninhkieupharma.pharmacy.auth.controller.LoginController;
import vn.ninhkieupharma.pharmacy.features.common.service.Localization;

import java.io.IOException;
import java.util.Objects;

public class NinhKieuPharmaApp extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FeatureServices featureServices;
        AuthenticationService authenticationService;
        try {
            DatabaseConnection.verifySchema();
            featureServices = new FeatureServices(new RelationalFeatureModuleDAO());
            authenticationService = new AuthenticationService(new UserDAOImpl());
        } catch (DataAccessException exception) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("MySQL connection error");
            alert.setHeaderText("Không thể khởi tạo database MySQL");
            alert.setContentText(exception.getMessage() + "\nChi tiết: " + rootCauseMessage(exception));
            alert.showAndWait();
            Platform.exit();
            return;
        } catch (java.sql.SQLException | IllegalStateException exception) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("MySQL connection error");
            alert.setHeaderText("Không thể kết nối database MySQL");
            alert.setContentText(exception.getMessage()
                    + "\nChi tiết: " + rootCauseMessage(exception)
                    + "\nKiểm tra XAMPP MySQL đã chạy và database/schema đã được tạo.");
            alert.showAndWait();
            Platform.exit();
            return;
        }

        FXMLLoader loader = new FXMLLoader(NinhKieuPharmaApp.class.getResource(
                "/vn/ninhkieupharma/pharmacy/view/login.fxml"));
        Scene scene = new Scene(loader.load(), 1280, 800);
        scene.getStylesheets().add(Objects.requireNonNull(
                NinhKieuPharmaApp.class.getResource("/vn/ninhkieupharma/pharmacy/styles/app.css"))
                .toExternalForm());

        loader.<LoginController>getController().setServices(featureServices, authenticationService);
        stage.setTitle("Ninh Kieu Pharma | "
                + Localization.text("Quản lý vận hành nhà thuốc"));
        stage.setMinWidth(1080);
        stage.setMinHeight(700);
        stage.setScene(scene);
        stage.show();
    }

    private static String rootCauseMessage(Throwable exception) {
        Throwable cause = exception;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        return cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
