package hotel.model;


public class User extends Person {
    private static final long serialVersionUID = 3L;

    public enum Role {
        ADMIN("Admin", "Full access — rooms, staff, revenue, settings"),
        RECEPTIONIST("Receptionist", "Check-in/out, bookings, room service"),
        GUEST("Guest", "Book rooms, view history, update profile");

        private final String displayName;
        private final String description;

        Role(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
    }

    private String username;
    private String password;
    private Role role;
    private boolean active;

    public User() {}

    public User(String username, String password, String fullName, String email, Role role) {
        super(fullName, email, null);
        this.username = username;
        this.password = password;
        this.role = role;
        this.active = true;
    }

    @Override
    public String getRoleDescription() {
        return role != null ? role.getDescription() : "No role assigned";
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { 
        if (username == null || username.length() < 3) {
            throw new IllegalArgumentException("Username must be at least 3 characters");
        }
        this.username = username; 
    }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public boolean isAdmin() { return role == Role.ADMIN; }
    public boolean isReceptionist() { return role == Role.RECEPTIONIST; }
    public boolean isGuest() { return role == Role.GUEST; }

    @Override
    public String toString() {
        return fullName + " (" + (role != null ? role.getDisplayName() : "N/A") + ")";
    }
}
