package hotel.dao;

import hotel.exception.AuthException;
import hotel.exception.UserNotFoundException;
import hotel.model.User;
import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Professional implementation of user authentication and management service.
 */
public class AuthService implements IAuthService {

    private final String USERS_FILE = FileStorage.getUsersFile();
    private List<User> users;
    private volatile User currentUser;

    private static final Set<String> USERS_ALLOWED_CLASSES = new HashSet<>(Set.of(
        "java.util.ArrayList",
        "java.lang.String",
        "java.lang.Boolean",
        "java.lang.Object",
        "[Ljava.lang.Object;",
        "hotel.model.Person",
        "hotel.model.User",
        "hotel.model.User$Role"
    ));

    private AuthService() {
        users = new java.util.concurrent.CopyOnWriteArrayList<>(loadUsers());
        if (users.isEmpty()) seedDefaultUsers();
    }


    private static class Holder {
        private static final AuthService INSTANCE = new AuthService();
    }

    public static AuthService getInstance() {
        return Holder.INSTANCE;
    }

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
            throw new RuntimeException("Critical security error: Hashing algorithm not found", e);
        }
    }

    private void seedDefaultUsers() {
        users.clear();
        users.add(new User("admin",        hashPassword("admin123"),        "Tony Stark",     "stark@avengers.com",     User.Role.ADMIN));
        users.add(new User("staff",         hashPassword("staff123"),        "Harry Potter",    "h.potter@hogwarts.com",  User.Role.RECEPTIONIST));
        users.add(new User("guest",        hashPassword("guest123"),        "James Bond",      "007@mi6.gov.uk",         User.Role.GUEST));
        saveUsers();
    }

    @Override
    public synchronized User login(String username, String password) throws AuthException, UserNotFoundException {
        String hashedInput = hashPassword(password);
        
        User user;
        synchronized (users) {
            user = users.stream()
                    .filter(u -> u.getUsername().equalsIgnoreCase(username))
                    .findFirst()
                    .orElseThrow(() -> new UserNotFoundException(username));
        }

        if (!user.isActive()) {
            throw new AuthException("Account is deactivated.");
        }

        if (user.getPassword().equals(hashedInput)) {
            currentUser = user;
            FileStorage.writeLog("Login: " + user.getFullName() + " [" + user.getRole() + "]");
            return user;
        } else {
            throw new AuthException("Invalid password for user: " + username);
        }
    }

    @Override
    public synchronized void logout() {
        if (currentUser != null) {
            FileStorage.writeLog("Logout: " + currentUser.getFullName());
        }
        currentUser = null;
    }

    @Override
    public synchronized User getCurrentUser() { return currentUser; }

    @Override
    public boolean addUser(User user) {
        synchronized (users) {
            boolean exists = users.stream().anyMatch(u -> u.getUsername().equalsIgnoreCase(user.getUsername()));
            if (exists) return false;

            user.setPassword(hashPassword(user.getPassword()));
            users.add(user);
            saveUsers();
            FileStorage.writeLog("User added: " + user.getUsername() + " [" + user.getRole() + "]");
            return true;
        }
    }

    @Override
    public boolean deleteUser(String username) {
        synchronized (users) {
            boolean removed = users.removeIf(u -> u.getUsername().equals(username) && !u.getUsername().equals("admin"));
            if (removed) {
                saveUsers();
                FileStorage.writeLog("User deleted: " + username);
            }
            return removed;
        }
    }

    @Override
    public List<User> getAllUsers() { return new ArrayList<>(users); }

    @Override
    public List<User> getUsersByRole(User.Role role) {
        // Filter by role

        return users.stream()
                .filter(u -> u.getRole() == role)
                .collect(Collectors.toList());
    }

    private void saveUsers() {
        java.nio.file.Path tmp = java.nio.file.Paths.get(USERS_FILE + ".tmp");
        java.nio.file.Path real = java.nio.file.Paths.get(USERS_FILE);
        try (ObjectOutputStream oos = new ObjectOutputStream(
                new BufferedOutputStream(java.nio.file.Files.newOutputStream(tmp)))) {
            oos.writeObject(new ArrayList<>(users));
            java.nio.file.Files.move(tmp, real, java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                                          java.nio.file.StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            FileStorage.writeLog("User persistence error: " + e.getMessage());
            try { java.nio.file.Files.deleteIfExists(tmp); } catch (IOException ignored) {}
        }
    }

    private List<User> loadUsers() {
        File f = new File(USERS_FILE);
        if (!f.exists()) return new ArrayList<>();
        try (ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(new FileInputStream(USERS_FILE)))) {
            ois.setObjectInputFilter(info -> {
                if (info.depth() > 24 || info.references() > 20_000 || info.arrayLength() > 50_000) {
                    return ObjectInputFilter.Status.REJECTED;
                }

                Class<?> clazz = info.serialClass();
                if (clazz == null) {
                    return ObjectInputFilter.Status.UNDECIDED;
                }
                if (clazz.isPrimitive()) {
                    return ObjectInputFilter.Status.ALLOWED;
                }
                if (clazz.isArray()) {
                    Class<?> component = clazz;
                    while (component.isArray()) {
                        component = component.getComponentType();
                    }
                    if (component.isPrimitive()) {
                        return ObjectInputFilter.Status.ALLOWED;
                    }
                    return USERS_ALLOWED_CLASSES.contains(component.getName())
                        ? ObjectInputFilter.Status.ALLOWED
                        : ObjectInputFilter.Status.REJECTED;
                }

                return USERS_ALLOWED_CLASSES.contains(clazz.getName())
                    ? ObjectInputFilter.Status.ALLOWED
                    : ObjectInputFilter.Status.REJECTED;
            });

            Object data = ois.readObject();
            if (!(data instanceof List<?> rawUsers)) {
                return new ArrayList<>();
            }

            List<User> validated = new ArrayList<>(rawUsers.size());
            for (Object item : rawUsers) {
                if (!(item instanceof User)) {
                    throw new InvalidObjectException("Unexpected user payload type");
                }
                validated.add((User) item);
            }
            return validated;
        } catch (IOException | ClassNotFoundException | SecurityException e) {
            FileStorage.writeLog("User load security/persistence error", e);
            return new ArrayList<>();
        }
    }
}
