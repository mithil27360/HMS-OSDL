package hotel.model;

import java.io.Serializable;

/**
 * User model with role-based access control.
 * Demonstrates: Encapsulation (Week 1), Enum (Week 2), Serialization (Week 6)
 */
public class User implements Serializable {
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
    private String fullName;
    private String email;
    private String phone;
    private Role role;
    private boolean active;

    public User() {}

    public User(String username, String password, String fullName, String email, Role role) {
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.email = email;
        this.role = role;
        this.active = true;
    }

    // Getters & Setters (Encapsulation - Week 1)
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    // Role check helpers
    public boolean isAdmin() { return role == Role.ADMIN; }
    public boolean isReceptionist() { return role == Role.RECEPTIONIST; }
    public boolean isGuest() { return role == Role.GUEST; }

    @Override
    public String toString() {
        return fullName + " (" + role.getDisplayName() + ")";
    }
}
