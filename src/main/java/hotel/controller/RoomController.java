package hotel.controller;

import hotel.dao.IHotelService;
import hotel.model.Room;
import javafx.beans.property.SimpleStringProperty;
import java.util.stream.Collectors;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;

/**
 * Controller for Room Inventory management.
 */
public class RoomController {


    private final IHotelService hotelService; 
    private VBox view;
    private TableView<Room> roomTable;

    private TextField roomNumField;
    private ComboBox<Room.RoomType> typeCombo;
    private Label formStatus;

    public RoomController(IHotelService hotelService) {
        this.hotelService = hotelService;
        buildView();
    }

    private void buildView() {
        view = new VBox(20);
        view.setPadding(new Insets(30));
        view.setStyle("-fx-background-color: #f4f4f6;");

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("Room Inventory Management");
        title.setStyle("-fx-text-fill: #2c3e50; -fx-font-size: 22px; -fx-font-weight: bold;");
        header.getChildren().add(title);


        HBox mainRow = new HBox(20);
        VBox.setVgrow(mainRow, Priority.ALWAYS);

        VBox formPanel = buildAddRoomForm();
        formPanel.setMinWidth(280);
        formPanel.setMaxWidth(280);

        VBox tablePanel = buildTablePanel();
        HBox.setHgrow(tablePanel, Priority.ALWAYS);

        mainRow.getChildren().addAll(formPanel, tablePanel);

        view.getChildren().addAll(header, mainRow);
        refresh();
    }

    private VBox buildAddRoomForm() {
        VBox panel = new VBox(14);
        panel.getStyleClass().add("panel-card");

        Label title = new Label("Register New Room");
        title.setStyle("-fx-text-fill: #3498db; -fx-font-size: 15px; -fx-font-weight: bold;");
        Region div = new Region(); div.getStyleClass().add("gold-divider");

        roomNumField = new TextField();
        roomNumField.setPromptText("Room # (e.g. 101)");
        roomNumField.getStyleClass().add("text-field");
        roomNumField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*"))
                roomNumField.setText(newVal.replaceAll("[^\\d]", ""));
        });


        typeCombo = new ComboBox<>(FXCollections.observableArrayList(Room.RoomType.values()));
        typeCombo.setPromptText("Select Category");
        typeCombo.getStyleClass().add("combo-box");
        typeCombo.setMaxWidth(Double.MAX_VALUE);
        typeCombo.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Room.RoomType r, boolean empty) {
                super.updateItem(r, empty);
                setText(empty || r == null ? null : r.getDisplayName() + " (₹" + r.getBasePrice() + ")");
            }
        });

        formStatus = new Label();
        formStatus.setWrapText(true);
        formStatus.setStyle("-fx-font-size: 11px;");

        Button addBtn = new Button("+ Add to Inventory");
        addBtn.getStyleClass().add("btn-primary");
        addBtn.setMaxWidth(Double.MAX_VALUE);
        addBtn.setPrefHeight(40);
        addBtn.setOnAction(e -> handleAddRoom());

        Button delBtn = new Button("Delete Selected");
        delBtn.getStyleClass().add("btn-danger");
        delBtn.setMaxWidth(Double.MAX_VALUE);
        delBtn.setPrefHeight(35);
        delBtn.setOnAction(e -> handleDeleteRoom());

        panel.getChildren().addAll(
            title, div,
            new Label("Room Number"), roomNumField,
            new Label("Room Category"), typeCombo,
            addBtn, delBtn, formStatus
        );
        return panel;
    }

    private VBox buildTablePanel() {
        VBox panel = new VBox(10);

        roomTable = new TableView<>();
        roomTable.getStyleClass().add("table-view");
        roomTable.setPlaceholder(new Label("No rooms registered."));
        VBox.setVgrow(roomTable, Priority.ALWAYS);

        TableColumn<Room, Integer> colNum = new TableColumn<>("Room #");
        colNum.setCellValueFactory(new PropertyValueFactory<>("roomNumber"));
        colNum.setPrefWidth(90);

        TableColumn<Room, String> colType = new TableColumn<>("Category");
        colType.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getRoomType().getDisplayName()));
        colType.setPrefWidth(140);

        TableColumn<Room, Double> colPrice = new TableColumn<>("Price (₹)");
        colPrice.setCellValueFactory(new PropertyValueFactory<>("pricePerNight"));
        colPrice.setPrefWidth(110);
        colPrice.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); return; }
                setText("₹" + String.format("%.0f", v));
                setStyle("-fx-text-fill: #3498db; -fx-font-weight: bold;");
            }
        });

        TableColumn<Room, String> colStatus = new TableColumn<>("Status Today");
        colStatus.setCellValueFactory(d -> {
            int num = d.getValue().getRoomNumber();
            if (hotelService.isRoomCleaning(num)) return new SimpleStringProperty("CLEANING");
            
            boolean avail = hotelService.isRoomAvailableForDates(num, 
                java.time.LocalDate.now(), java.time.LocalDate.now().plusDays(1));
            return new SimpleStringProperty(avail ? "AVAILABLE" : "OCCUPIED");
        });
        colStatus.setPrefWidth(110);
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setText(null); return; }
                setText(s);
                String color = "#2ecc71"; // Green
                if (s.equals("OCCUPIED")) color = "#e74c3c"; // Red
                if (s.equals("CLEANING")) color = "#f39c12"; // Orange
                setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold;");
            }
        });

        TableColumn<Room, String> colGuest = new TableColumn<>("Guest Tonight");
        colGuest.setCellValueFactory(d -> {
            Room r = d.getValue();
            java.util.List<hotel.model.Booking> active = hotelService.getAllBookings().stream()
                .filter(b -> b.getRoomNumber() == r.getRoomNumber() && !b.isCheckedOut())
                .filter(b -> !java.time.LocalDate.now().isBefore(b.getCheckIn()) && java.time.LocalDate.now().isBefore(b.getCheckOut()))
                .collect(Collectors.toList());
            return new SimpleStringProperty(active.isEmpty() ? "-" : active.get(0).getGuestName());
        });
        colGuest.setPrefWidth(140);

        roomTable.getColumns().setAll(colNum, colType, colPrice, colStatus, colGuest);

        roomTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        panel.getChildren().addAll(roomTable);
        return panel;
    }

    private void handleAddRoom() {
        String numStr = roomNumField.getText().trim();
        Room.RoomType type = typeCombo.getValue();

        if (numStr.isEmpty() || type == null) {
            setStatus("Enter number and category.", false); return;
        }


        try {
            int num = Integer.parseInt(numStr);
            // Validation in the model setter

            Room room = new Room(num, type);
            
            if (hotelService.getRoomByNumber(num) != null) {
                setStatus("Room " + num + " already exists.", false);
                return;
            }


            hotelService.addRoom(room);
            setStatus("Room " + num + " registered.", true);
            roomNumField.clear(); typeCombo.setValue(null);
            refresh();
        } catch (NumberFormatException e) {
            setStatus("Room number must be numeric.", false);
        } catch (IllegalArgumentException e) {
            // Catching 3-digit validation from Room.java
            setStatus("Validation Error: " + e.getMessage(), false);
        } catch (Exception e) {
            setStatus("Error: " + e.getMessage(), false);
        }

    }

    private void handleDeleteRoom() {
        Room selected = roomTable.getSelectionModel().getSelectedItem();
        if (selected == null) { setStatus("Select a room to remove.", false); return; }

        if (hotelService.deleteRoom(selected.getRoomNumber())) {
            setStatus("Room removed.", true);
            refresh();
        } else {
            setStatus("Cannot delete room with active reservations.", false);
        }
    }


    private void setStatus(String msg, boolean ok) {
        formStatus.setText(msg);
        formStatus.setStyle("-fx-font-size: 11px; -fx-text-fill: " + (ok ? "#2ecc71" : "#e74c3c") + ";");
    }

    public void refresh() {
        // Sort list via Comparator

        java.util.List<Room> rooms = hotelService.getAllRooms();
        rooms.sort(java.util.Comparator.comparingInt(Room::getRoomNumber));
        roomTable.setItems(FXCollections.observableArrayList(rooms));
        // CRITICAL FIX: Refresh table to re-evaluate cell factories (status column)
        roomTable.refresh();
    }

    public Node getView() { return view; }
}
