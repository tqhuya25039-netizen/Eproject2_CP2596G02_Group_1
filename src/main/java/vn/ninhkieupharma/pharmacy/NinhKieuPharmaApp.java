package vn.ninhkieupharma.pharmacy;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

public class NinhKieuPharmaApp extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(
                NinhKieuPharmaApp.class.getResource("/vn/ninhkieupharma/pharmacy/view/login.fxml"));
        Scene scene = new Scene(loader.load(), 1280, 800);
        scene.getStylesheets().add(Objects.requireNonNull(
                NinhKieuPharmaApp.class.getResource("/vn/ninhkieupharma/pharmacy/styles/app.css"))
                .toExternalForm());

        stage.setTitle("Ninh Kieu Pharma | Quản lý nhà thuốc");
        stage.setMinWidth(1080);
        stage.setMinHeight(700);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
