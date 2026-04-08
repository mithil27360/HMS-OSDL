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
 * Login Screen — role-based login with 3 account cards.
 * JavaFX: Labels, TextField, PasswordField, Button, event handling
 */
public class LoginController {

    private final Runnable onLoginSuccess;
    private StackPane view;

    private TextField usernameField;
    private PasswordField passwordField;
    private Label statusLabel;
    private Label selectedRoleHint;

    public LoginController(Runnable onLoginSuccess) {
        this.onLoginSuccess = onLoginSuccess;
        buildView();
    }

    private void buildView() {
        view = new StackPane();
        view.setStyle("-fx-background-color: #f4f4f6;");

        // Background decorative rectangles
        view.getChildren().add(buildBackground());

        // Main centered content
        VBox center = new VBox(0);
        center.setAlignment(Pos.CENTER);
        center.setMaxWidth(480);

        // ── Brand ──────────────────────────────────────────────────────────
        VBox brand = new VBox(6);
        brand.setAlignment(Pos.CENTER);
        brand.setPadding(new Insets(0, 0, 36, 0));

        Label subtitle = new Label("HOTEL MANAGEMENT SYSTEM");
        subtitle.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 11px; -fx-letter-spacing: 3;");

        brand.getChildren().addAll(subtitle);

        // ── Role Cards ─────────────────────────────────────────────────────
        Label chooseLabel = new Label("Select your role to continue");
        chooseLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 13px;");
        chooseLabel.setPadding(new Insets(0, 0, 14, 0));

        HBox roleCards = new HBox(12);
        roleCards.setAlignment(Pos.CENTER);
        roleCards.setPadding(new Insets(0, 0, 24, 0));

        roleCards.getChildren().addAll(
            roleCard("⚙", "Admin",        "admin",        "admin123",  "#3498db", "#f8f9fa"),
            roleCard("🏨", "Receptionist", "receptionist", "staff123",  "#3498db", "#f8f9fa"),
            roleCard("👤", "Guest",        "guest",        "guest123",  "#2ecc71", "#f8f9fa")
        );

        // ── Login Form ─────────────────────────────────────────────────────
        VBox form = new VBox(12);
        form.setStyle(
            "-fx-background-color: #ffffff;" +
            "-fx-background-radius: 14;" +
            "-fx-padding: 28 32 28 32;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.6), 20, 0, 0, 6);"
        );

        Label formTitle = new Label("Sign In");
        formTitle.setStyle("-fx-text-fill: #2c3e50; -fx-font-size: 18px; -fx-font-weight: bold;");

        selectedRoleHint = new Label("or enter credentials manually");
        selectedRoleHint.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 12px;");

        // Username
        Label lblUser = new Label("Username");
        lblUser.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 12px; -fx-font-weight: bold;");
        usernameField = new TextField();
        usernameField.setPromptText("Enter username");
        usernameField.getStyleClass().add("text-field");
        usernameField.setStyle(
            "-fx-background-color: #ffffff; -fx-text-fill: #2c3e50;" +
            "-fx-border-color: #bdc3c7; -fx-border-radius: 8;" +
            "-fx-background-radius: 8; -fx-padding: 12 14; -fx-font-size: 14px;"
        );

        // Password
        Label lblPass = new Label("Password");
        lblPass.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 12px; -fx-font-weight: bold;");
        passwordField = new PasswordField();
        passwordField.setPromptText("Enter password");
        passwordField.setStyle(
            "-fx-background-color: #ffffff; -fx-text-fill: #2c3e50;" +
            "-fx-border-color: #bdc3c7; -fx-border-radius: 8;" +
            "-fx-background-radius: 8; -fx-padding: 12 14; -fx-font-size: 14px;"
        );

        // Login button
        Button loginBtn = new Button("Sign In  →");
        loginBtn.setMaxWidth(Double.MAX_VALUE);
        loginBtn.setStyle(
            "-fx-background-color: #3498db; -fx-text-fill: #f4f4f6;" +
            "-fx-font-weight: bold; -fx-font-size: 14px;" +
            "-fx-background-radius: 8; -fx-padding: 13 0; -fx-cursor: hand;"
        );
        loginBtn.setOnAction(e -> handleLogin());
        // Also login on Enter key
        passwordField.setOnAction(e -> handleLogin());
        usernameField.setOnAction(e -> passwordField.requestFocus());

        // Status label
        statusLabel = new Label();
        statusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #e74c3c;");
        statusLabel.setMaxWidth(Double.MAX_VALUE);
        statusLabel.setTextAlignment(TextAlignment.CENTER);
        statusLabel.setAlignment(Pos.CENTER);

        form.getChildren().addAll(
            formTitle, selectedRoleHint,
            new Region() {{ setMinHeight(4); }},
            lblUser, usernameField,
            lblPass, passwordField,
            loginBtn, statusLabel
        );

        center.getChildren().addAll(brand, form);

        // Fade-in animation
        FadeTransition ft = new FadeTransition(Duration.millis(600), center);
        ft.setFromValue(0); ft.setToValue(1); ft.play();

        TranslateTransition tt = new TranslateTransition(Duration.millis(500), center);
        tt.setFromY(20); tt.setToY(0); tt.play();

        view.getChildren().add(center);
        StackPane.setAlignment(center, Pos.CENTER);
    }

    private Node roleCard(String icon, String role, String user, String pass,
                          String color, String bg) {
        VBox card = new VBox(8);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(18, 20, 18, 20));
        card.setMinWidth(130);
        card.setStyle(
            "-fx-background-color: " + bg + ";" +
            "-fx-background-radius: 12;" +
            "-fx-border-color: " + bg.replace("00", "33") + ";" +
            "-fx-border-radius: 12; -fx-border-width: 1.5; -fx-cursor: hand;"
        );

        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 26px;");

        Label nameLabel = new Label(role);
        nameLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 13px; -fx-font-weight: bold;");

        Label accessLabel = new Label(role.equals("Admin") ? "Full Access" :
                            role.equals("Receptionist") ? "Operational" : "Limited");
        accessLabel.setStyle("-fx-text-fill: #bdc3c7; -fx-font-size: 10px;");

        card.getChildren().addAll(iconLabel, nameLabel, accessLabel);

        // Click to quick-fill
        card.setOnMouseClicked(e -> {
            usernameField.setText(user);
            passwordField.setText(pass);
            selectedRoleHint.setText("Signed in as " + role);
            selectedRoleHint.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 12px;");
            // Highlight selected card
            card.setStyle(
                "-fx-background-color: " + bg + ";" +
                "-fx-background-radius: 12;" +
                "-fx-border-color: " + color + ";" +
                "-fx-border-radius: 12; -fx-border-width: 2; -fx-cursor: hand;" +
                "-fx-effect: dropshadow(gaussian," + color + "55, 10, 0, 0, 0);"
            );
        });

        card.setOnMouseEntered(e -> card.setStyle(
            "-fx-background-color: " + bg + ";" +
            "-fx-background-radius: 12;" +
            "-fx-border-color: " + color + "80;" +
            "-fx-border-radius: 12; -fx-border-width: 1.5; -fx-cursor: hand;"
        ));
        card.setOnMouseExited(e -> {
            if (!usernameField.getText().equals(user)) {
                card.setStyle(
                    "-fx-background-color: " + bg + ";" +
                    "-fx-background-radius: 12;" +
                    "-fx-border-color: " + bg.replace("00", "33") + ";" +
                    "-fx-border-radius: 12; -fx-border-width: 1.5; -fx-cursor: hand;"
                );
            }
        });

        return card;
    }

    private Node buildBackground() {
        StackPane bg = new StackPane();

        // Decorative blurred accent circles
        for (int i = 0; i < 4; i++) {
            Region circle = new Region();
            double size = 200 + i * 80;
            circle.setPrefSize(size, size);
            circle.setStyle("-fx-background-color: " +
                (i % 2 == 0 ? "rgba(212,175,55,0.04)" : "rgba(52,152,219,0.03)") +
                "; -fx-background-radius: " + size + ";");
            StackPane.setAlignment(circle,
                i == 0 ? Pos.TOP_LEFT : i == 1 ? Pos.BOTTOM_RIGHT :
                i == 2 ? Pos.TOP_RIGHT : Pos.BOTTOM_LEFT);
        }
        return bg;
    }

    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showStatus("Please enter username and password.", false);
            return;
        }

        User user = AuthService.getInstance().login(username, password);
        if (user != null) {
            showStatus("Welcome, " + user.getFullName() + "!", true);
            // Small delay then navigate
            javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(Duration.millis(400));
            pause.setOnFinished(e -> onLoginSuccess.run());
            pause.play();
        } else {
            showStatus("Invalid username or password.", false);
            // Shake animation
            TranslateTransition shake = new TranslateTransition(Duration.millis(60), usernameField);
            shake.setByX(8); shake.setCycleCount(4); shake.setAutoReverse(true); shake.play();
        }
    }

    private void showStatus(String msg, boolean ok) {
        statusLabel.setText(msg);
        statusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: " + (ok ? "#2ecc71" : "#e74c3c") + ";");
    }

    public StackPane getView() { return view; }
}
