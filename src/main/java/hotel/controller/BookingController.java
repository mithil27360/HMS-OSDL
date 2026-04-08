package hotel.controller;

import hotel.dao.HotelService;
import hotel.model.Bill;
import hotel.model.Room;
import hotel.util.RoomServiceThread;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * Booking Controller
 * Manages room reservations, checkouts, and data validation.
 */
public class BookingController {

    private final HotelService hotelService;
    private final MainController mainController;
    private VBox view;

    private ComboBox<Integer> roomCombo;
    private TextField guestNameField;
    private TextField contactField;
    private DatePicker checkInPicker;
    private DatePicker checkOutPicker;
    private Label bookingStatus;
    private Label pricePreview;

    private ComboBox<Integer> checkoutRoomCombo;
    private TextArea billOutput;
    private TableView<Room> bookingTable;

    public BookingController(HotelService hotelService, MainController mainController) {
        this.hotelService = hotelService;
        this.mainController = mainController;
        buildView();
    }

    private void buildView() {
        view = new VBox(24);
        view.setPadding(new Insets(30));
        view.setStyle("-fx-background-color: #f4f4f6;");

        Label title = new Label("Booking & Checkout");
        title.getStyleClass().add("section-title");
        title.setStyle("-fx-text-fill: #2c3e50; -fx-font-size: 24px; -fx-font-weight: bold;");

        HBox formsRow = new HBox(20);
        formsRow.setMaxHeight(450);

        VBox bookPanel = buildBookingForm();
        bookPanel.setMinWidth(320);
        bookPanel.setMaxWidth(320);

        VBox checkoutPanel = buildCheckoutPanel();
        HBox.setHgrow(checkoutPanel, Priority.ALWAYS);

        formsRow.getChildren().addAll(bookPanel, checkoutPanel);

        Label tableTitle = new Label("Active Bookings");
        tableTitle.setStyle("-fx-text-fill: #2c3e50; -fx-font-size: 16px; -fx-font-weight: bold;");
        
        bookingTable = buildBookingTable();
        VBox.setVgrow(bookingTable, Priority.ALWAYS);

        view.getChildren().addAll(title, formsRow, tableTitle, bookingTable);
        refresh();
    }

    private VBox buildBookingForm() {
        VBox panel = new VBox(12);
        panel.getStyleClass().add("panel-card");

        Label title = new Label("New Reservation");
        title.setStyle("-fx-text-fill: #3498db; -fx-font-size: 15px; -fx-font-weight: bold;");
        Region div = new Region(); div.getStyleClass().add("gold-divider");

        roomCombo = new ComboBox<>();
        roomCombo.getStyleClass().add("combo-box");
        roomCombo.setMaxWidth(Double.MAX_VALUE);
        roomCombo.setOnAction(e -> updatePricePreview());

        guestNameField = new TextField();
        guestNameField.setPromptText("Guest Full Name");
        guestNameField.getStyleClass().add("text-field");

        contactField = new TextField();
        contactField.setPromptText("Contact (10 digits)");
        contactField.getStyleClass().add("text-field");

        checkInPicker = new DatePicker(LocalDate.now());
        checkInPicker.setMaxWidth(Double.MAX_VALUE);
        checkInPicker.setOnAction(e -> updatePricePreview());

        checkOutPicker = new DatePicker(LocalDate.now().plusDays(1));
        checkOutPicker.setMaxWidth(Double.MAX_VALUE);
        checkOutPicker.setOnAction(e -> updatePricePreview());

        pricePreview = new Label("Select a room to see pricing");
        pricePreview.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 11px;");

        bookingStatus = new Label();
        bookingStatus.setWrapText(true);
        bookingStatus.setStyle("-fx-font-size: 11px;");

        Button bookBtn = new Button("✓ Complete Booking");
        bookBtn.getStyleClass().add("btn-primary");
        bookBtn.setMaxWidth(Double.MAX_VALUE);
        bookBtn.setPrefHeight(40);
        bookBtn.setOnAction(e -> handleBookRoom());

        panel.getChildren().addAll(title, div, 
            new Label("Select Room"), roomCombo,
            new Label("Guest Name"), guestNameField,
            new Label("Contact Number"), contactField,
            new Label("Check-in"), checkInPicker,
            new Label("Check-out"), checkOutPicker,
            pricePreview, bookBtn, bookingStatus
        );
        return panel;
    }

    private VBox buildCheckoutPanel() {
        VBox panel = new VBox(12);
        panel.getStyleClass().add("panel-card");
        VBox.setVgrow(panel, Priority.ALWAYS);

        Label title = new Label("Checkout & Settlement");
        title.setStyle("-fx-text-fill: #3498db; -fx-font-size: 15px; -fx-font-weight: bold;");
        Region div = new Region(); div.getStyleClass().add("gold-divider");

        checkoutRoomCombo = new ComboBox<>();
        checkoutRoomCombo.getStyleClass().add("combo-box");
        checkoutRoomCombo.setMaxWidth(Double.MAX_VALUE);

        Button checkoutBtn = new Button("⬛ Process Checkout");
        checkoutBtn.getStyleClass().add("btn-success");
        checkoutBtn.setMaxWidth(Double.MAX_VALUE);
        checkoutBtn.setPrefHeight(40);
        checkoutBtn.setOnAction(e -> handleCheckout());

        billOutput = new TextArea();
        billOutput.getStyleClass().add("text-area");
        billOutput.setEditable(false);
        billOutput.setPromptText("Final invoice will be generated here...");
        billOutput.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 12px;");
        VBox.setVgrow(billOutput, Priority.ALWAYS);

        panel.getChildren().addAll(title, div, new Label("Occupied Room"), checkoutRoomCombo, checkoutBtn, billOutput);
        return panel;
    }

    private TableView<Room> buildBookingTable() {
        TableView<Room> table = new TableView<>();
        table.getStyleClass().add("table-view");
        table.setPlaceholder(new Label("No active bookings found."));
        table.setMaxHeight(300);

        TableColumn<Room, Integer> colNum = new TableColumn<>("Room");
        colNum.setCellValueFactory(new PropertyValueFactory<>("roomNumber"));
        colNum.setPrefWidth(80);

        TableColumn<Room, String> colType = new TableColumn<>("Type");
        colType.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getRoomType().getDisplayName()));
        colType.setPrefWidth(120);

        TableColumn<Room, String> colGuest = new TableColumn<>("Guest Name");
        colGuest.setCellValueFactory(new PropertyValueFactory<>("guestName"));
        colGuest.setPrefWidth(150);

        TableColumn<Room, String> colContact = new TableColumn<>("Contact");
        colContact.setCellValueFactory(new PropertyValueFactory<>("guestContact"));
        colContact.setPrefWidth(120);

        TableColumn<Room, String> colDates = new TableColumn<>("Duration");
        colDates.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
            d.getValue().getCheckInDate() + " to " + d.getValue().getCheckOutDate()));
        colDates.setPrefWidth(180);

        TableColumn<Room, String> colTotal = new TableColumn<>("Est. Total");
        colTotal.setCellValueFactory(d -> {
            Room r = d.getValue();
            double est = r.getPricePerNight() * r.getDaysBooked() * 1.298; // Fix: Use correct billing multiplier
            return new javafx.beans.property.SimpleStringProperty("₹" + String.format("%.0f", est));
        });
        colTotal.setPrefWidth(100);

        table.getColumns().addAll(colNum, colType, colGuest, colContact, colDates, colTotal);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        return table;
    }

    private void handleBookRoom() {
        Integer roomNum = roomCombo.getValue();
        String name = guestNameField.getText().trim();
        String contact = contactField.getText().trim();
        LocalDate start = checkInPicker.getValue();
        LocalDate end = checkOutPicker.getValue();

        // ─── VALIDATION ───────────────────────────────────────────────────────
        if (roomNum == null || name.isEmpty() || contact.isEmpty() || start == null || end == null) {
            setBookStatus("✗ Missing required information.", false);
            return;
        }

        if (name.length() < 2 || !name.matches("^[a-zA-Z\\s]+$")) {
            setBookStatus("✗ Invalid Name. Use letters (min 2).", false);
            return;
        }

        if (!contact.matches("^\\d{10}$")) {
            setBookStatus("✗ Invalid Contact. Use 10 digits.", false);
            return;
        }

        if (start.isBefore(LocalDate.now())) {
            setBookStatus("✗ Check-in cannot be in the past.", false);
            return;
        }

        if (!end.isAfter(start)) {
            setBookStatus("✗ Checkout must be after Check-in.", false);
            return;
        }

        long days = ChronoUnit.DAYS.between(start, end);
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        
        boolean success = hotelService.bookRoom(roomNum, name, contact, (int)days, start.format(dtf), end.format(dtf));
        if (success) {
            setBookStatus("✓ Room " + roomNum + " successfully reserved.", true);
            clearFields();
            new Thread(() -> RoomServiceThread.startRoomServices(roomNum)).start();
            refresh();
        } else {
            setBookStatus("✗ Room " + roomNum + " is currently occupied.", false);
        }
    }

    private void clearFields() {
        guestNameField.clear();
        contactField.clear();
        checkInPicker.setValue(LocalDate.now());
        checkOutPicker.setValue(LocalDate.now().plusDays(1));
    }

    private void handleCheckout() {
        Integer roomNum = checkoutRoomCombo.getValue();
        if (roomNum == null) return;
        
        Bill bill = hotelService.checkoutRoom(roomNum);
        if (bill != null) {
            billOutput.setText(bill.generateBillText());
            refresh();
        }
    }

    private void updatePricePreview() {
        Integer roomNum = roomCombo.getValue();
        if (roomNum == null || checkInPicker.getValue() == null || checkOutPicker.getValue() == null) return;
        
        Room room = hotelService.getRoomByNumber(roomNum);
        if (room != null) {
            LocalDate start = checkInPicker.getValue();
            LocalDate end = checkOutPicker.getValue();
            
            if (end.isAfter(start)) {
                long days = ChronoUnit.DAYS.between(start, end);
                double total = room.getPricePerNight() * days * 1.298; // Correct 1.298x multiplier
                pricePreview.setText(String.format("₹%.0f/night × %d nights ≈ ₹%.0f (incl. taxes)", 
                    room.getPricePerNight(), days, total));
                pricePreview.setStyle("-fx-text-fill: #3498db; -fx-font-size: 11px;");
            } else {
                pricePreview.setText("Check-out must be after check-in");
                pricePreview.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 11px;");
            }
        }
    }

    private void setBookStatus(String msg, boolean ok) {
        bookingStatus.setText(msg);
        bookingStatus.setStyle("-fx-font-size: 12px; -fx-text-fill: " + (ok ? "#2ecc71" : "#e74c3c") + ";");
    }

    public void refresh() {
        java.util.List<Integer> avail = new java.util.ArrayList<>();
        for (Room r : hotelService.getAvailableRooms()) avail.add(r.getRoomNumber());
        roomCombo.setItems(FXCollections.observableArrayList(avail));
        
        java.util.List<Integer> booked = new java.util.ArrayList<>();
        for (Room r : hotelService.getBookedRooms()) booked.add(r.getRoomNumber());
        checkoutRoomCombo.setItems(FXCollections.observableArrayList(booked));

        bookingTable.setItems(FXCollections.observableArrayList(hotelService.getBookedRooms()));
        updatePricePreview();
    }

    public Node getView() { return view; }
}
