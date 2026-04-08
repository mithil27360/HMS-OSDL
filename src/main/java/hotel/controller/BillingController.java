package hotel.controller;

import hotel.dao.IHotelService;
import hotel.model.Bill;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;

public class BillingController {

    private final IHotelService hotelService; 
    private VBox view;
    private TableView<Bill> billTable;
    private TextArea billDetail;
    private Label totalRevLabel;
    private Label billCountLabel;
    private Label avgBillLabel;

    public BillingController(IHotelService hotelService) {
        this.hotelService = hotelService;
        buildView();
    }

    private void buildView() {
        view = new VBox(20);
        view.setPadding(new Insets(30));
        view.setStyle("-fx-background-color: #f4f4f6;");

        Label title = new Label("Billing & Revenue Management");
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

        panel.getChildren().addAll(title, div, billDetail);
        return panel;
    }

    public void refresh() {
        java.util.List<Bill> bills = hotelService.getAllBills();
        billTable.setItems(FXCollections.observableArrayList(bills));

        // Calculate and show stats from the service

        double total = hotelService.getCollectedRevenue();
        int count = bills.size();
        double avg = count > 0 ? total / count : 0.0;

        totalRevLabel.setText("₹" + String.format("%.0f", total));
        billCountLabel.setText(String.valueOf(count));
        avgBillLabel.setText("₹" + String.format("%.0f", avg));
    }

    public Node getView() { return view; }
}
