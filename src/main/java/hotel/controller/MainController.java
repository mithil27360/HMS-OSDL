package hotel.controller;

import hotel.dao.AuthService;
import hotel.dao.HotelService;
import hotel.model.User;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;

/**
 * Main Controller
 * Orchestrates navigation and view management using a sidebar-driven BorderPane.
 * Features: Null-safe user handling, Singleton service integration.
 */
public class MainController {

    private final HotelService hotelService;
    private final AuthService authService;
    private final Runnable onLogout;

    private BorderPane rootLayout;
    private StackPane contentPane;

    private DashboardController dashboardController;
    private RoomController roomController;
    private BookingController bookingController;
    private BillingController billingController;
    private LogController logController;
    private StaffController staffController;

    private Button activeBtn = null;

    public MainController(Runnable onLogout) {
        this.onLogout = onLogout;
        // Fix: Use Singleton instances to prevent duplicate seeding and state loss
        this.hotelService = HotelService.getInstance();
        this.authService  = AuthService.getInstance();
        buildLayout();
    }

    private void buildLayout() {
        rootLayout = new BorderPane();
        rootLayout.setStyle("-fx-background-color: #f4f4f6;");
        
        rootLayout.setLeft(buildSidebar());
        
        contentPane = new StackPane();
        contentPane.setStyle("-fx-background-color: #f4f4f6;");
        rootLayout.setCenter(contentPane);

        dashboardController = new DashboardController(hotelService, this);
        roomController      = new RoomController(hotelService, this);
        bookingController   = new BookingController(hotelService, this);
        billingController   = new BillingController(hotelService);
        logController       = new LogController(hotelService);
        staffController     = new StaffController(authService);

        showDashboard();
    }

    private VBox buildSidebar() {
        VBox sidebar = new VBox(0);
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPrefWidth(240);

        // 1. Brand Area
        VBox brand = new VBox(2);
        brand.getStyleClass().add("sidebar-brand");
        Label title = new Label("★ HOTEL");
        title.getStyleClass().add("nav-title");
        Label sub = new Label("MANAGEMENT SYSTEM");
        sub.getStyleClass().add("nav-subtitle");
        brand.getChildren().addAll(title, sub);

        // 2. Navigation Area
        VBox navBtns = new VBox(0);
        VBox.setVgrow(navBtns, Priority.ALWAYS);
        navBtns.getStyleClass().add("sidebar-btn-container");

        User user = authService.getCurrentUser();
        if (user == null) return sidebar; // Null safety

        Button btnDash = navButton("Dashboard");
        btnDash.setOnAction(e -> { setActive(btnDash); showDashboard(); });
        navBtns.getChildren().add(btnDash);
        setActive(btnDash);

        if (user.isAdmin() || user.isReceptionist()) {
            Button btnBook  = navButton("Bookings");
            Button btnRooms = navButton("Room Management");
            Button btnBill  = navButton("Billing Service");
            btnBook.setOnAction(e  -> { setActive(btnBook);  showBookings(); });
            btnRooms.setOnAction(e -> { setActive(btnRooms); showRooms(); });
            btnBill.setOnAction(e  -> { setActive(btnBill);  showBilling(); });
            navBtns.getChildren().addAll(btnBook, btnRooms, btnBill);
        }

        if (user.isAdmin()) {
            Button btnStaff = navButton("Staff Manager");
            Button btnLog   = navButton("System Activity");
            btnStaff.setOnAction(e -> { setActive(btnStaff); showStaff(); });
            btnLog.setOnAction(e   -> { setActive(btnLog);   showLog(); });
            navBtns.getChildren().addAll(btnStaff, btnLog);
        }

        if (user.isGuest()) {
            Button btnMyBook = navButton("My Bookings");
            btnMyBook.setOnAction(e -> { setActive(btnMyBook); showBookings(); });
            navBtns.getChildren().add(btnMyBook);
        }

        // 3. User & Logout Area
        VBox bottomArea = new VBox(10);
        bottomArea.getStyleClass().add("sidebar-bottom");

        String roleColor = user.isAdmin() ? "#3498db" : user.isReceptionist() ? "#3498db" : "#2ecc71";
        
        Label userInfo = new Label(user.getFullName());
        userInfo.setStyle("-fx-text-fill: #2c3e50; -fx-font-size: 13px; -fx-font-weight: bold;");
        
        Label roleBadge = new Label(user.getRole().getDisplayName().toUpperCase());
        roleBadge.setStyle(
            "-fx-background-color: transparent; -fx-text-fill: " + roleColor + ";" +
            "-fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 3 8;" +
            "-fx-border-color: " + roleColor + "; -fx-border-radius: 4; -fx-border-width: 1;"
        );

        Button logoutBtn = new Button("Sign Out");
        logoutBtn.setMaxWidth(Double.MAX_VALUE);
        logoutBtn.setStyle(baseLogoutStyle());
        logoutBtn.setOnMouseEntered(e -> logoutBtn.setStyle(hoverLogoutStyle()));
        logoutBtn.setOnMouseExited(e  -> logoutBtn.setStyle(baseLogoutStyle()));
        logoutBtn.setOnAction(e -> { authService.logout(); onLogout.run(); });

        bottomArea.getChildren().addAll(userInfo, roleBadge, logoutBtn);

        sidebar.getChildren().addAll(brand, navBtns, bottomArea);
        return sidebar;
    }

    private String baseLogoutStyle() {
        return "-fx-background-color: transparent; -fx-text-fill: #95a5a6; -fx-font-size: 12px;" +
               "-fx-cursor: hand; -fx-padding: 8 16; -fx-border-color: #ecf0f1;" +
               "-fx-border-radius: 6; -fx-border-width: 1;";
    }
    private String hoverLogoutStyle() {
        return "-fx-background-color: #fff5f5; -fx-text-fill: #e74c3c; -fx-font-size: 12px;" +
               "-fx-cursor: hand; -fx-padding: 8 16; -fx-border-color: #ffcccc;" +
               "-fx-border-radius: 6; -fx-border-width: 1;";
    }

    private Button navButton(String text) {
        Button btn = new Button(text);
        btn.getStyleClass().add("nav-btn");
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setMaxWidth(Double.MAX_VALUE);
        return btn;
    }

    private void setActive(Button btn) {
        if (activeBtn != null) {
            activeBtn.getStyleClass().remove("nav-btn-active");
            if (!activeBtn.getStyleClass().contains("nav-btn"))
                activeBtn.getStyleClass().add("nav-btn");
        }
        btn.getStyleClass().remove("nav-btn");
        btn.getStyleClass().add("nav-btn-active");
        activeBtn = btn;
    }

    public void showDashboard() { dashboardController.refresh(); setContent(dashboardController.getView()); }
    public void showRooms()     { roomController.refresh();      setContent(roomController.getView()); }
    public void showBookings()  { bookingController.refresh();   setContent(bookingController.getView()); }
    public void showBilling()   { billingController.refresh();   setContent(billingController.getView()); }
    public void showStaff()     { staffController.refresh();     setContent(staffController.getView()); }
    public void showLog()       { logController.refresh();       setContent(logController.getView()); }

    private void setContent(javafx.scene.Node node) { 
        if (node != null) contentPane.getChildren().setAll(node); 
    }
    public BorderPane getRootLayout() { return rootLayout; }
    public HotelService getHotelService() { return hotelService; }
}
