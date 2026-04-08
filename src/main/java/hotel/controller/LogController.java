package hotel.controller;

import hotel.dao.HotelService;
import hotel.util.Pair;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;

/**
 * Log & Info View
 * Shows activity log, booking log (Generic Pair), and concept summary.
 */
public class LogController {

    private final HotelService hotelService;
    private VBox view;
    private TextArea logArea;
    private TextArea bookLogArea;

    public LogController(HotelService hotelService) {
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

        // Log panel
        VBox logPanel = buildLogPanel();
        HBox.setHgrow(logPanel, Priority.ALWAYS);

        // Info panel
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
        logArea.getStyleClass().add("text-area");
        logArea.setEditable(false);
        VBox.setVgrow(logArea, Priority.ALWAYS);

        HBox btnRow = new HBox(10);
        Button refreshBtn = new Button("Refresh Log");
        refreshBtn.getStyleClass().add("btn-secondary");
        refreshBtn.setOnAction(e -> refresh());

        Button backupBtn = new Button("Backup Log");
        backupBtn.getStyleClass().add("btn-secondary");
        backupBtn.setOnAction(e -> {
            hotel.dao.FileStorage.copyLogToBackup();
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Log backed up to hotel_activity_backup.log");
            alert.setTitle("Backup");
            alert.showAndWait();
        });

        Button clearBtn = new Button("Reset Statistics");
        clearBtn.getStyleClass().add("btn-danger");
        clearBtn.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("System Reset");
            confirm.setHeaderText("Wipe all historical data?");
            confirm.setContentText("This will clear all earnings, logs, and unbook all rooms. This cannot be undone.");
            
            if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                hotelService.resetSystemData();
                refresh();
                Alert ok = new Alert(Alert.AlertType.INFORMATION, "System has been reset for a clean presentation.");
                ok.showAndWait();
            }
        });

        btnRow.getChildren().addAll(refreshBtn, backupBtn, clearBtn);

        Label bookLogTitle = new Label("Current Session Transaction Summary");
        bookLogTitle.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 12px;");
        bookLogArea = new TextArea();
        bookLogArea.getStyleClass().add("text-area");
        bookLogArea.setEditable(false);
        bookLogArea.setMaxHeight(120);

        panel.getChildren().addAll(t, div, logArea, btnRow, bookLogTitle, bookLogArea);
        return panel;
    }

    private VBox buildInfoPanel() {
        VBox panel = new VBox(14);
        panel.getStyleClass().add("panel-card");

        Label t = new Label("System Overview");
        t.setStyle("-fx-text-fill: #3498db; -fx-font-size: 14px; -fx-font-weight: bold;");
        Region div = new Region(); div.getStyleClass().add("gold-divider");

        VBox stats = new VBox(10);
        stats.getChildren().addAll(
            miniStat("Total Earnings", "₹" + String.format("%.2f", hotelService.getTotalRevenue())),
            miniStat("Total Rooms", String.valueOf(hotelService.getTotalRooms())),
            miniStat("Occupied", String.valueOf(hotelService.getOccupiedCount())),
            miniStat("Available", String.valueOf(hotelService.getAvailableCount()))
        );

        Label sessionTitle = new Label("Current Session Analytics");
        sessionTitle.setStyle("-fx-text-fill: #2ecc71; -fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 8 0 0 0;");

        Label sessionDesc = new Label("Real-time monitoring of local storage transactions and runtime threading.");
        sessionDesc.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 11px;");
        sessionDesc.setWrapText(true);

        panel.getChildren().addAll(t, div, stats, sessionTitle, sessionDesc);
        return panel;
    }

    private Node miniStat(String label, String value) {
        HBox row = new HBox(10);
        row.setPadding(new Insets(8, 12, 8, 12));
        row.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 6; -fx-border-color: #f0f0f0; -fx-border-radius: 6;");
        row.setAlignment(Pos.CENTER_LEFT);

        Label lbl = new Label(label + ":");
        lbl.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 11px; -fx-font-weight: bold;");
        Label val = new Label(value);
        val.setStyle("-fx-text-fill: #2c3e50; -fx-font-size: 12px; -fx-font-weight: bold;");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        row.getChildren().addAll(lbl, spacer, val);
        return row;
    }

    public void refresh() {
        if (logArea != null) {
            String log = hotelService.getActivityLog();
            logArea.setText(log.isEmpty() ? "(No activity yet)" : log);
            logArea.setScrollTop(Double.MAX_VALUE);
        }
        if (bookLogArea != null) {
            List<Pair<Integer, String>> log = hotelService.getBookingLog();
            if (log.isEmpty()) {
                bookLogArea.setText("No bookings in this session.");
            } else {
                StringBuilder sb = new StringBuilder();
                for (Pair<Integer, String> p : log) {
                    sb.append("Room ").append(p.getFirst())
                      .append(" -> ").append(p.getSecond()).append("\n");
                }
                bookLogArea.setText(sb.toString());
            }
        }
    }

    public Node getView() { return view; }
}
