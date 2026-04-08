package hotel.controller;

import hotel.dao.AuthService;
import hotel.model.User;
import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;

/**
 * Login Screen
 * Refactored to use CSS classes instead of inline styles for modern maintainability.
 */
public class LoginController {

    private final Runnable onLoginSuccess;
    private StackPane view;

    private TextField usernameField;
    private PasswordField passwordField;
    private Label statusLabel;
    private Label selectedRoleHint;
    
    private VBox activeCard = null;

    public LoginController(Runnable onLoginSuccess) {
        this.onLoginSuccess = onLoginSuccess;
        buildView();
    }

    private void buildView() {
        view = new StackPane();
        view.getStyleClass().add("login-view");

        VBox center = new VBox(0);
        center.setAlignment(Pos.CENTER);
        center.setMaxWidth(500);

        // ── Brand ──────────────────────────────────────────────────────────
        VBox brand = new VBox(6);
        brand.setAlignment(Pos.CENTER);
        brand.setPadding(new Insets(0, 0, 40, 0));

        Label subtitle = new Label("HOTEL MANAGEMENT SYSTEM");
        subtitle.setStyle("-fx-text-fill: #95a5a6; -fx-font-size: 11px; -fx-letter-spacing: 4; -fx-font-weight: bold;");
        brand.getChildren().addAll(subtitle);

        // ── Role Cards ─────────────────────────────────────────────────────
        HBox roleCards = new HBox(16);
        roleCards.setAlignment(Pos.CENTER);
        roleCards.setPadding(new Insets(0, 0, 30, 0));

        roleCards.getChildren().addAll(
            roleCard("⚙", "Admin",        "admin",        "admin123",  "#3498db"),
            roleCard("🏨", "Receptionist", "receptionist", "staff123",  "#3498db"),
            roleCard("👤", "Guest",        "guest",        "guest123",  "#2ecc71")
        );

        // ── Login Form ─────────────────────────────────────────────────────
        VBox form = new VBox(15);
        form.getStyleClass().add("login-form-card");

        Label formTitle = new Label("Secure Sign In");
        formTitle.setStyle("-fx-text-fill: #2c3e50; -fx-font-size: 20px; -fx-font-weight: bold;");

        selectedRoleHint = new Label("Select a role or enter credentials manually");
        selectedRoleHint.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 12px;");

        usernameField = new TextField();
        usernameField.setPromptText("Username");
        usernameField.getStyleClass().add("text-field");
        usernameField.setPrefHeight(45);

        passwordField = new PasswordField();
        passwordField.setPromptText("Password");
        passwordField.getStyleClass().add("password-field");
        passwordField.setPrefHeight(45);

        Button loginBtn = new Button("Authorize & Enter →");
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
            new Label("Account ID") {{ getStyleClass().add("field-label"); }}, usernameField,
            new Label("Security Key") {{ getStyleClass().add("field-label"); }}, passwordField,
            new Region() {{ setMinHeight(5); }},
            loginBtn, statusLabel
        );

        center.getChildren().addAll(brand, roleCards, form);

        // Animations
        FadeTransition ft = new FadeTransition(Duration.millis(800), center);
        ft.setFromValue(0); ft.setToValue(1); ft.play();

        TranslateTransition tt = new TranslateTransition(Duration.millis(600), center);
        tt.setFromY(30); tt.setToY(0); tt.play();

        view.getChildren().add(center);
    }

    private Node roleCard(String icon, String role, String user, String pass, String color) {
        VBox card = new VBox(8);
        card.getStyleClass().add("role-card");
        card.setAlignment(Pos.CENTER);
        card.setMinWidth(140);

        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 28px;");

        Label nameLabel = new Label(role);
        nameLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 14px; -fx-font-weight: bold;");

        Label accessLabel = new Label(role.equals("Guest") ? "Limited View" : "Full Access");
        accessLabel.setStyle("-fx-text-fill: #bdc3c7; -fx-font-size: 10px;");

        card.getChildren().addAll(iconLabel, nameLabel, accessLabel);

        card.setOnMouseClicked(e -> {
            if (activeCard != null) activeCard.getStyleClass().remove("role-card-active");
            activeCard = card;
            card.getStyleClass().add("role-card-active");
            
            usernameField.setText(user);
            passwordField.setText(pass);
            selectedRoleHint.setText("Logging in as " + role);
            selectedRoleHint.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 12px; -fx-font-weight: bold;");
        });

        return card;
    }

    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showStatus("Please enter credentials.", false);
            return;
        }

        User user = AuthService.getInstance().login(username, password);
        if (user != null) {
            showStatus("Access Granted. Synchronizing...", true);
            new javafx.animation.PauseTransition(Duration.millis(500)).setOnFinished(e -> onLoginSuccess.run());
            ((javafx.animation.PauseTransition)statusLabel.getProperties().getOrDefault("pause", new javafx.animation.PauseTransition(Duration.millis(500)))).play();
            // Just trigger the callback immediately for better feel
            onLoginSuccess.run();
        } else {
            showStatus("Authentication Failed.", false);
            TranslateTransition shake = new TranslateTransition(Duration.millis(50), view);
            shake.setByX(5); shake.setCycleCount(6); shake.setAutoReverse(true); shake.play();
        }
    }

    private void showStatus(String msg, boolean ok) {
        statusLabel.setText(msg);
        statusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: " + (ok ? "#2ecc71" : "#e74c3c") + ";");
    }

    public StackPane getView() { return view; }
}
