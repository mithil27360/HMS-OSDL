package hotel.controller;

import hotel.dao.AuthService;
import hotel.dao.IHotelService;
import hotel.dao.IAuthService;
import hotel.exception.RoomAlreadyBookedException;
import hotel.model.Bill;
import hotel.model.Room;
import hotel.model.Booking;
import hotel.model.User;
import hotel.util.ValidationUtils;
import javafx.util.StringConverter;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.beans.property.SimpleStringProperty;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.stream.Collectors;

public class BookingController {

    private final IHotelService hotelService; 
    private final IAuthService authService;
    private final User currentUser;

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
    private TableView<Booking> bookingTable;

    public BookingController(IHotelService hotelService, MainController mainController) {
        this.hotelService = hotelService;
        this.authService = AuthService.getInstance();
        this.currentUser = authService.getCurrentUser();
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

        if (currentUser != null && (currentUser.isAdmin() || currentUser.isReceptionist())) {
            VBox checkoutPanel = buildCheckoutPanel();
            HBox.setHgrow(checkoutPanel, Priority.ALWAYS);
            formsRow.getChildren().addAll(bookPanel, checkoutPanel);
        } else {
            // Guest mode: Give more space to the booking form or just show it alone
            HBox.setHgrow(bookPanel, Priority.ALWAYS);
            bookPanel.setMaxWidth(Double.MAX_VALUE);
            formsRow.getChildren().add(bookPanel);
        }

        String tableHeadline = (currentUser != null && currentUser.isGuest()) ? "My Reservations" : "Active Bookings";
        Label tableTitle = new Label(tableHeadline);
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

        roomCombo = new ComboBox<>();
        roomCombo.getStyleClass().add("combo-box");
        roomCombo.setMaxWidth(Double.MAX_VALUE);
        roomCombo.setConverter(getRoomStringConverter());
        roomCombo.setOnAction(e -> updatePricePreview());

        guestNameField = new TextField();
        guestNameField.setPromptText("Guest Full Name");
        guestNameField.getStyleClass().add("text-field");
        guestNameField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("[a-zA-Z ]*")) {
                guestNameField.setText(newVal.replaceAll("[^a-zA-Z ]", ""));
            }
        });

        contactField = new TextField();
        contactField.setPromptText("Contact (10 digits)");
        contactField.getStyleClass().add("text-field");
        contactField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*")) {
                contactField.setText(newVal.replaceAll("[^\\d]", ""));
            }
            if (contactField.getText().length() > 10) {
                contactField.setText(contactField.getText().substring(0, 10));
            }
        });

        checkInPicker = new DatePicker(LocalDate.now());
        checkInPicker.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                if (date.isBefore(LocalDate.now())) {
                    setDisable(true);
                    setStyle("-fx-background-color: #eeeeee; -fx-text-fill: #cccccc;");
                }
            }
        });
        checkInPicker.setMaxWidth(Double.MAX_VALUE);
        checkInPicker.setOnAction(e -> {
            LocalDate checkIn = checkInPicker.getValue();
            if (checkIn != null) {
                LocalDate checkOut = checkOutPicker.getValue();
                if (checkOut == null || !checkOut.isAfter(checkIn)) {
                    checkOutPicker.setValue(checkIn.plusDays(1));
                }
                updateCheckOutPickerCellFactory(checkIn);
            }
            updatePricePreview();
        });

        checkOutPicker = new DatePicker(LocalDate.now().plusDays(1));
        updateCheckOutPickerCellFactory(LocalDate.now());
        checkOutPicker.setMaxWidth(Double.MAX_VALUE);
        checkOutPicker.setOnAction(e -> {
            updatePricePreview();
            refresh();
        });

        pricePreview = new Label("Select a room to see pricing");
        pricePreview.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 11px;");

        bookingStatus = new Label();
        bookingStatus.setWrapText(true);
        bookingStatus.setStyle("-fx-font-size: 11px;");

        Button bookBtn = new Button("Confirm Booking");
        bookBtn.getStyleClass().add("btn-primary");
        bookBtn.setMaxWidth(Double.MAX_VALUE);
        bookBtn.setPrefHeight(40);
        bookBtn.setOnAction(e -> handleBookRoom());

        Label policyLabel = new Label("Check-out: 11:00 AM | Check-in: 3:00 PM");
        policyLabel.setStyle("-fx-text-fill: #95a5a6; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 5 0 0 0;");

        panel.getChildren().addAll(title, div, 
            new Label("Select Room"), roomCombo,
            new Label("Guest Name"), guestNameField,
            new Label("Contact Number"), contactField,
            new Label("Check-in"), checkInPicker,
            new Label("Check-out"), checkOutPicker,
            policyLabel, pricePreview, bookBtn, bookingStatus
        );
        return panel;
    }

    private VBox buildCheckoutPanel() {
        VBox panel = new VBox(12);
        panel.getStyleClass().add("panel-card");
        VBox.setVgrow(panel, Priority.ALWAYS);

        Label title = new Label("Checkout");
        title.setStyle("-fx-text-fill: #3498db; -fx-font-size: 15px; -fx-font-weight: bold;");
        Region div = new Region(); div.getStyleClass().add("gold-divider");

        checkoutRoomCombo = new ComboBox<>();
        checkoutRoomCombo.getStyleClass().add("combo-box");
        checkoutRoomCombo.setMaxWidth(Double.MAX_VALUE);
        checkoutRoomCombo.setConverter(getRoomStringConverter());

        Button checkoutBtn = new Button("Process Checkout");
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
        
        Label hint = new Label("Right-click a booking below to CANCEL it.");
        hint.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 10px; -fx-font-style: italic;");
        panel.getChildren().add(hint);
        
        return panel;
    }

    private TableView<Booking> buildBookingTable() {
        TableView<Booking> table = new TableView<>();
        table.getStyleClass().add("table-view");
        table.setPlaceholder(new Label("No active bookings found."));
        table.setMaxHeight(300);

        TableColumn<Booking, Integer> colNum = new TableColumn<>("Room");
        colNum.setCellValueFactory(new PropertyValueFactory<>("roomNumber"));
        colNum.setPrefWidth(80);

        TableColumn<Booking, String> colType = new TableColumn<>("Type");
        colType.setCellValueFactory(d -> {
            Room r = hotelService.getRoomByNumber(d.getValue().getRoomNumber());
            return new SimpleStringProperty(r != null ? r.getRoomType().getDisplayName() : "N/A");
        });
        colType.setPrefWidth(120);

        TableColumn<Booking, String> colGuest = new TableColumn<>("Guest Name");
        colGuest.setCellValueFactory(new PropertyValueFactory<>("guestName"));
        colGuest.setPrefWidth(150);

        TableColumn<Booking, String> colContact = new TableColumn<>("Contact");
        colContact.setCellValueFactory(new PropertyValueFactory<>("guestContact"));
        colContact.setPrefWidth(120);

        TableColumn<Booking, String> colDates = new TableColumn<>("Duration\n(In: 3PM | Out: 11AM)");
        colDates.setCellValueFactory(d -> new SimpleStringProperty(
            d.getValue().getCheckIn().toString() + " to " + d.getValue().getCheckOut().toString()));
        colDates.setPrefWidth(180);

        TableColumn<Booking, String> colTotal = new TableColumn<>("Est. Total");
        colTotal.setCellValueFactory(d -> {
            Booking b = d.getValue();
            Room r = hotelService.getRoomByNumber(b.getRoomNumber());
            if (r == null) return new SimpleStringProperty("-");
            long days = ChronoUnit.DAYS.between(b.getCheckIn(), b.getCheckOut());
            if (days <= 0) days = 1;
            double est = r.getPricePerNight() * days * 1.298; 
            return new SimpleStringProperty("₹" + String.format("%.0f", est));
        });
        colTotal.setPrefWidth(100);

        table.getColumns().addAll(colNum, colType, colGuest, colContact, colDates, colTotal);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Cancellation Context Menu
        ContextMenu menu = new ContextMenu();
        MenuItem cancelItem = new MenuItem("Cancel Reservation");
        cancelItem.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
        cancelItem.setOnAction(e -> {
            Booking selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                // Ensure guests only cancel their OWN bookings
                if (currentUser.isGuest() && !selected.getGuestName().equalsIgnoreCase(currentUser.getFullName())) {
                    setBookStatus("You can only cancel your own bookings.", false);
                    return;
                }

                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                confirm.setTitle("Cancel Booking");
                confirm.setHeaderText("Cancel reservation for Room " + selected.getRoomNumber() + "?");
                confirm.setContentText("This action cannot be undone.");
                
                if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                    if (hotelService.cancelBooking(selected.getBookingId())) {
                        refresh();
                    }
                }
            }
        });
        menu.getItems().add(cancelItem);
        table.setContextMenu(menu);

        return table;
    }

    private void handleBookRoom() {
        // Step 1: Check roomCombo.getValue() != null
        Integer roomNum = roomCombo.getValue();
        if (roomNum == null) {
            setBookStatus("Please select a room", false);
            return;
        }

        // Step 2: Check guestNameField not empty
        String name = guestNameField.getText().trim();
        if (name.isEmpty()) {
            setBookStatus("Guest name is required", false);
            return;
        }

        // Step 3: Validate name via Utility
        if (!ValidationUtils.isValidName(name)) {
            setBookStatus("Name must be 2-50 letters only", false);
            return;
        }

        // Step 4: Check contact not empty
        String contact = contactField.getText().trim();
        if (contact.isEmpty()) {
            setBookStatus("Contact number is required", false);
            return;
        }

        // Step 5: Validate contact via Utility
        if (!ValidationUtils.isValidPhone(contact)) {
            setBookStatus("Contact must be exactly 10 digits", false);
            return;
        }

        // Step 6: Check checkInPicker.getValue() != null
        LocalDate checkIn = checkInPicker.getValue();
        if (checkIn == null) {
            setBookStatus("Select a check-in date", false);
            return;
        }

        // Step 7: Check checkOutPicker.getValue() != null
        LocalDate checkOut = checkOutPicker.getValue();
        if (checkOut == null) {
            setBookStatus("Select a check-out date", false);
            return;
        }

        // Step 8: Check checkIn is not before LocalDate.now()
        if (checkIn.isBefore(LocalDate.now())) {
            setBookStatus("Check-in cannot be in the past", false);
            return;
        }

        // Step 9: Check checkOut.isAfter(checkIn)
        if (!checkOut.isAfter(checkIn)) {
            setBookStatus("Check-out must be after check-in", false);
            return;
        }

        // Step 10: Calculate days = ChronoUnit.DAYS.between(checkIn, checkOut). Check days >= 1
        long days = ChronoUnit.DAYS.between(checkIn, checkOut);
        if (days < 1) {
            setBookStatus("Minimum stay is 1 night", false);
            return;
        }

        // Step 11: Check days <= 365
        if (days > 365) {
            setBookStatus("Booking cannot exceed 365 nights", false);
            return;
        }

        // Step 12: Check hotelService.isRoomAvailableForDates(roomNum, checkIn, checkOut)
        if (!hotelService.isRoomAvailableForDates(roomNum, checkIn, checkOut)) {
            setBookStatus("Room " + roomNum + " is currently occupied. Please select another room.", false);
            return;
        }

        // Step 13: Call hotelService.bookRoom(...)
        try {
            hotelService.bookRoom(roomNum, name, contact, (int)days, checkIn, checkOut);
            
            // Show success popup with details
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Booking Confirmed");
            alert.setHeaderText("Room " + roomNum + " successfully reserved!");
            alert.setContentText(String.format(
                "Guest: %s\nCheck-in: %s\nCheck-out: %s\nDuration: %d night(s)\n\nRoom is now blocked for these dates.",
                name, checkIn, checkOut, days
            ));
            alert.showAndWait();

            setBookStatus("Room " + roomNum + " successfully reserved.", true);
            clearFields();
            refresh();
            
        } catch (RoomAlreadyBookedException e) {
            setBookStatus(e.getMessage(), false);
        } catch (IllegalArgumentException e) {
            setBookStatus(e.getMessage(), false);
        } catch (Exception e) {
            setBookStatus("Booking failed. Please try again.", false);
        }
    }

    private void updateCheckOutPickerCellFactory(LocalDate checkIn) {
        checkOutPicker.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                LocalDate minDate = checkIn != null ? checkIn.plusDays(1) : LocalDate.now().plusDays(1);
                if (date.isBefore(minDate)) {
                    setDisable(true);
                    setStyle("-fx-background-color: #eeeeee; -fx-text-fill: #cccccc;");
                }
            }
        });
    }

    private void clearFields() {
        guestNameField.clear();
        contactField.clear();
        checkInPicker.setValue(LocalDate.now());
        checkOutPicker.setValue(LocalDate.now().plusDays(1));
    }

    private void handleCheckout() {
        // Step 1: Check checkoutRoomCombo.getValue() != null
        Integer roomNum = checkoutRoomCombo.getValue();
        if (roomNum == null) {
            billOutput.setText("Select an occupied room to checkout");
            return;
        }

        // Step 2: Get room = hotelService.getRoomByNumber(roomNum). Check room != null
        Room room = hotelService.getRoomByNumber(roomNum);
        if (room == null) {
            billOutput.setText("Selected room no longer exists");
            return;
        }

        // Step 3: Call hotelService.checkoutRoom(roomNum)
        Bill bill = hotelService.checkoutRoom(roomNum);
        if (bill == null) {
            billOutput.setText("Checkout failed. Try again.");
            return;
        }

        // Step 4: Call refresh() after successful checkout
        billOutput.setText(bill.generateBillText());
        refresh();
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
                double total = room.getPricePerNight() * days * 1.298;
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
        LocalDate start = (checkInPicker != null && checkInPicker.getValue() != null) ? checkInPicker.getValue() : LocalDate.now();
        LocalDate end = (checkOutPicker != null && checkOutPicker.getValue() != null) ? checkOutPicker.getValue() : LocalDate.now().plusDays(1);
        
        if (!end.isAfter(start)) end = start.plusDays(1);

        java.util.List<Integer> avail = hotelService.getAvailableRooms(start, end).stream()
                .map(Room::getRoomNumber)
                .sorted()
                .collect(Collectors.toList());
        roomCombo.setItems(FXCollections.observableArrayList(avail));
        
        if (currentUser != null && (currentUser.isAdmin() || currentUser.isReceptionist())) {
            java.util.List<Integer> bookedToday = hotelService.getBookedRooms(LocalDate.now()).stream()
                    .map(Room::getRoomNumber)
                    .sorted()
                    .collect(Collectors.toList());
            checkoutRoomCombo.setItems(FXCollections.observableArrayList(bookedToday));
        }

        java.util.List<Booking> active = hotelService.getAllBookings().stream()
                .filter(b -> !b.isCheckedOut())
                .filter(b -> {
                    if (currentUser == null) return false;
                    if (currentUser.isGuest()) {
                        // Guest only sees their own
                        return b.getGuestName().equalsIgnoreCase(currentUser.getFullName());
                    }
                    return true; // Staff see all
                })
                .collect(Collectors.toList());
        bookingTable.setItems(FXCollections.observableArrayList(active));
        
        bookingStatus.setText("");
        updatePricePreview();
    }


    private StringConverter<Integer> getRoomStringConverter() {
        return new StringConverter<>() {
            @Override public String toString(Integer roomNum) {
                if (roomNum == null) return "";
                Room r = hotelService.getRoomByNumber(roomNum);
                return roomNum + (r != null ? " - " + r.getRoomType().getDisplayName() : "");
            }
            @Override public Integer fromString(String string) { return null; }
        };
    }

    public Node getView() { return view; }
}
