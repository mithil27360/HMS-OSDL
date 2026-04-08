package hotel.dao;

import hotel.model.Bill;
import hotel.model.Room;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * File-based persistence using modern Java NIO and Buffered I/O.
 * Data is stored in the user's home directory for portability and security.
 */
public class FileStorage {

    private static final String DATA_DIR = System.getProperty("user.home") + "/HMS_Data/";
    private static final String ROOMS_FILE = DATA_DIR + "hotel_rooms.dat";
    private static final String BILLS_FILE = DATA_DIR + "hotel_bills.dat";
    private static final String LOG_FILE = DATA_DIR + "hotel_activity.log";

    static {
        try {
            Files.createDirectories(Paths.get(DATA_DIR));
        } catch (IOException e) {
            System.err.println("Could not create data directory: " + e.getMessage());
        }
    }

    // ─── ROOM PERSISTENCE ────────────────────────────────────────────────────
    public static void saveRooms(List<Room> rooms) {
        try (ObjectOutputStream oos = new ObjectOutputStream(
                new BufferedOutputStream(new FileOutputStream(ROOMS_FILE)))) {
            oos.writeObject(rooms);
        } catch (IOException e) {
            System.err.println("Serialization error: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public static List<Room> loadRooms() {
        if (!Files.exists(Paths.get(ROOMS_FILE))) return new ArrayList<>();
        try (ObjectInputStream ois = new ObjectInputStream(
                new BufferedInputStream(new FileInputStream(ROOMS_FILE)))) {
            return (List<Room>) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Deserialization error: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    // ─── BILL PERSISTENCE ────────────────────────────────────────────────────
    public static void saveBill(Bill bill) {
        List<Bill> bills = loadBills();
        bills.add(bill);
        try (ObjectOutputStream oos = new ObjectOutputStream(
                new BufferedOutputStream(new FileOutputStream(BILLS_FILE)))) {
            oos.writeObject(bills);
        } catch (IOException e) {
            System.err.println("Bill save error: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public static List<Bill> loadBills() {
        if (!Files.exists(Paths.get(BILLS_FILE))) return new ArrayList<>();
        try (ObjectInputStream ois = new ObjectInputStream(
                new BufferedInputStream(new FileInputStream(BILLS_FILE)))) {
            return (List<Bill>) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            return new ArrayList<>();
        }
    }

    // ─── TEXT LOG ────────────────────────────────────────────────────────────
    public static void writeLog(String message) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(LOG_FILE, true))) {
            bw.write(String.format("[%tF %<tT] %s%n", System.currentTimeMillis(), message));
        } catch (IOException e) {
            System.err.println("Log write error: " + e.getMessage());
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

    /**
     * Copy log using modern transferTo for high performance.
     */
    public static void copyLogToBackup() {
        Path source = Paths.get(LOG_FILE);
        Path target = Paths.get(DATA_DIR + "hotel_activity_backup.log");
        if (!Files.exists(source)) return;
        
        try (InputStream is = Files.newInputStream(source);
             OutputStream os = Files.newOutputStream(target)) {
            is.transferTo(os);
        } catch (IOException e) {
            System.err.println("Backup error: " + e.getMessage());
        }
    }

    public static void clearBills() {
        try {
            Files.deleteIfExists(Paths.get(BILLS_FILE));
        } catch (IOException e) {
            System.err.println("Error clearing bills: " + e.getMessage());
        }
    }

    public static void clearLog() {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(LOG_FILE, false))) {
            bw.write("[LOG] System statistics reset. Starting fresh.\n");
        } catch (IOException e) {
            System.err.println("Log clear error: " + e.getMessage());
        }
    }
}
