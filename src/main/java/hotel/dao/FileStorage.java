package hotel.dao;

import hotel.model.Bill;
import hotel.model.Room;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import java.util.Properties;
import java.util.Objects;

/**
 * Enterprise Storage Manager for persistence and system auditing.
 */
public class FileStorage {

    private static final String CONFIG_FILE = "/config.properties";
    private static final Properties props = new Properties();
    
    private static String DATA_DIR;
    private static String ROOMS_FILE;
    private static String BILLS_FILE;
    private static String USERS_FILE;
    private static String BOOKINGS_FILE;
    private static String LOG_FILE;
    private static int MAX_LOG_LINES;

    static {
        try (InputStream is = FileStorage.class.getResourceAsStream(CONFIG_FILE)) {
            props.load(Objects.requireNonNull(is, "Config file not found: " + CONFIG_FILE));
            
            // Base data directory
            DATA_DIR = System.getProperty("user.home") + "/HMS_Data/";
            Files.createDirectories(Paths.get(DATA_DIR));

            ROOMS_FILE = DATA_DIR + props.getProperty("rooms.path", "rooms.dat");
            BILLS_FILE = DATA_DIR + props.getProperty("bills.path", "bills.dat");
            USERS_FILE = DATA_DIR + props.getProperty("users.path", "users.dat");
            BOOKINGS_FILE = DATA_DIR + props.getProperty("bookings.path", "bookings.dat");
            LOG_FILE   = DATA_DIR + props.getProperty("logs.path", "activity.log");
            MAX_LOG_LINES = Integer.parseInt(props.getProperty("max.log.lines", "10000"));

            // Ensure parent directories for all files exist
            Files.createDirectories(Paths.get(ROOMS_FILE).getParent());
            Files.createDirectories(Paths.get(BILLS_FILE).getParent());
            Files.createDirectories(Paths.get(USERS_FILE).getParent());
            Files.createDirectories(Paths.get(BOOKINGS_FILE).getParent());
            Files.createDirectories(Paths.get(LOG_FILE).getParent());

        } catch (Exception e) {
            System.err.println("CRITICAL: HMS Configuration failure. Using defaults.");
            e.printStackTrace();
            DATA_DIR = System.getProperty("user.home") + "/HMS_Data/";
            ROOMS_FILE = DATA_DIR + "rooms.dat";
            BILLS_FILE = DATA_DIR + "bills.dat";
            USERS_FILE = DATA_DIR + "users.dat";
            LOG_FILE = DATA_DIR + "activity.log";
            MAX_LOG_LINES = 10000;
        }
    }

    public static String getUsersFile() { return USERS_FILE; }


    public static void saveRooms(List<Room> rooms) {
        Path tmp = Paths.get(ROOMS_FILE + ".tmp");
        Path real = Paths.get(ROOMS_FILE);
        try (ObjectOutputStream oos = new ObjectOutputStream(
                new BufferedOutputStream(Files.newOutputStream(tmp)))) {
            oos.writeObject(rooms);
            Files.move(tmp, real, java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                                  java.nio.file.StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            writeLog("Persistence Error [Rooms]", e);
            try { Files.deleteIfExists(tmp); } catch (IOException ignored) {}
        }
    }

    @SuppressWarnings("unchecked")
    public static List<Room> loadRooms() {
        if (!Files.exists(Paths.get(ROOMS_FILE))) return new ArrayList<>();
        try (ObjectInputStream ois = new ObjectInputStream(
                new BufferedInputStream(new FileInputStream(ROOMS_FILE)))) {
            return (List<Room>) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            writeLog("Persistence Error [LoadRooms]", e);
            return new ArrayList<>();
        }
    }

    public static void saveBill(Bill bill) {
        List<Bill> bills = loadBills();  // loads existing
        bills.add(bill);
        saveBills(bills);
    }

    public static void saveBills(List<Bill> bills) {
        Path tmp = Paths.get(BILLS_FILE + ".tmp");
        Path real = Paths.get(BILLS_FILE);
        try (ObjectOutputStream oos = new ObjectOutputStream(
                new BufferedOutputStream(Files.newOutputStream(tmp)))) {
            oos.writeObject(bills);
            // atomic move: either the full file is replaced or nothing changes
            Files.move(tmp, real, java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                                  java.nio.file.StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            writeLog("Persistence Error [SaveBills]", e);
            try { Files.deleteIfExists(tmp); } catch (IOException ignored) {}
        }
    }


    public static void saveBookings(List<hotel.model.Booking> bookings) {
        Path tmp = Paths.get(BOOKINGS_FILE + ".tmp");
        Path real = Paths.get(BOOKINGS_FILE);
        try (ObjectOutputStream oos = new ObjectOutputStream(
                new BufferedOutputStream(Files.newOutputStream(tmp)))) {
            oos.writeObject(bookings);
            Files.move(tmp, real, java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                                  java.nio.file.StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            writeLog("Persistence Error [Bookings]", e);
            try { Files.deleteIfExists(tmp); } catch (IOException ignored) {}
        }
    }

    @SuppressWarnings("unchecked")
    public static List<hotel.model.Booking> loadBookings() {
        if (!Files.exists(Paths.get(BOOKINGS_FILE))) return new ArrayList<>();
        try (ObjectInputStream ois = new ObjectInputStream(
                new BufferedInputStream(new FileInputStream(BOOKINGS_FILE)))) {
            return (List<hotel.model.Booking>) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            writeLog("Persistence Error [LoadBookings]", e);
            return new ArrayList<>();
        }
    }


    @SuppressWarnings("unchecked")
    public static List<Bill> loadBills() {
        if (!Files.exists(Paths.get(BILLS_FILE))) return new ArrayList<>();
        try (ObjectInputStream ois = new ObjectInputStream(
                new BufferedInputStream(new FileInputStream(BILLS_FILE)))) {
            return (List<Bill>) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Load Error [Bills]: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public static void writeLog(String message) {
        writeLog(message, null);
    }

    public static void writeLog(String message, Throwable throwable) {
        // Optimized Log Rotation check: uses file size instead of reading all lines
        try {
            Path logPath = Paths.get(LOG_FILE);
            if (Files.exists(logPath) && Files.size(logPath) > 5 * 1024 * 1024) { // Rotate at ~5MB
                List<String> lines = Files.readAllLines(logPath);
                if (lines.size() >= MAX_LOG_LINES) {
                    List<String> truncated = new ArrayList<>();
                    truncated.add("[LOG] System statistics reset due to rotation. Continuing log.");
                    truncated.addAll(lines.subList(lines.size() - 2000, lines.size()));
                    Files.write(logPath, truncated);
                }
            }
        } catch (IOException e) {
            System.err.println("Log Rotation failed: " + e.getMessage());
        }

        try (PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(LOG_FILE, true)))) {
            pw.printf("[%tF %<tT] %s%n", System.currentTimeMillis(), message);
            if (throwable != null) {
                pw.println("STACK TRACE:");
                throwable.printStackTrace(pw);
                pw.println("--------------------------------------------------");
            }
        } catch (IOException e) {
            System.err.println("FATAL: Logging failed: " + e.getMessage());
        }
    }

    public static String readLog() {
        if (!Files.exists(Paths.get(LOG_FILE))) return "No activity log found.";
        try {
            return Files.readString(Paths.get(LOG_FILE));
        } catch (IOException e) {
            return "Error reading log: " + e.getMessage();
        }
    }

    public static void clearBills() {
        try { Files.deleteIfExists(Paths.get(BILLS_FILE)); } catch (IOException ignored) {}
    }

    public static void clearLog() {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(LOG_FILE, false))) {
            bw.write("[LOG] System statistics reset. Starting fresh.\n");
        } catch (IOException e) {
            System.err.println("Log clear error: " + e.getMessage());
        }
    }
}

