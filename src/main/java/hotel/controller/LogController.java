package hotel.controller;

import hotel.dao.IHotelService;
import hotel.util.GenericUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.util.List;

public class LogController {


    private final IHotelService hotelService; 
    private VBox view;
    private TextArea logArea;
    private TextArea bookLogArea;
    private VBox statsContainer;

    public LogController(IHotelService hotelService) {
        this.hotelService = hotelService;
        buildView();
    }

    private void buildView() {
        view = new VBox(20);
        view.setPadding(new Insets(30));
        view.setStyle("-fx-background-color: #f4f4f6;");

        Label title = new Label("System Activity & Reports");
        title.setStyle("-fx-text-fill: #2c3e50; -fx-font-size: 22px; -fx-font-weight: bold;");

        HBox mainRow = new HBox(20);
        VBox.setVgrow(mainRow, Priority.ALWAYS);

        VBox logPanel = buildLogPanel();
        HBox.setHgrow(logPanel, Priority.ALWAYS);

        VBox infoPanel = buildInfoPanel();
        infoPanel.setMinWidth(300);
        infoPanel.setMaxWidth(300);

        mainRow.getChildren().addAll(logPanel, infoPanel);
        view.getChildren().addAll(title, mainRow);
        refresh();
    }

    private VBox buildLogPanel() {
        VBox panel = new VBox(12);
        panel.getStyleClass().add("panel-card");
        VBox.setVgrow(panel, Priority.ALWAYS);

        Label t = new Label("Recent Activity Log");
        t.setStyle("-fx-text-fill: #3498db; -fx-font-size: 14px; -fx-font-weight: bold;");
        Region div = new Region(); div.getStyleClass().add("gold-divider");

        logArea = new TextArea();
        logArea.getStyleClass().add("text-area-log");
        logArea.setEditable(false);
        logArea.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 11px;");
        VBox.setVgrow(logArea, Priority.ALWAYS);

        HBox btnRow = new HBox(15);
        btnRow.setPadding(new Insets(5, 0, 5, 0));
        btnRow.setAlignment(Pos.CENTER_LEFT);
        
        Button refreshBtn = new Button("Refresh Log");
        refreshBtn.getStyleClass().add("btn-secondary");
        refreshBtn.setPrefHeight(35);
        refreshBtn.setMinWidth(120);
        refreshBtn.setOnAction(e -> refresh());

        Button clearBtn = new Button("Reset Statistics");
        clearBtn.getStyleClass().add("btn-danger");
        clearBtn.setPrefHeight(35);
        clearBtn.setMinWidth(130);
        clearBtn.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("System Reset");
            confirm.setHeaderText("Wipe all historical data?");
            confirm.setContentText("This will clear all earnings, logs, and unbook all rooms.");
            
            if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                hotelService.resetSystemData();
                refresh();
            }
        });

        btnRow.getChildren().addAll(refreshBtn, clearBtn);


        Label bookLogTitle = new Label("Audit Trail: Producer-Consumer Log Queue");
        bookLogTitle.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 12px;");
        bookLogArea = new TextArea();
        bookLogArea.getStyleClass().add("text-area-audit");
        bookLogArea.setEditable(false);
        bookLogArea.setMaxHeight(120);
        bookLogArea.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 11px; -fx-text-fill: #2ecc71;");

        panel.getChildren().addAll(t, div, logArea, btnRow, bookLogTitle, bookLogArea);
        return panel;
    }

    private VBox buildInfoPanel() {
        VBox panel = new VBox(14);
        panel.getStyleClass().add("panel-card-stats");

        Label t = new Label("Property Performance");
        t.setStyle("-fx-text-fill: #3498db; -fx-font-size: 14px; -fx-font-weight: bold;");
        Region div = new Region(); div.getStyleClass().add("gold-divider");

        statsContainer = new VBox(10);
        
        panel.getChildren().addAll(t, div, statsContainer);
        return panel;
    }

    private Node miniStat(String label, String value) {
        HBox row = new HBox(10);
        row.setPadding(new Insets(10, 15, 10, 15));
        row.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 8; -fx-border-color: #f0f0f0; -fx-border-radius: 8;");
        row.setAlignment(Pos.CENTER_LEFT);

        Label lbl = new Label(label + ":");
        lbl.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 11px; -fx-font-weight: bold;");
        Label val = new Label(value);
        val.setStyle("-fx-text-fill: #2c3e50; -fx-font-size: 13px; -fx-font-weight: bold;");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        row.getChildren().addAll(lbl, spacer, val);
        return row;
    }

    public void refresh() {
        if (logArea != null) {
            String log = hotelService.getActivityLog();
            logArea.setText(log.isEmpty() ? "(No activity yet)" : log);
            
            // Ensure auto-scroll to bottom
            javafx.application.Platform.runLater(() -> {
                logArea.selectPositionCaret(logArea.getLength());
                logArea.deselect();
            });
        }

        if (statsContainer != null) {
            statsContainer.getChildren().clear();
            
            long totalRooms = hotelService.getAllRooms().size();
            long occupiedToday = hotelService.getBookedRooms(java.time.LocalDate.now()).size();
            double occupancyRate = totalRooms > 0 ? (double) occupiedToday / totalRooms * 100 : 0;
            
            java.time.LocalDate now = java.time.LocalDate.now();
            double monthlyRevenue = hotelService.getAllBills().stream()
                .filter(b -> {
                    try {
                        java.time.LocalDate checkOut = java.time.LocalDate.parse(b.getCheckOutDate());
                        return checkOut.getMonth() == now.getMonth() && checkOut.getYear() == now.getYear();
                    } catch (Exception e) { return false; }
                })
                .mapToDouble(hotel.model.Bill::getTotalAmount)
                .sum();

            statsContainer.getChildren().addAll(
                miniStat("Total Capacity", totalRooms + " Units"),
                miniStat("Occupancy Today", String.format("%.1f%%", occupancyRate)),
                miniStat("Monthly Revenue", GenericUtils.formatRupees(monthlyRevenue)),
                miniStat("Lifetime Revenue", GenericUtils.formatRupees(hotelService.getTotalRevenue()))
            );
        }

        if (bookLogArea != null) {
            List<hotel.model.Booking> log = hotelService.getAllBookings();
            if (log.isEmpty()) {
                bookLogArea.setText("No active session audit trail.");
            } else {
                StringBuilder sb = new StringBuilder();
                log.forEach(b -> sb.append("[AUDIT] Room ").append(b.getRoomNumber())
                                   .append(" -> ").append(b.getGuestName())
                                   .append(" [").append(b.getCheckIn()).append("]\n"));
                bookLogArea.setText(sb.toString());
            }
        }
    }

    public Node getView() { return view; }
}
