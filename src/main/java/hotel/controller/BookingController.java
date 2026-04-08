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
 * - Book rooms (prevent double-booking)
 * - Checkout rooms (generate bill)
 * - Shows active bookings in TableView
 * - Triggers room service threads on booking
 */
public class BookingController {

    private final HotelService hotelService;
    private final MainController mainController;
    private VBox view;

    // Book form fields
    private ComboBox<Integer> roomCombo;
    private TextField guestNameField;
    private TextField contactField;
    private DatePicker checkInPicker;
    private DatePicker checkOutPicker;
    private Label bookingStatus;
    private Label pricePreview;

    // Checkout
    private ComboBox<Integer> checkoutRoomCombo;
    private TextArea billOutput;

    // Table
    private TableView<Room> bookingTable;

    public BookingController(HotelService hotelService, MainController mainController) {
        this.hotelService = hotelService;
        this.mainController = mainController;
        buildView();
    }

    private void buildView() {
        view = new VBox(20);
        view.setPadding(new Insets(30));
        view.setStyle("-fx-background-color: #f4f4f6;");

        Label title = new Label("Booking & Checkout");
        title.setStyle("-fx-text-fill: #2c3e50; -fx-font-size: 22px; -fx-font-weight: bold;");

        // Two-column layout: book form | checkout form
        HBox formsRow = new HBox(20);
        formsRow.setMaxHeight(400);

        VBox bookPanel = buildBookingForm();
        bookPanel.setMinWidth(320);
        bookPanel.setMaxWidth(320);

        VBox checkoutPanel = buildCheckoutPanel();
        HBox.setHgrow(checkoutPanel, Priority.ALWAYS);

        formsRow.getChildren().addAll(bookPanel, checkoutPanel);

        // Active bookings table
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

        Label title = new Label("Book a Room");
        title.setStyle("-fx-text-fill: #3498db; -fx-font-size: 15px; -fx-font-weight: bold;");
        Region div = new Region(); div.getStyleClass().add("gold-divider");

        // Room selector
        Label lblRoom = new Label("Select Room");
        lblRoom.getStyleClass().add("field-label");
        roomCombo = new ComboBox<>();
        roomCombo.getStyleClass().add("combo-box");
        roomCombo.setMaxWidth(Double.MAX_VALUE);
        roomCombo.setOnAction(e -> updatePricePreview());

        // Guest Name
        Label lblGuest = new Label("Guest Name");
        lblGuest.getStyleClass().add("field-label");
        guestNameField = new TextField();
        guestNameField.setPromptText("Full name");
        guestNameField.getStyleClass().add("text-field");

        // Contact
        Label lblContact = new Label("Contact Number");
        lblContact.getStyleClass().add("field-label");
        contactField = new TextField();
        contactField.setPromptText("e.g. 9876543210");
        contactField.getStyleClass().add("text-field");

        // Dates (DatePicker)
        Label lblCheckIn = new Label("Check-in Date");
        lblCheckIn.getStyleClass().add("field-label");
        checkInPicker = new DatePicker(LocalDate.now());
        checkInPicker.getStyleClass().add("date-picker");
        checkInPicker.setMaxWidth(Double.MAX_VALUE);
        checkInPicker.setOnAction(e -> updatePricePreview());

        Label lblCheckOut = new Label("Check-out Date");
        lblCheckOut.getStyleClass().add("field-label");
        checkOutPicker = new DatePicker(LocalDate.now().plusDays(1));
        checkOutPicker.getStyleClass().add("date-picker");
        checkOutPicker.setMaxWidth(Double.MAX_VALUE);
        checkOutPicker.setOnAction(e -> updatePricePreview());

        // Price preview
        pricePreview = new Label("Select a room to see pricing");
        pricePreview.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 12px;");

        // Status
        bookingStatus = new Label();
        bookingStatus.setWrapText(true);
        bookingStatus.setStyle("-fx-font-size: 12px;");

        // Book Button
        Button bookBtn = new Button("✓ Confirm Booking");
        bookBtn.getStyleClass().add("btn-primary");
        bookBtn.setMaxWidth(Double.MAX_VALUE);
        bookBtn.setOnAction(e -> handleBookRoom());

        panel.getChildren().addAll(title, div,
            lblRoom, roomCombo,
            lblGuest, guestNameField,
            lblContact, contactField,
            lblCheckIn, checkInPicker,
            lblCheckOut, checkOutPicker,
            pricePreview, bookBtn, bookingStatus
        );
        return panel;
    }

    private VBox buildCheckoutPanel() {
        VBox panel = new VBox(12);
        panel.getStyleClass().add("panel-card");
        VBox.setVgrow(panel, Priority.ALWAYS);

        Label title = new Label("Checkout & Generate Bill");
        title.setStyle("-fx-text-fill: #3498db; -fx-font-size: 15px; -fx-font-weight: bold;");
        Region div = new Region(); div.getStyleClass().add("gold-divider");

        Label lblRoom = new Label("Select Occupied Room");
        lblRoom.getStyleClass().add("field-label");
        checkoutRoomCombo = new ComboBox<>();
        checkoutRoomCombo.getStyleClass().add("combo-box");
        checkoutRoomCombo.setMaxWidth(Double.MAX_VALUE);

        Button checkoutBtn = new Button("⬛ Checkout & Print Bill");
        checkoutBtn.getStyleClass().add("btn-success");
        checkoutBtn.setMaxWidth(Double.MAX_VALUE);
        checkoutBtn.setOnAction(e -> handleCheckout());

        billOutput = new TextArea();
        billOutput.getStyleClass().add("text-area");
        billOutput.setEditable(false);
        billOutput.setPromptText("Bill will appear here after checkout...");
        billOutput.setWrapText(false);
        VBox.setVgrow(billOutput, Priority.ALWAYS);

        panel.getChildren().addAll(title, div, lblRoom, checkoutRoomCombo, checkoutBtn, billOutput);
        return panel;
    }

    private TableView<Room> buildBookingTable() {
        TableView<Room> table = new TableView<>();
        table.getStyleClass().add("table-view");
        table.setPlaceholder(new Label("No active bookings"));
        table.setMaxHeight(200);

        TableColumn<Room, Integer> colNum = new TableColumn<>("Room");
        colNum.setCellValueFactory(new PropertyValueFactory<>("roomNumber"));
        colNum.setPrefWidth(80);

        TableColumn<Room, String> colType = new TableColumn<>("Type");
        colType.setCellValueFactory(d ->
            new javafx.beans.property.SimpleStringProperty(d.getValue().getRoomType().getDisplayName()));
        colType.setPrefWidth(130);

        TableColumn<Room, String> colGuest = new TableColumn<>("Guest Name");
        colGuest.setCellValueFactory(new PropertyValueFactory<>("guestName"));
        colGuest.setPrefWidth(160);

        TableColumn<Room, String> colContact = new TableColumn<>("Contact");
        colContact.setCellValueFactory(new PropertyValueFactory<>("guestContact"));
        colContact.setPrefWidth(130);

        TableColumn<Room, Integer> colDays = new TableColumn<>("Days");
        colDays.setCellValueFactory(new PropertyValueFactory<>("daysBooked"));
        colDays.setPrefWidth(70);

        TableColumn<Room, String> colTotal = new TableColumn<>("Est. Total");
        colTotal.setCellValueFactory(d -> {
            Room r = d.getValue();
            double est = r.getPricePerNight() * r.getDaysBooked() * 1.28; // with charges
            return new javafx.beans.property.SimpleStringProperty("₹" + String.format("%.0f", est));
        });
        colTotal.setPrefWidth(100);

        table.getColumns().addAll(colNum, colType, colGuest, colContact, colDays, colTotal);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        return table;
    }

    // ─── Event Handlers ───────────────────────────────────────────────────────

    private void handleBookRoom() {
        Integer roomNum = roomCombo.getValue();
        String name = guestNameField.getText().trim();
        String contact = contactField.getText().trim();
        
        LocalDate start = checkInPicker.getValue();
        LocalDate end = checkOutPicker.getValue();

        if (roomNum == null || name.isEmpty() || contact.isEmpty() || start == null || end == null) {
            setBookStatus("✗ Please fill all fields.", false);
            return;
        }

        if (start.isBefore(LocalDate.now())) {
            setBookStatus("✗ Check-in cannot be in the past.", false);
            return;
        }

        if (!end.isAfter(start)) {
            setBookStatus("✗ Check-out must be after check-in.", false);
            return;
        }

        long days = ChronoUnit.DAYS.between(start, end);
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        String checkInStr = start.format(dtf);
        String checkOutStr = end.format(dtf);

        boolean success = hotelService.bookRoom(roomNum, name, contact, (int)days, checkInStr, checkOutStr);
        if (success) {
            setBookStatus("✓ Room " + roomNum + " booked for " + name + "!", true);
            guestNameField.clear();
            contactField.clear();
            checkInPicker.setValue(LocalDate.now());
            checkOutPicker.setValue(LocalDate.now().plusDays(1));
            // Start room service threads in background
            new Thread(() -> RoomServiceThread.startRoomServices(roomNum), "ServiceThread-" + roomNum).start();
            refresh();
        } else {
            setBookStatus("✗ Room " + roomNum + " is already occupied or not found.", false);
        }
    }

    private void handleCheckout() {
        Integer roomNum = checkoutRoomCombo.getValue();
        if (roomNum == null) {
            billOutput.setText("Please select a room to checkout.");
            return;
        }
        Bill bill = hotelService.checkoutRoom(roomNum);
        if (bill != null) {
            billOutput.setText(bill.generateBillText());
            refresh();
        } else {
            billOutput.setText("Checkout failed — room not found or not booked.");
        }
    }

    private void updatePricePreview() {
        Integer roomNum = roomCombo.getValue();
        if (roomNum == null || checkInPicker == null || checkOutPicker == null) return;
        Room room = hotelService.getRoomByNumber(roomNum);
        if (room != null) {
            LocalDate start = checkInPicker.getValue();
            LocalDate end = checkOutPicker.getValue();
            
            if (start != null && end != null && end.isAfter(start)) {
                long days = ChronoUnit.DAYS.between(start, end);
                double base = room.getPricePerNight() * days;
                double total = base * 1.28;
                pricePreview.setText(String.format("₹%.0f/night × %d days ≈ ₹%.0f (incl. taxes)", 
                    room.getPricePerNight(), days, total));
                pricePreview.setStyle("-fx-text-fill: #3498db; -fx-font-size: 12px;");
            } else {
                pricePreview.setText("Please select valid dates");
                pricePreview.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 12px;");
            }
        }
    }

    private void setBookStatus(String msg, boolean ok) {
        bookingStatus.setText(msg);
        bookingStatus.setStyle("-fx-font-size: 12px; -fx-text-fill: " + (ok ? "#2ecc71" : "#e74c3c") + ";");
    }

    public void refresh() {
        // Populate available rooms in booking combo
        java.util.List<Integer> avail = new java.util.ArrayList<>();
        for (Room r : hotelService.getAvailableRooms()) avail.add(r.getRoomNumber());
        roomCombo.setItems(FXCollections.observableArrayList(avail));
        if (!avail.isEmpty()) roomCombo.setValue(avail.get(0));

        // Populate occupied rooms in checkout combo
        java.util.List<Integer> booked = new java.util.ArrayList<>();
        for (Room r : hotelService.getBookedRooms()) booked.add(r.getRoomNumber());
        checkoutRoomCombo.setItems(FXCollections.observableArrayList(booked));
        if (!booked.isEmpty()) checkoutRoomCombo.setValue(booked.get(0));

        // Refresh table
        bookingTable.setItems(FXCollections.observableArrayList(hotelService.getBookedRooms()));
        updatePricePreview();
    }

    public Node getView() { return view; }
}
