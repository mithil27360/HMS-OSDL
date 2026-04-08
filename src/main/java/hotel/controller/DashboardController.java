package hotel.controller;

import hotel.dao.IHotelService;
import hotel.model.Room;
import hotel.util.GenericUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.*;

/**
 * Controller for the Main Dashboard view.
 */
public class DashboardController {

    private final IHotelService hotelService; 

    private VBox view;
    
    private Label totalRoomsLbl;
    private Label availRoomsLbl;
    private Label revenueLbl;
    private Label occupancyLbl;

    public DashboardController(IHotelService hotelService) {
        this.hotelService = hotelService;
        buildView();
    }

    private void buildView() {
        view = new VBox(24);
        view.setPadding(new Insets(30));
        view.setStyle("-fx-background-color: #f4f4f6;");

        Label title = new Label("Dashboard");
        title.setStyle("-fx-text-fill: #2c3e50; -fx-font-size: 24px; -fx-font-weight: bold;");

        HBox topRow = new HBox(20);
        topRow.setAlignment(Pos.CENTER_LEFT);

        totalRoomsLbl = new Label("0");
        availRoomsLbl = new Label("0");
        revenueLbl = new Label("₹0"); 
        Label projectedLbl = new Label("₹0");
        occupancyLbl = new Label("0%");

        topRow.getChildren().addAll(
            statCard("TOTAL ROOMS", totalRoomsLbl, "#3498db"),
            statCard("AVAILABLE NOW", availRoomsLbl, "#2ecc71"),
            statCard("COLLECTED REVENUE", revenueLbl, "#f1c40f"),
            statCard("PROJECTED REVENUE", projectedLbl, "#e67e22"),
            statCard("OCCUPANCY RATE", occupancyLbl, "#9b59b6")
        );

        // We need to keep a reference to projectedLbl to refresh it
        view.setUserData(projectedLbl);

        view.getChildren().addAll(title, topRow);
        refresh();
    }


    private VBox statCard(String title, Label valLabel, String color) {
        VBox card = new VBox(8);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(20));
        card.setMinWidth(200);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 10, 0, 0, 5);");

        Label t = new Label(title);
        t.setStyle("-fx-text-fill: #95a5a6; -fx-font-size: 11px; -fx-font-weight: bold; -fx-letter-spacing: 1;");
        
        valLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 26px; -fx-font-weight: bold;");
        
        card.getChildren().addAll(t, valLabel);
        return card;
    }



    public void refresh() {
        // Calculate stats using the service

        java.util.List<Room> all = hotelService.getAllRooms();
        java.util.List<Room> avail = hotelService.getAvailableRooms();
        
        totalRoomsLbl.setText(String.valueOf(all.size()));
        availRoomsLbl.setText(String.valueOf(avail.size()));
        revenueLbl.setText(GenericUtils.formatRupees(hotelService.getCollectedRevenue()));
        
        Label projLbl = (Label) view.getUserData();
        if (projLbl != null) {
            projLbl.setText(GenericUtils.formatRupees(hotelService.getProjectedRevenue()));
        }
        
        double rate = all.isEmpty() ? 0 : ((double)(all.size() - avail.size()) / all.size()) * 100;
        occupancyLbl.setText(String.format("%.1f%%", rate));
    }

    public Node getView() { return view; }
}
