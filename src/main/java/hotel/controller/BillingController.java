package hotel.controller;

import hotel.dao.IHotelService;
import hotel.model.Bill;
import hotel.model.Booking;
import hotel.model.Room;
import hotel.util.BillingUtils;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import java.util.ArrayList;
import java.util.List;
import java.time.temporal.ChronoUnit;

public class BillingController {

    private final IHotelService hotelService; 
    private VBox view;
    private TableView<RevenueRecord> revenueTable;
    private TextArea billDetail;
    private Label totalRevLabel;
    private Label countLabel;
    private Label avgLabel;

    
    public static class RevenueRecord {
        private final String id;
        private final String guest;
        private final int room;
        private final String type;
        private final double amount;
        private final String date;
        private final String status; 
        private final Object originalObject;

        public RevenueRecord(String id, String guest, int room, String type, double amount, String date, String status, Object original) {
            this.id = id; this.guest = guest; this.room = room; this.type = type;
            this.amount = amount; this.date = date; this.status = status; this.originalObject = original;
        }

        public String getId() { return id; }
        public String getGuest() { return guest; }
        public int getRoom() { return room; }
        public String getType() { return type; }
        public double getAmount() { return amount; }
        public String getDate() { return date; }
        public String getStatus() { return status; }
        public Object getOriginalObject() { return originalObject; }
    }

    public BillingController(IHotelService hotelService) {
        this.hotelService = hotelService;
        buildView();
    }

    private void buildView() {
        view = new VBox(20);
        view.setPadding(new Insets(30));
        view.setStyle("-fx-background-color: #f4f4f6;");

        Label title = new Label("Revenue");
        title.setStyle("-fx-text-fill: #2c3e50; -fx-font-size: 22px; -fx-font-weight: bold;");

        HBox statsRow = buildStatsRow();

        HBox mainRow = new HBox(20);
        VBox.setVgrow(mainRow, Priority.ALWAYS);

        VBox tablePanel = buildTablePanel();
        HBox.setHgrow(tablePanel, Priority.ALWAYS);

        VBox detailPanel = buildDetailPanel();
        detailPanel.setMinWidth(340);
        detailPanel.setMaxWidth(340);

        mainRow.getChildren().addAll(tablePanel, detailPanel);

        view.getChildren().addAll(title, statsRow, mainRow);
        refresh();
    }

    private HBox buildStatsRow() {
        HBox row = new HBox(16);
        row.setAlignment(Pos.CENTER_LEFT);

        totalRevLabel = new Label("₹0");
        countLabel = new Label("0");
        avgLabel = new Label("₹0");

        row.getChildren().addAll(
            revCard("Total Combined Revenue", totalRevLabel, "#2c3e50"),
            revCard("Total Transactions", countLabel, "#3498db"),
            revCard("Value Per Entry", avgLabel, "#9b59b6")
        );
        return row;
    }

    private VBox revCard(String label, Label valueLabel, String color) {
        VBox card = new VBox(6);
        card.getStyleClass().add("stat-card");
        card.setMinWidth(200);

        valueLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 28px; -fx-font-weight: bold;");
        Label lbl = new Label(label.toUpperCase());
        lbl.getStyleClass().add("stat-label");

        card.getChildren().addAll(valueLabel, lbl);
        return card;
    }

    private VBox buildTablePanel() {
        VBox panel = new VBox(10);

        Label title = new Label("Revenue Timeline");
        title.setStyle("-fx-text-fill: #2c3e50; -fx-font-size: 15px; -fx-font-weight: bold;");

        revenueTable = new TableView<>();
        revenueTable.getStyleClass().add("table-view");
        revenueTable.setPlaceholder(new Label("No financial activity found."));
        VBox.setVgrow(revenueTable, Priority.ALWAYS);

        TableColumn<RevenueRecord, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colStatus.setPrefWidth(90);
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                if ("PAID".equals(item)) {
                    setText("PAID");
                    setStyle("-fx-text-fill: #ffffff; -fx-background-color: #2ecc71; -fx-background-radius: 4; -fx-alignment: center; -fx-font-weight: bold; -fx-padding: 2 5 2 5;");
                } else {
                    setText("BOOKED");
                    setStyle("-fx-text-fill: #ffffff; -fx-background-color: #f39c12; -fx-background-radius: 4; -fx-alignment: center; -fx-font-weight: bold; -fx-padding: 2 5 2 5;");
                }
            }
        });

        TableColumn<RevenueRecord, String> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colId.setPrefWidth(70);

        TableColumn<RevenueRecord, String> colGuest = new TableColumn<>("Guest");
        colGuest.setCellValueFactory(new PropertyValueFactory<>("guest"));
        colGuest.setPrefWidth(140);

        TableColumn<RevenueRecord, Integer> colRoom = new TableColumn<>("Room");
        colRoom.setCellValueFactory(new PropertyValueFactory<>("room"));
        colRoom.setPrefWidth(80);

        TableColumn<RevenueRecord, Double> colTotal = new TableColumn<>("Amount (₹)");
        colTotal.setCellValueFactory(new PropertyValueFactory<>("amount"));
        colTotal.setPrefWidth(110);
        colTotal.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); return; }
                setText("₹" + String.format("%.2f", v));
                setStyle("-fx-text-fill: #2c3e50; -fx-font-weight: bold;");
            }
        });

        TableColumn<RevenueRecord, String> colDate = new TableColumn<>("Date");
        colDate.setCellValueFactory(new PropertyValueFactory<>("date"));
        colDate.setPrefWidth(140);

        revenueTable.getColumns().setAll(List.of(colStatus, colId, colGuest, colRoom, colTotal, colDate));
        revenueTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        revenueTable.getSelectionModel().selectedItemProperty().addListener((obs, old, newVal) -> {
            if (newVal != null) {
                Object orig = newVal.getOriginalObject();
                if (orig instanceof Bill) {
                    billDetail.setText(((Bill) orig).generateBillText());
                } else if (orig instanceof Booking) {
                    Booking b = (Booking) orig;
                    billDetail.setText("TRANSACTION PREVIEW\n" +
                                     "--------------------\n" +
                                     "Status: BOOKED [PENDING]\n" +
                                     "Guest : " + b.getGuestName() + "\n" +
                                     "Room  : " + b.getRoomNumber() + "\n" +
                                     "Dates : " + b.getCheckIn() + " to " + b.getCheckOut() + "\n" +
                                     "Total: ₹" + String.format("%.2f", newVal.getAmount()));
                }
            }
        });

        panel.getChildren().addAll(title, revenueTable);
        return panel;
    }

    private VBox buildDetailPanel() {
        VBox panel = new VBox(12);
        panel.getStyleClass().add("panel-card");
        VBox.setVgrow(panel, Priority.ALWAYS);

        Label title = new Label("Revenue Detail");
        title.setStyle("-fx-text-fill: #3498db; -fx-font-size: 15px; -fx-font-weight: bold;");
        Region div = new Region(); div.getStyleClass().add("gold-divider");

        billDetail = new TextArea();
        billDetail.getStyleClass().add("text-area");
        billDetail.setEditable(false);
        billDetail.setPromptText("Select a row for financial breakdown...");
        VBox.setVgrow(billDetail, Priority.ALWAYS);

        panel.getChildren().addAll(title, div, billDetail);
        return panel;
    }

    public void refresh() {
        List<RevenueRecord> combined = new ArrayList<>();
        
        
        List<Bill> finalBills = hotelService.getAllBills();
        for (Bill b : finalBills) {
            combined.add(new RevenueRecord(
                "BILL-" + b.getBillId(),
                b.getGuestName(),
                b.getRoomNumber(),
                b.getRoomType(),
                b.getTotalAmount(),
                b.getCheckOutDate(),
                "PAID",
                b
            ));
        }

        
        List<Booking> bookings = hotelService.getAllBookings();
        for (Booking b : bookings) {
            if (!b.isCheckedOut()) {
                Room r = hotelService.getRoomByNumber(b.getRoomNumber());
                long nights = ChronoUnit.DAYS.between(b.getCheckIn(), b.getCheckOut());
                if (nights <= 0) nights = 1;
                double amount = (r != null ? r.getPricePerNight() : 0.0) * nights * BillingUtils.getTotalMultiplier();

                combined.add(new RevenueRecord(
                    "RES-" + b.getBookingId(),
                    b.getGuestName(),
                    b.getRoomNumber(),
                    r != null ? r.getRoomType().getDisplayName() : "N/A",
                    amount,
                    b.getCheckIn().toString(),
                    "BOOKED",
                    b
                ));
            }
        }

        revenueTable.setItems(FXCollections.observableArrayList(combined));

        
        double total = hotelService.getTotalRevenue();
        int entries = combined.size();
        double avg = entries > 0 ? total / entries : 0.0;

        totalRevLabel.setText("₹" + String.format("%.0f", total));
        countLabel.setText(String.valueOf(entries));
        avgLabel.setText("₹" + String.format("%.0f", avg));
    }

    public Node getView() { return view; }
}
