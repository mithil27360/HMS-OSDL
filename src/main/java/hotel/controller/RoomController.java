package hotel.controller;

import hotel.dao.HotelService;
import hotel.model.Room;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;

/**
 * Room Management View
 * Implements: Add Room, View All Rooms, Filter by type, Sort, Delete
 */
public class RoomController {

    private final HotelService hotelService;
    private final MainController mainController;
    private VBox view;

    private TableView<Room> roomTable;
    private Label statusLabel;
    private ComboBox<String> filterCombo;
    private ComboBox<String> sortCombo;

    // Form fields
    private TextField roomNumField;
    private ComboBox<Room.RoomType> typeCombo;
    private TextField priceField;
    private Label formStatus;

    public RoomController(HotelService hotelService, MainController mainController) {
        this.hotelService = hotelService;
        this.mainController = mainController;
        buildView();
    }

    private void buildView() {
        view = new VBox(20);
        view.setPadding(new Insets(30));
        view.setStyle("-fx-background-color: #f4f4f6;");

        // ─── Header ───────────────────────────────────────────────────────────
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("Room Management");
        title.setStyle("-fx-text-fill: #2c3e50; -fx-font-size: 22px; -fx-font-weight: bold;");
        header.getChildren().add(title);

        // ─── Main content: form (left) + table (right) ────────────────────────
        HBox mainContent = new HBox(20);
        HBox.setHgrow(mainContent, Priority.ALWAYS);

        // Left: Add Room Form
        VBox formPanel = buildAddRoomForm();
        formPanel.setMinWidth(280);
        formPanel.setMaxWidth(280);

        // Right: Table + filters
        VBox tablePanel = buildTablePanel();
        HBox.setHgrow(tablePanel, Priority.ALWAYS);

        mainContent.getChildren().addAll(formPanel, tablePanel);

        view.getChildren().addAll(header, mainContent);
        refresh();
    }

    private VBox buildAddRoomForm() {
        VBox panel = new VBox(14);
        panel.getStyleClass().add("panel-card");

        Label title = new Label("Add New Room");
        title.setStyle("-fx-text-fill: #3498db; -fx-font-size: 15px; -fx-font-weight: bold;");

        Region div = new Region();
        div.getStyleClass().add("gold-divider");

        // Room Number
        Label lblNum = new Label("Room Number");
        lblNum.getStyleClass().add("field-label");
        roomNumField = new TextField();
        roomNumField.setPromptText("e.g. 103");
        roomNumField.getStyleClass().add("text-field");

        // Room Type (ComboBox - Week 9)
        Label lblType = new Label("Room Type");
        lblType.getStyleClass().add("field-label");
        typeCombo = new ComboBox<>(FXCollections.observableArrayList(Room.RoomType.values()));
        typeCombo.setPromptText("Select type");
        typeCombo.getStyleClass().add("combo-box");
        typeCombo.setMaxWidth(Double.MAX_VALUE);

        // Auto-fill price on type selection (Event Handling - Week 9)
        typeCombo.setOnAction(e -> {
            Room.RoomType selected = typeCombo.getValue();
            if (selected != null) {
                priceField.setText(String.valueOf(selected.getBasePrice()));
            }
        });

        // Price
        Label lblPrice = new Label("Price / Night (₹)");
        lblPrice.getStyleClass().add("field-label");
        priceField = new TextField();
        priceField.setPromptText("e.g. 2500");
        priceField.getStyleClass().add("text-field");

        // Status
        formStatus = new Label();
        formStatus.setWrapText(true);
        formStatus.setStyle("-fx-font-size: 12px;");

        // Add Button
        Button addBtn = new Button("+ Add Room");
        addBtn.getStyleClass().add("btn-primary");
        addBtn.setMaxWidth(Double.MAX_VALUE);
        addBtn.setOnAction(e -> handleAddRoom());

        // Delete selected
        Button deleteBtn = new Button("Delete Selected");
        deleteBtn.getStyleClass().add("btn-danger");
        deleteBtn.setMaxWidth(Double.MAX_VALUE);
        deleteBtn.setOnAction(e -> handleDeleteRoom());

        panel.getChildren().addAll(title, div,
            lblNum, roomNumField,
            lblType, typeCombo,
            lblPrice, priceField,
            addBtn, deleteBtn,
            formStatus
        );
        return panel;
    }

    private VBox buildTablePanel() {
        VBox panel = new VBox(12);

        // Filter + Sort bar
        HBox filterBar = new HBox(12);
        filterBar.setAlignment(Pos.CENTER_LEFT);

        Label filterLabel = new Label("Filter:");
        filterLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 12px;");
        filterCombo = new ComboBox<>(FXCollections.observableArrayList(
            "All Rooms", "Available Only", "Occupied Only",
            "STANDARD", "DOUBLE", "DELUXE", "SUITE"
        ));
        filterCombo.setValue("All Rooms");
        filterCombo.getStyleClass().add("combo-box");
        filterCombo.setOnAction(e -> refreshTable());

        Label sortLabel = new Label("Sort by:");
        sortLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 12px;");
        sortCombo = new ComboBox<>(FXCollections.observableArrayList(
            "Room Number", "Price (Low-High)", "Price (High-Low)"
        ));
        sortCombo.setValue("Room Number");
        sortCombo.getStyleClass().add("combo-box");
        sortCombo.setOnAction(e -> refreshTable());

        statusLabel = new Label();
        statusLabel.setStyle("-fx-text-fill: #3498db; -fx-font-size: 12px;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        filterBar.getChildren().addAll(filterLabel, filterCombo, sortLabel, sortCombo, spacer, statusLabel);

        // TableView
        roomTable = new TableView<>();
        roomTable.getStyleClass().add("table-view");
        roomTable.setPlaceholder(new Label("No rooms found"));
        VBox.setVgrow(roomTable, Priority.ALWAYS);

        // Columns
        TableColumn<Room, Integer> colNum = new TableColumn<>("Room No");
        colNum.setCellValueFactory(new PropertyValueFactory<>("roomNumber"));
        colNum.setPrefWidth(90);

        TableColumn<Room, String> colType = new TableColumn<>("Type");
        colType.setCellValueFactory(data ->
            new javafx.beans.property.SimpleStringProperty(data.getValue().getRoomType().getDisplayName()));
        colType.setPrefWidth(130);

        TableColumn<Room, Double> colPrice = new TableColumn<>("Price/Night");
        colPrice.setCellValueFactory(new PropertyValueFactory<>("pricePerNight"));
        colPrice.setPrefWidth(110);
        colPrice.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double val, boolean empty) {
                super.updateItem(val, empty);
                setText(empty || val == null ? null : "₹" + String.format("%.0f", val));
            }
        });

        TableColumn<Room, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(data ->
            new javafx.beans.property.SimpleStringProperty(data.getValue().getAvailabilityStatus()));
        colStatus.setPrefWidth(110);
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setText(null); setStyle(""); return; }
                setText(s);
                setStyle("-fx-font-weight: bold; -fx-text-fill: " +
                    (s.equals("AVAILABLE") ? "#2ecc71" : "#e74c3c") + ";");
            }
        });

        TableColumn<Room, String> colGuest = new TableColumn<>("Guest");
        colGuest.setCellValueFactory(data ->
            new javafx.beans.property.SimpleStringProperty(
                data.getValue().getGuestName().isEmpty() ? "—" : data.getValue().getGuestName()));
        colGuest.setPrefWidth(140);

        TableColumn<Room, String> colDays = new TableColumn<>("Days");
        colDays.setCellValueFactory(data ->
            new javafx.beans.property.SimpleStringProperty(
                data.getValue().isBooked() ? String.valueOf(data.getValue().getDaysBooked()) : "—"));
        colDays.setPrefWidth(70);

        roomTable.getColumns().addAll(colNum, colType, colPrice, colStatus, colGuest, colDays);
        roomTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        panel.getChildren().addAll(filterBar, roomTable);
        return panel;
    }

    // ─── Event Handlers ───────────────────────────────────────────────────────

    private void handleAddRoom() {
        try {
            int num = Integer.parseInt(roomNumField.getText().trim());
            Room.RoomType type = typeCombo.getValue();
            double price = Double.parseDouble(priceField.getText().trim());

            if (type == null) throw new IllegalArgumentException("Select a room type");
            if (price <= 0) throw new IllegalArgumentException("Price must be positive");

            Room room = new Room(num, type);
            room.setPricePerNight(price);
            hotelService.addRoom(room);

            setFormStatus("✓ Room " + num + " added successfully!", true);
            roomNumField.clear();
            typeCombo.setValue(null);
            priceField.clear();
            refresh();

        } catch (NumberFormatException ex) {
            setFormStatus("✗ Invalid number format.", false);
        } catch (IllegalArgumentException ex) {
            setFormStatus("✗ " + ex.getMessage(), false);
        }
    }

    private void handleDeleteRoom() {
        Room selected = roomTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            setFormStatus("✗ Select a room to delete.", false);
            return;
        }
        if (selected.isBooked()) {
            setFormStatus("✗ Cannot delete an occupied room.", false);
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Delete Room " + selected.getRoomNumber() + "?",
            ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirm Delete");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                hotelService.deleteRoom(selected.getRoomNumber());
                setFormStatus("✓ Room deleted.", true);
                refresh();
            }
        });
    }

    private void setFormStatus(String msg, boolean success) {
        formStatus.setText(msg);
        formStatus.setStyle("-fx-font-size: 12px; -fx-text-fill: " +
            (success ? "#2ecc71" : "#e74c3c") + ";");
    }

    // ─── Refresh ──────────────────────────────────────────────────────────────

    public void refresh() {
        refreshTable();
    }

    private void refreshTable() {
        String filter = filterCombo != null ? filterCombo.getValue() : "All Rooms";
        String sort = sortCombo != null ? sortCombo.getValue() : "Room Number";

        java.util.List<Room> list;
        switch (filter) {
            case "Available Only": list = hotelService.getAvailableRooms(); break;
            case "Occupied Only": list = hotelService.getBookedRooms(); break;
            case "STANDARD": list = hotelService.getRoomsByType(Room.RoomType.STANDARD); break;
            case "DOUBLE": list = hotelService.getRoomsByType(Room.RoomType.DOUBLE); break;
            case "DELUXE": list = hotelService.getRoomsByType(Room.RoomType.DELUXE); break;
            case "SUITE": list = hotelService.getRoomsByType(Room.RoomType.SUITE); break;
            default: list = hotelService.getAllRooms();
        }

        if ("Price (Low-High)".equals(sort)) {
            list = new java.util.ArrayList<>(list);
            list.sort(java.util.Comparator.comparingDouble(Room::getPricePerNight));
        } else if ("Price (High-Low)".equals(sort)) {
            list = new java.util.ArrayList<>(list);
            list.sort(java.util.Comparator.comparingDouble(Room::getPricePerNight).reversed());
        } else {
            list = new java.util.ArrayList<>(list);
            list.sort(java.util.Comparator.comparingInt(Room::getRoomNumber));
        }

        ObservableList<Room> data = FXCollections.observableArrayList(list);
        if (roomTable != null) {
            roomTable.setItems(data);
            if (statusLabel != null)
                statusLabel.setText(list.size() + " rooms shown");
        }
    }

    public Node getView() { return view; }
}
