package hotel;

import hotel.controller.LoginController;
import hotel.controller.MainController;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

    private Stage primaryStage;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        primaryStage.setTitle("Hotel Management System");
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(620);
        showLogin();
    }

    private void showLogin() {
        LoginController loginController = new LoginController(this::showMain);
        Scene scene = new Scene(loginController.getView(), 960, 700);
        String css = getClass().getResource("/hotel/css/hotel.css").toExternalForm();
        scene.getStylesheets().add(css);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void showMain() {
        MainController mainController = new MainController(this::showLogin);
        Scene scene = new Scene(mainController.getRootLayout(), 1200, 750);
        String css = getClass().getResource("/hotel/css/hotel.css").toExternalForm();
        scene.getStylesheets().add(css);
        primaryStage.setScene(scene);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
