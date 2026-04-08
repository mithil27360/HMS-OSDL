package hotel.dao;

import hotel.model.User;
import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

/**
 * Authentication Service — manages users, login, and secure persistence.
 * Features: SHA-256 Hashing, Thread-Safe Singleton, and Local Serialization.
 */
public class AuthService {

    private static final String USERS_FILE = "hotel_users.dat";
    private List<User> users;
    private User currentUser;

    // ─── Thread-Safe Singleton (Static Holder Pattern) ───────────────────────
    private AuthService() {
        users = loadUsers();
        if (users.isEmpty()) seedDefaultUsers();
    }

    private static class Holder {
        private static final AuthService INSTANCE = new AuthService();
    }

    public static AuthService getInstance() {
        return Holder.INSTANCE;
    }

    // ─── Password Hashing (SHA-256) ──────────────────────────────────────────
    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Hashing algorithm not found", e);
        }
    }

    // ─── Seed default accounts ────────────────────────────────────────────────
    private void seedDefaultUsers() {
        users.clear(); // Ensure fresh start for hashing update
        users.add(new User("admin",        hashPassword("admin123"),  "Admin Manager",     "admin@hms.com",  User.Role.ADMIN));
        users.add(new User("receptionist", hashPassword("staff123"),  "Priya Sharma",      "priya@hms.com",  User.Role.RECEPTIONIST));
        users.add(new User("guest",        hashPassword("guest123"),  "Rahul Kumar",       "rahul@email.com",       User.Role.GUEST));
        saveUsers();
    }

    // ─── Login ────────────────────────────────────────────────────────────────
    public User login(String username, String password) {
        String hashedInput = hashPassword(password);
        for (User u : users) {
            if (u.getUsername().equals(username) &&
                u.getPassword().equals(hashedInput) &&
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
            if (u.getUsername().equals(user.getUsername())) return false; 
        }
        // Hash password if adding new user
        user.setPassword(hashPassword(user.getPassword()));
        users.add(user);
        saveUsers();
        FileStorage.writeLog("User added: " + user.getUsername() + " [" + user.getRole() + "]");
        return true;
    }

    public boolean deleteUser(String username) {
        boolean removed = users.removeIf(u -> u.getUsername().equals(username) && !u.getUsername().equals("admin"));
        if (removed) {
            saveUsers(); // Fix: Persist deletion to disk
            FileStorage.writeLog("User deleted: " + username);
        }
        return removed;
    }

    public List<User> getAllUsers() { return new ArrayList<>(users); }

    public List<User> getUsersByRole(User.Role role) {
        List<User> result = new ArrayList<>();
        for (User u : users) if (u.getRole() == role) result.add(u);
        return result;
    }

    // ─── Serialization ────────────────────────────────────────────────────────
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
