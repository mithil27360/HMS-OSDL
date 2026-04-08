package hotel.controller;

import hotel.dao.HotelService;
import hotel.model.Room;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.*;

import java.util.ArrayList;

/**
 * Dashboard View
 * Shows overview stats and room quick-view.
 */
public class DashboardController {

    private final HotelService hotelService;
    private final MainController mainController;
    private VBox view;

    public DashboardController(HotelService hotelService, MainController mainController) {
        this.hotelService = hotelService;
        this.mainController = mainController;
        buildView();
    }

    private void buildView() {
        view = new VBox(24);
        view.setPadding(new Insets(30, 30, 30, 30));
        view.getStyleClass().add("content-area");
        refresh();
    }

    public void refresh() {
        view.getChildren().clear();

        // ─── Header ───────────────────────────────────────────────────────────
        VBox header = new VBox(4);
        Label welcome = new Label("Welcome Back");
        welcome.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 14px;");
        Label title = new Label("Hotel — Dashboard");
        title.getStyleClass().add("section-title");
        title.setStyle("-fx-text-fill: #2c3e50; -fx-font-size: 24px; -fx-font-weight: bold;");
        header.getChildren().addAll(welcome, title);

        // ─── Stats Row ────────────────────────────────────────────────────────
        HBox statsRow = new HBox(16);
        statsRow.setAlignment(Pos.CENTER_LEFT);

        statsRow.getChildren().addAll(
            statCard("Total Rooms", String.valueOf(hotelService.getTotalRooms()), "#3498db", false),
            statCard("Available", String.valueOf(hotelService.getAvailableCount()), "#2ecc71", false),
            statCard("Occupied", String.valueOf(hotelService.getOccupiedCount()), "#e74c3c", false),
            statCard("Total Earnings", "₹" + String.format("%.0f", hotelService.getTotalRevenue()), "#3498db", true)
        );

        // ─── Divider ──────────────────────────────────────────────────────────
        Region divider = new Region();
        divider.getStyleClass().add("gold-divider");
        divider.setMaxWidth(Double.MAX_VALUE);

        // ─── Room Overview Grid ───────────────────────────────────────────────
        Label roomsTitle = new Label("Room Overview");
        roomsTitle.getStyleClass().add("section-title");

        FlowPane roomGrid = buildRoomGrid();

        // ─── Room Type Summary ────────────────────────────────────────────────
        Label typesTitle = new Label("Room Types & Pricing");
        typesTitle.setStyle("-fx-text-fill: #2c3e50; -fx-font-size: 16px; -fx-font-weight: bold;");

        HBox typesRow = buildRoomTypesRow();

        view.getChildren().addAll(header, statsRow, divider, roomsTitle, roomGrid, typesTitle, typesRow);
    }

    private Node statCard(String label, String value, String color, boolean gold) {
        VBox card = new VBox(6);
        card.getStyleClass().add(gold ? "stat-card-gold" : "stat-card");
        card.setAlignment(Pos.CENTER_LEFT);
        card.setMinWidth(170);

        Label valLabel = new Label(value);
        valLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 32px; -fx-font-weight: bold;");

        Label nameLabel = new Label(label.toUpperCase());
        nameLabel.getStyleClass().add("stat-label");

        card.getChildren().addAll(valLabel, nameLabel);
        return card;
    }

    private FlowPane buildRoomGrid() {
        FlowPane flow = new FlowPane();
        flow.setHgap(10);
        flow.setVgap(10);
        flow.setPrefWrapLength(900);

        for (Room room : hotelService.getRoomsSortedByNumber()) {
            VBox cell = new VBox(4);
            cell.setAlignment(Pos.CENTER);
            cell.setPrefSize(90, 75);
            cell.setPadding(new Insets(8));
            cell.setStyle(
                "-fx-background-color: " + (room.isBooked() ? "#fff5f5" : "#f0fff5") + ";" +
                "-fx-background-radius: 8;" +
                "-fx-border-color: " + (room.isBooked() ? "#ffcccc" : "#ccffdd") + ";" +
                "-fx-border-radius: 8; -fx-border-width: 1.5; -fx-cursor: hand;"
            );

            Label numLabel = new Label(String.valueOf(room.getRoomNumber()));
            numLabel.setStyle("-fx-text-fill: " + (room.isBooked() ? "#e74c3c" : "#27ae60") + "; " +
                "-fx-font-size: 16px; -fx-font-weight: bold;");

            Label typeLabel = new Label(room.getRoomType().name());
            typeLabel.setStyle("-fx-text-fill: #95a5a6; -fx-font-size: 9px;");

            Label statusLabel = new Label(room.isBooked() ? "OCCUPIED" : "AVAILABLE");
            statusLabel.setStyle("-fx-text-fill: " + (room.isBooked() ? "#e74c3c" : "#27ae60") +
                "; -fx-font-size: 9px; -fx-font-weight: bold;");

            cell.getChildren().addAll(numLabel, typeLabel, statusLabel);
            flow.getChildren().add(cell);
        }
        return flow;
    }

    private HBox buildRoomTypesRow() {
        HBox row = new HBox(14);

        for (Room.RoomType type : Room.RoomType.values()) {
            long count = hotelService.getRoomsByType(type).size();
            long available = hotelService.getRoomsByType(type).stream()
                    .filter(r -> !r.isBooked()).count();

            VBox card = new VBox(6);
            card.setPadding(new Insets(16 ,20, 16, 20));
            card.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 10;" +
                    "-fx-border-color: #bdc3c7; -fx-border-radius: 10; -fx-border-width: 1;");
            card.setMinWidth(160);

            Label typeName = new Label(type.getDisplayName());
            typeName.setStyle("-fx-text-fill: #3498db; -fx-font-size: 13px; -fx-font-weight: bold;");

            Label price = new Label("₹" + String.format("%.0f", type.getBasePrice()) + "/night");
            price.setStyle("-fx-text-fill: #ccccdd; -fx-font-size: 12px;");

            Label avail = new Label(available + " of " + count + " available");
            avail.setStyle("-fx-text-fill: " + (available > 0 ? "#2ecc71" : "#e74c3c") +
                    "; -fx-font-size: 11px;");

            card.getChildren().addAll(typeName, price, avail);
            row.getChildren().add(card);
        }
        return row;
    }

    public Node getView() { return view; }
}
