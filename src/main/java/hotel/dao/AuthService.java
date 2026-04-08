package hotel.dao;

import hotel.model.User;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Authentication Service — manages users, login, serialization (Week 6)
 * Uses ArrayList (Week 8), Serialization (Week 6)
 */
public class AuthService {

    private static final String USERS_FILE = "hotel_users.dat";
    private static AuthService instance;
    private List<User> users;
    private User currentUser;

    private AuthService() {
        users = loadUsers();
        if (users.isEmpty()) seedDefaultUsers();
    }

    public static AuthService getInstance() {
        if (instance == null) instance = new AuthService();
        return instance;
    }

    // ─── Seed default accounts ────────────────────────────────────────────────
    private void seedDefaultUsers() {
        users.add(new User("admin",        "admin123",  "Admin Manager",     "admin@grandvista.com",  User.Role.ADMIN));
        users.add(new User("receptionist", "staff123",  "Priya Sharma",      "priya@grandvista.com",  User.Role.RECEPTIONIST));
        users.add(new User("guest",        "guest123",  "Rahul Kumar",       "rahul@email.com",       User.Role.GUEST));
        saveUsers();
    }

    // ─── Login ────────────────────────────────────────────────────────────────
    public User login(String username, String password) {
        for (User u : users) {
            if (u.getUsername().equals(username) &&
                u.getPassword().equals(password) &&
                u.isActive()) {
                currentUser = u;
                FileStorage.writeLog("Login: " + u.getFullName() + " [" + u.getRole() + "]");
                return u;
            }
        }
        return null;
    }

    public void logout() {
        if (currentUser != null)
            FileStorage.writeLog("Logout: " + currentUser.getFullName());
        currentUser = null;
    }

    public User getCurrentUser() { return currentUser; }

    // ─── User CRUD (Admin only) ───────────────────────────────────────────────
    public boolean addUser(User user) {
        for (User u : users) {
            if (u.getUsername().equals(user.getUsername())) return false; // duplicate
        }
        users.add(user);
        saveUsers();
        FileStorage.writeLog("User added: " + user.getUsername() + " [" + user.getRole() + "]");
        return true;
    }

    public boolean deleteUser(String username) {
        return users.removeIf(u -> u.getUsername().equals(username) && !u.getUsername().equals("admin"));
    }

    public List<User> getAllUsers() { return new ArrayList<>(users); }

    public List<User> getUsersByRole(User.Role role) {
        List<User> result = new ArrayList<>();
        for (User u : users) if (u.getRole() == role) result.add(u);
        return result;
    }

    // ─── Serialization (Week 6) ───────────────────────────────────────────────
    private void saveUsers() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(USERS_FILE))) {
            oos.writeObject(users);
        } catch (IOException e) {
            System.out.println("User save error: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private List<User> loadUsers() {
        File f = new File(USERS_FILE);
        if (!f.exists()) return new ArrayList<>();
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(USERS_FILE))) {
            return (List<User>) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            return new ArrayList<>();
        }
    }
}
