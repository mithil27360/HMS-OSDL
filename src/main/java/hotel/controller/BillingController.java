package hotel.controller;

import hotel.dao.HotelService;
import hotel.model.Bill;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;

/**
 * Billing Management Controller
 * - View all past bills
 * - Show full bill details
 * - Revenue summary using Wrapper classes
 */
public class BillingController {

    private final HotelService hotelService;
    private VBox view;
    private TableView<Bill> billTable;
    private TextArea billDetail;
    private Label totalRevLabel;
    private Label billCountLabel;
    private Label avgBillLabel;

    public BillingController(HotelService hotelService) {
        this.hotelService = hotelService;
        buildView();
    }

    private void buildView() {
        view = new VBox(20);
        view.setPadding(new Insets(30));
        view.setStyle("-fx-background-color: #f4f4f6;");

        // Header
        Label title = new Label("Billing & Revenue Management");
        title.setStyle("-fx-text-fill: #2c3e50; -fx-font-size: 22px; -fx-font-weight: bold;");

        // Stats row
        HBox statsRow = buildStatsRow();

        // Main: table + detail
        HBox mainRow = new HBox(20);
        VBox.setVgrow(mainRow, Priority.ALWAYS);

        // Bill table
        VBox tablePanel = buildTablePanel();
        HBox.setHgrow(tablePanel, Priority.ALWAYS);

        // Bill detail panel
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

        // Wrapper class usage for display
        totalRevLabel = new Label("₹0");
        billCountLabel = new Label("0");
        avgBillLabel = new Label("₹0");

        row.getChildren().addAll(
            revCard("Total Revenue", totalRevLabel, "#3498db"),
            revCard("Bills Generated", billCountLabel, "#3498db"),
            revCard("Average Bill", avgBillLabel, "#9b59b6")
        );
        return row;
    }

    private VBox revCard(String label, Label valueLabel, String color) {
        VBox card = new VBox(6);
        card.getStyleClass().add("stat-card");
        card.setMinWidth(180);

        valueLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 28px; -fx-font-weight: bold;");
        Label lbl = new Label(label.toUpperCase());
        lbl.getStyleClass().add("stat-label");

        card.getChildren().addAll(valueLabel, lbl);
        return card;
    }

    private VBox buildTablePanel() {
        VBox panel = new VBox(10);

        Label title = new Label("Bill History");
        title.setStyle("-fx-text-fill: #2c3e50; -fx-font-size: 15px; -fx-font-weight: bold;");

        billTable = new TableView<>();
        billTable.getStyleClass().add("table-view");
        billTable.setPlaceholder(new Label("No bills yet"));
        VBox.setVgrow(billTable, Priority.ALWAYS);

        TableColumn<Bill, Integer> colId = new TableColumn<>("Bill #");
        colId.setCellValueFactory(new PropertyValueFactory<>("billId"));
        colId.setPrefWidth(70);

        TableColumn<Bill, String> colGuest = new TableColumn<>("Guest");
        colGuest.setCellValueFactory(new PropertyValueFactory<>("guestName"));
        colGuest.setPrefWidth(140);

        TableColumn<Bill, Integer> colRoom = new TableColumn<>("Room");
        colRoom.setCellValueFactory(new PropertyValueFactory<>("roomNumber"));
        colRoom.setPrefWidth(80);

        TableColumn<Bill, String> colType = new TableColumn<>("Type");
        colType.setCellValueFactory(new PropertyValueFactory<>("roomType"));
        colType.setPrefWidth(120);

        TableColumn<Bill, Integer> colDays = new TableColumn<>("Days");
        colDays.setCellValueFactory(new PropertyValueFactory<>("numberOfDays"));
        colDays.setPrefWidth(70);

        TableColumn<Bill, Double> colTotal = new TableColumn<>("Total (₹)");
        colTotal.setCellValueFactory(new PropertyValueFactory<>("totalAmount"));
        colTotal.setPrefWidth(110);
        colTotal.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); return; }
                setText("₹" + String.format("%.2f", v));
                setStyle("-fx-text-fill: #3498db; -fx-font-weight: bold;");
            }
        });

        TableColumn<Bill, String> colDate = new TableColumn<>("Checkout Date");
        colDate.setCellValueFactory(new PropertyValueFactory<>("checkOutDate"));
        colDate.setPrefWidth(140);

        billTable.getColumns().addAll(colId, colGuest, colRoom, colType, colDays, colTotal, colDate);
        billTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Show bill detail on row click (Event handling - Week 9)
        billTable.getSelectionModel().selectedItemProperty().addListener((obs, old, newVal) -> {
            if (newVal != null) billDetail.setText(newVal.generateBillText());
        });

        panel.getChildren().addAll(title, billTable);
        return panel;
    }

    private VBox buildDetailPanel() {
        VBox panel = new VBox(12);
        panel.getStyleClass().add("panel-card");
        VBox.setVgrow(panel, Priority.ALWAYS);

        Label title = new Label("Bill Preview");
        title.setStyle("-fx-text-fill: #3498db; -fx-font-size: 15px; -fx-font-weight: bold;");
        Region div = new Region(); div.getStyleClass().add("gold-divider");

        billDetail = new TextArea();
        billDetail.getStyleClass().add("text-area");
        billDetail.setEditable(false);
        billDetail.setPromptText("Click a bill row to preview...");
        VBox.setVgrow(billDetail, Priority.ALWAYS);

        // Discount calculator (Bounded generics - Week 7)
        Label discTitle = new Label("Discount Calculator");
        discTitle.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 13px; -fx-font-weight: bold;");

        HBox discRow = new HBox(8);
        discRow.setAlignment(Pos.CENTER_LEFT);
        TextField discField = new TextField("10");
        discField.getStyleClass().add("text-field");
        discField.setMaxWidth(60);
        discField.setPromptText("%");
        Label discResult = new Label();
        discResult.setStyle("-fx-text-fill: #2ecc71; -fx-font-size: 12px;");

        Button calcBtn = new Button("Calc");
        calcBtn.getStyleClass().add("btn-secondary");
        calcBtn.setOnAction(e -> {
            Bill sel = billTable.getSelectionModel().getSelectedItem();
            if (sel == null) { discResult.setText("Select a bill"); return; }
            try {
                double pct = Double.parseDouble(discField.getText());
                // Bounded generic method
                double discounted = hotelService.getDiscountedPrice(sel.getTotalAmount(), pct);
                discResult.setText(String.format("₹%.2f (%.0f%% off)", discounted, pct));
            } catch (NumberFormatException ex) {
                discResult.setText("Invalid %");
            }
        });
        discRow.getChildren().addAll(new Label("Disc %:"), discField, calcBtn, discResult);
        discRow.setStyle("-fx-padding: 0;");
        ((Label)discRow.getChildren().get(0)).setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 12px;");

        panel.getChildren().addAll(title, div, billDetail, discTitle, discRow);
        return panel;
    }

    public void refresh() {
        java.util.List<Bill> bills = hotelService.getAllBills();
        billTable.setItems(FXCollections.observableArrayList(bills));

        // Wrapper class usage: Double for arithmetic
        Double total = hotelService.getTotalRevenue();  // wrapper
        Integer count = bills.size();                   // wrapper (autoboxing)
        Double avg = count > 0 ? total / count : 0.0;  // unboxing arithmetic

        totalRevLabel.setText("₹" + String.format("%.0f", total));
        billCountLabel.setText(String.valueOf(count));
        avgBillLabel.setText("₹" + String.format("%.0f", avg));
    }

    public Node getView() { return view; }
}
