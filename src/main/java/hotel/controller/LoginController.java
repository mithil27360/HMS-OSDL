package hotel.controller;

import hotel.dao.AuthService;
import hotel.dao.IAuthService;
import hotel.exception.AuthException;
import hotel.exception.UserNotFoundException;
import hotel.model.User;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;

/**
 * Controller for the Login screen.
 */
public class LoginController {

    private final Runnable onLoginSuccess;
    private final IAuthService authService; 

    private StackPane view;

    private TextField usernameField;
    private PasswordField passwordField;
    private Label statusLabel;
    private Label selectedRoleHint;
    
    private VBox activeCard = null;

    public LoginController(Runnable onLoginSuccess) {
        this.onLoginSuccess = onLoginSuccess;
        this.authService = AuthService.getInstance();
        buildView();
    }

    private void buildView() {
        view = new StackPane();
        view.getStyleClass().add("login-view");

        VBox center = new VBox(0);
        center.setAlignment(Pos.CENTER);
        center.setMaxWidth(500);

        VBox brand = new VBox(6);
        brand.setAlignment(Pos.CENTER);
        brand.setPadding(new Insets(0, 0, 40, 0));

        Label subtitle = new Label("HOTEL MANAGEMENT SYSTEM");
        subtitle.setStyle("-fx-text-fill: #95a5a6; -fx-font-size: 11px; -fx-letter-spacing: 4; -fx-font-weight: bold;");
        brand.getChildren().addAll(subtitle);

        VBox form = new VBox(15);
        form.getStyleClass().add("login-form-card");

        Label formTitle = new Label("Sign In");
        formTitle.setStyle("-fx-text-fill: #2c3e50; -fx-font-size: 20px; -fx-font-weight: bold;");

        selectedRoleHint = new Label("Please enter your credentials to continue");
        selectedRoleHint.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 12px;");

        usernameField = new TextField();
        usernameField.setPromptText("Username");
        usernameField.getStyleClass().add("text-field");
        usernameField.setPrefHeight(45);

        passwordField = new PasswordField();
        passwordField.setPromptText("Password");
        passwordField.getStyleClass().add("password-field");
        passwordField.setPrefHeight(45);

        Button loginBtn = new Button("Sign In");
        loginBtn.getStyleClass().add("btn-primary");
        loginBtn.setMaxWidth(Double.MAX_VALUE);
        loginBtn.setPrefHeight(48);
        loginBtn.setOnAction(e -> handleLogin());

        passwordField.setOnAction(e -> handleLogin());
        usernameField.setOnAction(e -> passwordField.requestFocus());

        statusLabel = new Label();
        statusLabel.setMinWidth(300);
        statusLabel.setAlignment(Pos.CENTER);
        statusLabel.setTextAlignment(TextAlignment.CENTER);

        form.getChildren().addAll(
            formTitle, selectedRoleHint,
            new Label("Username") {{ getStyleClass().add("field-label"); }}, usernameField,
            new Label("Password") {{ getStyleClass().add("field-label"); }}, passwordField,
            new Region() {{ setMinHeight(5); }},
            loginBtn, statusLabel
        );

        center.getChildren().addAll(brand, form);

        FadeTransition ft = new FadeTransition(Duration.millis(800), center);
        ft.setFromValue(0); ft.setToValue(1); ft.play();

        TranslateTransition tt = new TranslateTransition(Duration.millis(600), center);
        tt.setByY(30); tt.setToY(0); tt.play();

        view.getChildren().add(center);
    }

    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showStatus("Please enter credentials.", false);
            return;
        }

        try {
            User user = authService.login(username, password);
            showStatus("Welcome, logging you in...", true);

            PauseTransition pause = new PauseTransition(Duration.millis(300));
            pause.setOnFinished(e -> onLoginSuccess.run());
            pause.play();
        } catch (UserNotFoundException | AuthException e) {
            showStatus(e.getMessage(), false);
            shakeForm();
        } catch (Exception e) {
            showStatus("An unexpected error occurred.", false);
            e.printStackTrace();
        }
    }

    private void shakeForm() {
        TranslateTransition shake = new TranslateTransition(Duration.millis(50), view);
        shake.setByX(5); 
        shake.setCycleCount(6); 
        shake.setAutoReverse(true); 
        shake.play();
    }

    private void showStatus(String msg, boolean ok) {
        statusLabel.setText(msg);
        statusLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: " + (ok ? "#2ecc71" : "#e74c3c") + ";");
    }

    public StackPane getView() { return view; }
}
