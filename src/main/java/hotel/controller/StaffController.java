package hotel.controller;

import hotel.dao.AuthService;
import hotel.model.User;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;

/**
 * Staff Management — Admin only view.
 * Add/remove staff accounts, view all users by role.
 
 */
public class StaffController {

    private final AuthService authService;
    private VBox view;
    private TableView<User> userTable;

    // Form fields
    private TextField nameField, usernameField, emailField, phoneField;
    private PasswordField passField;
    private ComboBox<User.Role> roleCombo;
    private Label formStatus;

    public StaffController(AuthService authService) {
        this.authService = authService;
        buildView();
    }

    private void buildView() {
        view = new VBox(20);
        view.setPadding(new Insets(30));
        view.setStyle("-fx-background-color: #f4f4f6;");

        // Header
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("Staff & Account Management");
        title.setStyle("-fx-text-fill: #2c3e50; -fx-font-size: 22px; -fx-font-weight: bold;");
        Label badge = new Label("ADMIN ONLY");
        badge.setStyle("-fx-background-color: #c0392b; -fx-text-fill: white;" +
            "-fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 3 8; -fx-background-radius: 4;");
        header.getChildren().addAll(title, badge);

        // Role summary cards
        HBox roleCards = buildRoleSummary();

        // Main layout
        HBox mainRow = new HBox(20);
        HBox.setHgrow(mainRow, Priority.ALWAYS);

        VBox formPanel = buildForm();
        formPanel.setMinWidth(290);
        formPanel.setMaxWidth(290);

        VBox tablePanel = buildTablePanel();
        HBox.setHgrow(tablePanel, Priority.ALWAYS);

        mainRow.getChildren().addAll(formPanel, tablePanel);
        view.getChildren().addAll(header, roleCards, mainRow);
        refresh();
    }

    private HBox buildRoleSummary() {
        HBox row = new HBox(14);

        String[][] roles = {
            {"⚙", "Admins",         "#3498db", "ADMIN"},
            {"🏨", "Receptionists", "#3498db",  "RECEPTIONIST"},
            {"👤", "Guests",        "#2ecc71",  "GUEST"}
        };

        for (String[] r : roles) {
            User.Role role = User.Role.valueOf(r[3]);
            long count = authService.getUsersByRole(role).size();

            VBox card = new VBox(4);
            card.setAlignment(Pos.CENTER_LEFT);
            card.setPadding(new Insets(14, 20, 14, 20));
            card.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 10;" +
                "-fx-border-color: #bdc3c7; -fx-border-radius: 10; -fx-border-width: 1;");
            card.setMinWidth(160);

            Label icon = new Label(r[0] + "  " + r[1]);
            icon.setStyle("-fx-text-fill: " + r[2] + "; -fx-font-size: 13px; -fx-font-weight: bold;");
            Label cnt = new Label(count + " account" + (count != 1 ? "s" : ""));
            cnt.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 12px;");
            Label desc = new Label(User.Role.valueOf(r[3]).getDescription());
            desc.setStyle("-fx-text-fill: #bdc3c7; -fx-font-size: 10px;");
            desc.setWrapText(true);

            card.getChildren().addAll(icon, cnt, desc);
            row.getChildren().add(card);
        }
        return row;
    }

    private VBox buildForm() {
        VBox panel = new VBox(11);
        panel.getStyleClass().add("panel-card");

        Label title = new Label("Add New Account");
        title.setStyle("-fx-text-fill: #3498db; -fx-font-size: 15px; -fx-font-weight: bold;");
        Region div = new Region(); div.getStyleClass().add("gold-divider");

        // Role ComboBox
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

        nameField    = styledField("Full Name", "e.g. Priya Sharma");
        usernameField= styledField("Username", "e.g. priya01");
        passField    = new PasswordField();
        passField.setPromptText("Password");
        passField.setStyle(fieldStyle());
        emailField   = styledField("Email", "e.g. priya@hotel.com");
        phoneField   = styledField("Phone", "e.g. 9876543210");

        Label lblName = fieldLabel("Full Name");
        Label lblUser = fieldLabel("Username");
        Label lblPass = fieldLabel("Password");
        Label lblEmail= fieldLabel("Email");
        Label lblPhone= fieldLabel("Phone");

        formStatus = new Label();
        formStatus.setWrapText(true);
        formStatus.setStyle("-fx-font-size: 12px;");

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
            lblName, nameField,
            lblUser, usernameField,
            lblPass, passField,
            lblEmail, emailField,
            lblPhone, phoneField,
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
                String color = s.equals("Admin") ? "#3498db" : s.equals("Receptionist") ? "#3498db" : "#2ecc71";
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

        userTable.getColumns().addAll(colRole, colName, colUser, colEmail, colStatus);
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

        if (role == null || name.isEmpty() || username.isEmpty() || pass.isEmpty()) {
            setStatus("✗ Fill all required fields.", false); return;
        }

        User user = new User(username, pass, name, email, role);
        user.setPhone(phoneField.getText().trim());

        if (authService.addUser(user)) {
            setStatus("✓ Account created for " + name, true);
            nameField.clear(); usernameField.clear(); passField.clear();
            emailField.clear(); phoneField.clear(); roleCombo.setValue(null);
            refresh();
        } else {
            setStatus("✗ Username already exists.", false);
        }
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
                setStatus("✓ Account deleted.", true);
                refresh();
            }
        });
    }

    private void setStatus(String msg, boolean ok) {
        formStatus.setText(msg);
        formStatus.setStyle("-fx-font-size: 12px; -fx-text-fill: " + (ok ? "#2ecc71" : "#e74c3c") + ";");
    }

    public void refresh() {
        userTable.setItems(FXCollections.observableArrayList(authService.getAllUsers()));
    }

    // Helpers
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
