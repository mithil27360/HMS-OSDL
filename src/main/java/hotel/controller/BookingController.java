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
    private TableView<Booking> bookingTable;
    private ComboBox<Integer> roomCombo;
    private ComboBox<Integer> checkinRoomCombo;
    private ComboBox<Integer> checkoutRoomCombo;
    private TextField guestNameField;
    private TextField contactField;
    private DatePicker checkInPicker;
    private DatePicker checkOutPicker;
    private Label bookingStatus;
    private Label pricePreview;
    private TextArea billOutput;

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
            VBox opsPanel = buildOperationsPanel();
            HBox.setHgrow(opsPanel, Priority.ALWAYS);
            formsRow.getChildren().addAll(bookPanel, opsPanel);
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

    private VBox buildOperationsPanel() {
        VBox panel = new VBox(15);
        panel.getStyleClass().add("panel-card");
        VBox.setVgrow(panel, Priority.ALWAYS);

        Label title = new Label("Check-In & Checkout");
        title.setStyle("-fx-text-fill: #3498db; -fx-font-size: 16px; -fx-font-weight: bold;");
        Region div = new Region(); div.getStyleClass().add("gold-divider");

        // Check-In Section
        checkinRoomCombo = new ComboBox<>();
        checkinRoomCombo.setMaxWidth(Double.MAX_VALUE);
        checkinRoomCombo.setConverter(getRoomStringConverter());
        checkinRoomCombo.setPromptText("Select reservation...");

        Button checkinBtn = new Button("Confirm Check-In");
        checkinBtn.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-weight: bold;");
        checkinBtn.setMaxWidth(Double.MAX_VALUE);
        checkinBtn.setPrefHeight(35);
        checkinBtn.setOnAction(e -> handleCheckIn());

        // Checkout Section
        checkoutRoomCombo = new ComboBox<>();
        checkoutRoomCombo.setMaxWidth(Double.MAX_VALUE);
        checkoutRoomCombo.setConverter(getRoomStringConverter());
        checkoutRoomCombo.setPromptText("Select stay...");

        Button checkoutBtn = new Button("Process Checkout");
        checkoutBtn.setStyle("-fx-background-color: #e67e22; -fx-text-fill: white; -fx-font-weight: bold;");
        checkoutBtn.setMaxWidth(Double.MAX_VALUE);
        checkoutBtn.setPrefHeight(35);
        checkoutBtn.setOnAction(e -> handleCheckout());

        billOutput = new TextArea();
        billOutput.setEditable(false);
        billOutput.setPrefRowCount(8);
        billOutput.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 11px;");

        panel.getChildren().addAll(title, div, 
            new Label("Guest Arrival (Check-In)"), checkinRoomCombo, checkinBtn, 
            new Separator(),
            new Label("Guest Departure (Checkout)"), checkoutRoomCombo, checkoutBtn, 
            billOutput);
        
        return panel;
    }

    private void handleCheckIn() {
        Integer roomNum = checkinRoomCombo.getValue();
        if (roomNum == null) {
            setBookStatus("Select a reservation to check in", false);
            return;
        }
        try {
            hotelService.checkInRoom(roomNum);
            refresh();
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setHeaderText("Success");
            alert.setContentText("Room " + roomNum + " is now Checked In. Bill added to Revenue.");
            alert.showAndWait();
        } catch (Exception e) {
            setBookStatus(e.getMessage(), false);
        }
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

        TableColumn<Booking, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(d -> {
            Booking b = d.getValue();
            if (b.isCheckedOut()) return new SimpleStringProperty("Checked Out");
            if (b.isCheckedIn()) return new SimpleStringProperty("Checked In");
            return new SimpleStringProperty("Reserved");
        });
        colStatus.setPrefWidth(100);
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                if (item.equals("Checked In")) setStyle("-fx-text-fill: #2ecc71; -fx-font-weight: bold;");
                else if (item.equals("Reserved")) setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold;");
                else setStyle("-fx-text-fill: #95a5a6;");
            }
        });

        table.getColumns().addAll(colNum, colType, colGuest, colContact, colDates, colTotal, colStatus);
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

                if (selected.isCheckedIn()) {
                    setBookStatus("Cannot cancel a booking that has already checked in.", false);
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

        MenuItem checkInItem = new MenuItem("Check-In Guest");
        checkInItem.setStyle("-fx-text-fill: #2ecc71; -fx-font-weight: bold;");
        checkInItem.setOnAction(e -> {
            Booking selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                if (selected.isCheckedIn()) {
                    setBookStatus("Guest is already checked in.", false);
                    return;
                }
                try {
                    hotelService.checkInRoom(selected.getRoomNumber());
                    refresh();
                    Alert success = new Alert(Alert.AlertType.INFORMATION);
                    success.setTitle("Check-In Successful");
                    success.setHeaderText("Guest Checked In & Bill Processed");
                    success.setContentText("Room " + selected.getRoomNumber() + " is now active. Bill added to Revenue.");
                    success.showAndWait();
                } catch (Exception ex) {
                    setBookStatus(ex.getMessage(), false);
                }
            }
        });

        if (currentUser.isAdmin() || currentUser.isReceptionist()) {
            menu.getItems().addAll(checkInItem, cancelItem);
        } else {
            menu.getItems().add(cancelItem);
        }
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
        Integer roomNum = checkoutRoomCombo.getValue();
        if (roomNum == null) {
            billOutput.setText("Select an occupied room to checkout");
            return;
        }

        Room room = hotelService.getRoomByNumber(roomNum);
        if (room == null) {
            billOutput.setText("Selected room no longer exists");
            return;
        }

        // Checkout effectively releases the room and moves it to cleaning
        Bill bill = hotelService.checkoutRoom(roomNum);
        if (bill != null) {
            billOutput.setText(bill.generateBillText());
            setBookStatus("Checkout complete. Room " + roomNum + " released and scheduled for cleaning.", true);
            refresh();
        } else {
            billOutput.setText("Checkout failed. Ensure room was checked in first.");
            setBookStatus("Checkout failed or room not occupied.", false);
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
            java.util.List<Booking> todaysBookings = hotelService.getAllBookings().stream()
                .filter(b -> !b.isCheckedOut())
                .filter(b -> b.getCheckIn().isEqual(LocalDate.now()) || b.getCheckIn().isBefore(LocalDate.now()))
                .collect(Collectors.toList());

            java.util.List<Integer> needsCheckin = todaysBookings.stream()
                .filter(b -> !b.isCheckedIn())
                .map(Booking::getRoomNumber)
                .sorted()
                .collect(Collectors.toList());
            
            java.util.List<Integer> needsCheckout = todaysBookings.stream()
                .filter(b -> b.isCheckedIn())
                .map(Booking::getRoomNumber)
                .sorted()
                .collect(Collectors.toList());

            checkinRoomCombo.setItems(FXCollections.observableArrayList(needsCheckin));
            checkoutRoomCombo.setItems(FXCollections.observableArrayList(needsCheckout));
        }

        java.util.List<Booking> active = hotelService.getAllBookings().stream()
                .filter(b -> !b.isCheckedOut())
                .filter(b -> {
                    if (currentUser == null) return false;
                    if (currentUser.isGuest()) {
                        // Guest only sees their own - handle name variations (Mithil vs Mithil S)
                        String bName = b.getGuestName().toLowerCase().trim();
                        String uName = currentUser.getFullName().toLowerCase().trim();
                        return bName.contains(uName) || uName.contains(bName);
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
