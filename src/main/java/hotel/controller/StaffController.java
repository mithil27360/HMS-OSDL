package hotel.controller;

import hotel.dao.IAuthService;
import hotel.model.User;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;

/**
 * Controller for Staff and Account management.
 */
public class StaffController {


    private final IAuthService authService; 

    private VBox view;
    private TableView<User> userTable;

    private TextField nameField, usernameField, emailField, phoneField;
    private PasswordField passField;
    private ComboBox<User.Role> roleCombo;
    private Label formStatus;

    public StaffController(IAuthService authService) {
        this.authService = authService;
        buildView();
    }

    private void buildView() {
        view = new VBox(20);
        view.setPadding(new Insets(30));
        view.setStyle("-fx-background-color: #f4f4f6;");

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("Staff & Account Management");
        title.setStyle("-fx-text-fill: #2c3e50; -fx-font-size: 22px; -fx-font-weight: bold;");
        header.getChildren().add(title);

        HBox mainRow = new HBox(20);
        HBox.setHgrow(mainRow, Priority.ALWAYS);

        VBox formPanel = buildForm();
        formPanel.setMinWidth(290);
        formPanel.setMaxWidth(290);

        VBox tablePanel = buildTablePanel();
        HBox.setHgrow(tablePanel, Priority.ALWAYS);

        mainRow.getChildren().addAll(formPanel, tablePanel);
        view.getChildren().addAll(header, mainRow);
        refresh();
    }

    private VBox buildForm() {
        VBox panel = new VBox(11);
        panel.getStyleClass().add("panel-card");

        Label title = new Label("Add New Account");
        title.setStyle("-fx-text-fill: #3498db; -fx-font-size: 15px; -fx-font-weight: bold;");
        Region div = new Region(); div.getStyleClass().add("gold-divider");

        Label lblRole = new Label("Role");
        lblRole.getStyleClass().add("field-label");
        roleCombo = new ComboBox<>(FXCollections.observableArrayList(User.Role.values()));
        roleCombo.setPromptText("Select role");
        roleCombo.getStyleClass().add("combo-box");
        roleCombo.setMaxWidth(Double.MAX_VALUE);
        roleCombo.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(User.Role r, boolean empty) {
                super.updateItem(r, empty);
                setText(empty || r == null ? null : r.getDisplayName());
            }
        });
        roleCombo.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(User.Role r, boolean empty) {
                super.updateItem(r, empty);
                setText(empty || r == null ? null : r.getDisplayName());
            }
        });

        nameField    = styledField("Full Name", "e.g. John Wick");
        usernameField= styledField("Username", "e.g. babbayaga");
        passField    = new PasswordField();
        passField.setPromptText("Password");
        passField.setStyle(fieldStyle());
        emailField   = styledField("Email", "e.g. j.wick@continental.com");
        phoneField   = styledField("Phone", "e.g. 9876543210");
        phoneField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*"))
                phoneField.setText(newVal.replaceAll("[^\\d]", ""));
            if (phoneField.getText().length() > 10)
                phoneField.setText(phoneField.getText().substring(0, 10));
        });

        formStatus = new Label();
        formStatus.setWrapText(true);
        formStatus.setStyle("-fx-font-size: 11px;");

        Button addBtn = new Button("+ Create Account");
        addBtn.getStyleClass().add("btn-primary");
        addBtn.setMaxWidth(Double.MAX_VALUE);
        addBtn.setOnAction(e -> handleAddUser());

        Button delBtn = new Button("Delete Selected");
        delBtn.getStyleClass().add("btn-danger");
        delBtn.setMaxWidth(Double.MAX_VALUE);
        delBtn.setOnAction(e -> handleDeleteUser());

        panel.getChildren().addAll(
            title, div,
            lblRole, roleCombo,
            fieldLabel("Full Name"), nameField,
            fieldLabel("Username"), usernameField,
            fieldLabel("Password"), passField,
            fieldLabel("Email"), emailField,
            fieldLabel("Phone"), phoneField,
            addBtn, delBtn, formStatus
        );
        return panel;
    }

    private VBox buildTablePanel() {
        VBox panel = new VBox(12);

        Label title = new Label("All Accounts");
        title.setStyle("-fx-text-fill: #2c3e50; -fx-font-size: 16px; -fx-font-weight: bold;");

        userTable = new TableView<>();
        userTable.getStyleClass().add("table-view");
        userTable.setPlaceholder(new Label("No users found"));
        VBox.setVgrow(userTable, Priority.ALWAYS);

        TableColumn<User, String> colRole = new TableColumn<>("Role");
        colRole.setCellValueFactory(d ->
            new javafx.beans.property.SimpleStringProperty(d.getValue().getRole().getDisplayName()));
        colRole.setPrefWidth(110);
        colRole.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setText(null); setStyle(""); return; }
                setText(s);
                String color = s.equals("Admin") ? "#e74c3c" : s.equals("Receptionist") ? "#3498db" : "#2ecc71";
                setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold;");
            }
        });

        TableColumn<User, String> colName = new TableColumn<>("Full Name");
        colName.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        colName.setPrefWidth(150);

        TableColumn<User, String> colUser = new TableColumn<>("Username");
        colUser.setCellValueFactory(new PropertyValueFactory<>("username"));
        colUser.setPrefWidth(120);

        TableColumn<User, String> colEmail = new TableColumn<>("Email");
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colEmail.setPrefWidth(180);

        TableColumn<User, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(d ->
            new javafx.beans.property.SimpleStringProperty(d.getValue().isActive() ? "Active" : "Inactive"));
        colStatus.setPrefWidth(80);
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setText(null); return; }
                setText(s);
                setStyle("-fx-text-fill: " + (s.equals("Active") ? "#2ecc71" : "#e74c3c") + "; -fx-font-weight: bold;");
            }
        });

        userTable.getColumns().setAll(colRole, colName, colUser, colEmail, colStatus);
        userTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        panel.getChildren().addAll(title, userTable);
        return panel;
    }

    private void handleAddUser() {
        User.Role role = roleCombo.getValue();
        String name = nameField.getText().trim();
        String username = usernameField.getText().trim();
        String pass = passField.getText();
        String email = emailField.getText().trim();
        String phone = phoneField.getText().trim();

        if (role == null || name.isEmpty() || username.isEmpty() || pass.isEmpty()) {
            setStatus("Fill all required fields.", false); return;
        }

        if (!email.isEmpty() && !email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            setStatus("Invalid email format.", false);
            return;
        }
        if (!phone.isEmpty() && !phone.matches("\\d{10}")) {
            setStatus("Phone must be 10 digits.", false);
            return;
        }
        if (pass.length() < 6) {
            setStatus("Password must be at least 6 characters.", false);
            return;
        }

        try {
            User user = new User(username, pass, name, email, role);
            user.setPhone(phone); 

            if (authService.addUser(user)) {
                setStatus("Account created for " + name, true);
                clearForm();
                refresh();
            } else {
                setStatus("Username already exists.", false);
            }
        } catch (IllegalArgumentException e) {
            setStatus("Validation Error: " + e.getMessage(), false);
        } catch (Exception e) {
            setStatus("System Error: Contact admin.", false);
            hotel.dao.FileStorage.writeLog("User creation error", e);
        }
    }

    private void clearForm() {
        nameField.clear(); usernameField.clear(); passField.clear();
        emailField.clear(); phoneField.clear(); roleCombo.setValue(null);
    }

    private void handleDeleteUser() {
        User selected = userTable.getSelectionModel().getSelectedItem();
        if (selected == null) { setStatus("✗ Select a user to delete.", false); return; }
        if (selected.getUsername().equals("admin")) { setStatus("✗ Cannot delete the main admin.", false); return; }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
            "Delete account for " + selected.getFullName() + "?", ButtonType.YES, ButtonType.NO);
        alert.setTitle("Confirm Delete");
        alert.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                authService.deleteUser(selected.getUsername());
                setStatus("Account deleted.", true);
                refresh();
            }
        });

    }

    private void setStatus(String msg, boolean ok) {
        formStatus.setText(msg);
        formStatus.setStyle("-fx-font-size: 11px; -fx-text-fill: " + (ok ? "#2ecc71" : "#e74c3c") + ";");
    }

    public void refresh() {
        userTable.setItems(FXCollections.observableArrayList(authService.getAllUsers()));
    }

    private TextField styledField(String label, String prompt) {
        TextField f = new TextField();
        f.setPromptText(prompt);
        f.setStyle(fieldStyle());
        return f;
    }

    private String fieldStyle() {
        return "-fx-background-color: #ffffff; -fx-text-fill: #2c3e50;" +
            "-fx-border-color: #bdc3c7; -fx-border-radius: 6; -fx-background-radius: 6;" +
            "-fx-padding: 10 14; -fx-font-size: 13px;";
    }

    private Label fieldLabel(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("field-label");
        return l;
    }

    public Node getView() { return view; }
}
