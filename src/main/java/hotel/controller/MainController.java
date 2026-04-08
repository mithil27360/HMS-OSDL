package hotel.controller;

import hotel.dao.AuthService;
import hotel.dao.HotelService;
import hotel.dao.IAuthService;
import hotel.dao.IHotelService;
import hotel.model.User;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import java.util.HashMap;
import java.util.Map;

/**
 * Main application controller managing the sidebar navigation and content switching.
 */
public class MainController {

    private final IHotelService hotelService;
    private final IAuthService authService;
    private final Runnable onLogout;

    private BorderPane rootLayout;
    private VBox sidebar;
    private StackPane contentArea;
    private Map<String, Button> navButtons = new HashMap<>();
    private Map<String, Node> controllerCache = new HashMap<>();  // Cache controller views to preserve state
    private String activeNav = "";

    public MainController(Runnable onLogout) {
        this.hotelService = HotelService.getInstance();
        this.authService = AuthService.getInstance();
        this.onLogout = onLogout;
        buildView();
    }

    private void buildView() {
        rootLayout = new BorderPane();
        rootLayout.setStyle("-fx-background-color: #f4f4f6;");

        sidebar = buildSidebar();
        contentArea = new StackPane();
        contentArea.setPadding(new Insets(0));

        rootLayout.setLeft(sidebar);
        rootLayout.setCenter(contentArea);

        // Initial view based on role
        User user = authService.getCurrentUser();
        if (user != null) {
            if (user.isAdmin() || user.isReceptionist()) navigate("Dashboard");
            else navigate("Bookings");
        }
    }

    private VBox buildSidebar() {
        VBox bar = new VBox(0);
        bar.setPrefWidth(240);
        bar.getStyleClass().add("sidebar");
        bar.setStyle("-fx-background-color: #2c3e50;");

        VBox brand = new VBox(5);
        brand.setPadding(new Insets(30, 20, 40, 20));
        Label logo = new Label("HMS");
        logo.setStyle("-fx-text-fill: white; -fx-font-size: 24px; -fx-font-weight: bold;");
        brand.getChildren().addAll(logo);


        VBox nav = new VBox(4);
        nav.setPadding(new Insets(0, 10, 0, 10));

        User user = authService.getCurrentUser();
        if (user != null) {
            if (user.isAdmin() || user.isReceptionist()) {
                nav.getChildren().add(navButton("Dashboard", ""));
                nav.getChildren().add(navButton("Bookings", ""));
                nav.getChildren().add(navButton("Rooms", ""));
            }
            if (user.isAdmin()) {
                nav.getChildren().add(navButton("Staff", ""));
                nav.getChildren().add(navButton("Revenue", ""));
                nav.getChildren().add(navButton("System Logs", ""));
            }
            if (user.isGuest()) {
                nav.getChildren().add(navButton("My Bookings", ""));
            }
        }

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        VBox userProfile = new VBox(8);
        userProfile.setPadding(new Insets(20));
        userProfile.setStyle("-fx-background-color: #34495e;");
        
        Label userName = new Label(user != null ? user.getFullName() : "Guest");
        userName.setStyle("-fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold;");
        Label userRole = new Label(user != null ? user.getRole().getDisplayName() : "");
        userRole.setStyle("-fx-text-fill: #95a5a6; -fx-font-size: 11px;");

        Button logoutBtn = new Button("Sign Out");
        logoutBtn.getStyleClass().add("btn-logout");
        logoutBtn.setMaxWidth(Double.MAX_VALUE);
        logoutBtn.setOnAction(e -> {
            authService.logout();
            onLogout.run();
        });

        userProfile.getChildren().addAll(userName, userRole, logoutBtn);

        bar.getChildren().addAll(brand, nav, spacer, userProfile);
        return bar;
    }

    private Button navButton(String text, String icon) {
        String btnText = icon.isEmpty() ? text : icon + "  " + text;
        Button btn = new Button(btnText);

        btn.getStyleClass().add("nav-btn");
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setPrefHeight(45);
        btn.setOnAction(e -> navigate(text));
        navButtons.put(text, btn);
        return btn;
    }

    private void navigate(String target) {
        if (activeNav.equals(target)) return;
        
        if (!activeNav.isEmpty() && navButtons.containsKey(activeNav)) {
            navButtons.get(activeNav).getStyleClass().remove("nav-btn-active");
        }
        activeNav = target;
        if (navButtons.containsKey(target)) {
            navButtons.get(target).getStyleClass().add("nav-btn-active");
        }

        // CRITICAL FIX: Cache controller views to preserve state and avoid recreating on every navigation
        Node content = controllerCache.get(target);
        
        if (content == null) {
            // Create controller only if not cached
            switch (target) {
                case "Dashboard":
                    DashboardController dashCtrl = new DashboardController(hotelService);
                    content = dashCtrl.getView();
                    controllerCache.put(target, content);
                    break;
                case "Bookings":
                case "My Bookings":
                    BookingController bookCtrl = new BookingController(hotelService, this);
                    content = bookCtrl.getView();
                    controllerCache.put(target, content);
                    break;
                case "Rooms":
                    RoomController roomCtrl = new RoomController(hotelService);
                    content = roomCtrl.getView();
                    controllerCache.put(target, content);
                    break;
                case "Staff":
                    StaffController staffCtrl = new StaffController(authService);
                    content = staffCtrl.getView();
                    controllerCache.put(target, content);
                    break;
                case "Revenue":
                    BillingController billCtrl = new BillingController(hotelService);
                    content = billCtrl.getView();
                    controllerCache.put(target, content);
                    break;
                case "System Logs":
                    LogController logCtrl = new LogController(hotelService);
                    content = logCtrl.getView();
                    controllerCache.put(target, content);
                    break;
                default:
                    content = new Label("Coming soon...");
            }
        }

        contentArea.getChildren().setAll(content);
    }

    public BorderPane getRootLayout() { return rootLayout; }
}
