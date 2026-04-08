package hotel.controller;

import hotel.dao.AuthService;
import hotel.dao.IAuthService;
import hotel.exception.AuthException;
import hotel.exception.UserNotFoundException;
import hotel.model.User;
import hotel.util.ValidationUtils;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;
import java.util.stream.Stream;


public class LoginController {

    private final Runnable onLoginSuccess;
    private final IAuthService authService; 

    private StackPane view;

    private TextField usernameField, signupNameField, signupUserField, signupEmailField, signupPhoneField;
    private PasswordField passwordField, signupPassField;
    private Label statusLabel, signupStatusLabel;
    private Label selectedRoleHint;
    
    private VBox loginCard;
    private VBox signupCard;
    private StackPane cardContainer;

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
        
        cardContainer = new StackPane();
        cardContainer.setAlignment(Pos.CENTER);

        loginCard = buildLoginForm();
        signupCard = buildSignUpForm();
        signupCard.setVisible(false);
        signupCard.setManaged(false);

        cardContainer.getChildren().addAll(loginCard, signupCard);
        center.getChildren().addAll(brand, cardContainer);

        FadeTransition ft = new FadeTransition(Duration.millis(800), center);
        ft.setFromValue(0); ft.setToValue(1); ft.play();

        TranslateTransition tt = new TranslateTransition(Duration.millis(600), center);
        tt.setByY(30); tt.setToY(0); tt.play();

        view.getChildren().add(center);
    }

    private VBox buildLoginForm() {
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

        Hyperlink signUpLink = new Hyperlink("New here? Create Account");
        signUpLink.setStyle("-fx-text-fill: #3498db; -fx-font-size: 12px;");
        signUpLink.setOnAction(e -> toggleForm(false));

        form.getChildren().addAll(
            formTitle, selectedRoleHint,
            new Label("Username") {{ getStyleClass().add("field-label"); }}, usernameField,
            new Label("Password") {{ getStyleClass().add("field-label"); }}, passwordField,
            new Region() {{ setMinHeight(5); }},
            loginBtn, statusLabel,
            new StackPane(signUpLink) {{ setPadding(new Insets(10,0,0,0)); }}
        );
        return form;
    }

    private VBox buildSignUpForm() {
        VBox form = new VBox(12);
        form.getStyleClass().add("login-form-card");

        Label formTitle = new Label("Guest Registry");
        formTitle.setStyle("-fx-text-fill: #2c3e50; -fx-font-size: 20px; -fx-font-weight: bold;");
        
        signupNameField = new TextField(); signupNameField.setPromptText("Full Name");
        signupUserField = new TextField(); signupUserField.setPromptText("Choose Username");
        signupEmailField = new TextField(); signupEmailField.setPromptText("Email Address");
        signupPhoneField = new TextField(); signupPhoneField.setPromptText("Phone Number");
        signupPassField = new PasswordField(); signupPassField.setPromptText("Password (min 6 chars)");
        
        Stream.of(signupNameField, signupUserField, signupEmailField, signupPhoneField, signupPassField).forEach(f -> {
            f.getStyleClass().add("text-field");
            f.setPrefHeight(40);
        });

        Button createBtn = new Button("Create Guest Account");
        createBtn.getStyleClass().add("btn-success");
        createBtn.setMaxWidth(Double.MAX_VALUE);
        createBtn.setPrefHeight(45);
        createBtn.setOnAction(e -> handleSignUp());

        Hyperlink backLink = new Hyperlink("Already have an account? Sign In");
        backLink.setStyle("-fx-text-fill: #3498db; -fx-font-size: 12px;");
        backLink.setOnAction(e -> toggleForm(true));

        signupStatusLabel = new Label();
        signupStatusLabel.setMinWidth(300);
        signupStatusLabel.setAlignment(Pos.CENTER);

        form.getChildren().addAll(
            formTitle,
            signupNameField, signupUserField, signupEmailField, signupPhoneField, signupPassField,
            new Region() {{ setMinHeight(5); }},
            createBtn, signupStatusLabel,
            new StackPane(backLink) {{ setPadding(new Insets(10,0,0,0)); }}
        );
        return form;
    }

    private void toggleForm(boolean showLogin) {
        VBox out = showLogin ? signupCard : loginCard;
        VBox in = showLogin ? loginCard : signupCard;

        FadeTransition fadeOut = new FadeTransition(Duration.millis(200), out);
        fadeOut.setFromValue(1.0); fadeOut.setToValue(0.0);
        
        fadeOut.setOnFinished(e -> {
            out.setVisible(false);
            out.setManaged(false);
            in.setVisible(true);
            in.setManaged(true);
            in.setOpacity(0);
            
            FadeTransition fadeIn = new FadeTransition(Duration.millis(200), in);
            fadeIn.setFromValue(0.0); fadeIn.setToValue(1.0);
            fadeIn.play();

            TranslateTransition slide = new TranslateTransition(Duration.millis(300), in);
            slide.setFromY(20); slide.setToY(0);
            slide.play();
        });
        fadeOut.play();
    }

    private void handleSignUp() {
        String name = signupNameField.getText().trim();
        String user = signupUserField.getText().trim();
        String mail = signupEmailField.getText().trim();
        String phone = signupPhoneField.getText().trim();
        String pass = signupPassField.getText();

        if (!ValidationUtils.isValidName(name)) {
            showSignUpStatus("Name must be 2-50 letters only.", false);
            return;
        }
        if (!ValidationUtils.isValidUsername(user)) {
            showSignUpStatus("Invalid username format.", false);
            return;
        }
        if (!ValidationUtils.isValidEmail(mail)) {
            showSignUpStatus("Invalid email format.", false);
            return;
        }
        if (!phone.isEmpty() && !ValidationUtils.isValidPhone(phone)) {
            showSignUpStatus("Phone must be 10 digits.", false);
            return;
        }
        if (!ValidationUtils.isValidPassword(pass)) {
            showSignUpStatus("Password must be at least 6 chars.", false);
            return;
        }

        try {
            User newUser = new User(user, pass, name, mail, User.Role.GUEST);
            newUser.setPhone(phone);
            
            if (authService.addUser(newUser)) {
                showSignUpStatus("Account created! Logging in...", true);
                
                
                authService.login(user, pass);
                PauseTransition pause = new PauseTransition(Duration.millis(800));
                pause.setOnFinished(e -> onLoginSuccess.run());
                pause.play();
            } else {
                showSignUpStatus("Username already taken.", false);
            }
        } catch (Exception e) {
            showSignUpStatus("Registration failed.", false);
            e.printStackTrace();
        }
    }

    private void showSignUpStatus(String msg, boolean ok) {
        signupStatusLabel.setText(msg);
        signupStatusLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: " + (ok ? "#2ecc71" : "#e74c3c") + ";");
    }

    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showStatus("Please enter credentials.", false);
            return;
        }

        try {
            authService.login(username, password);
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
