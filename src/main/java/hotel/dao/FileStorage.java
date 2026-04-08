package hotel.dao;

import hotel.model.Bill;
import hotel.model.Room;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * File-based persistence using Serialization/Deserialization (Week 6).
 * Permanent storage of data in files (Rubric requirement).
 */
public class FileStorage {

    private static final String ROOMS_FILE = "hotel_rooms.dat";
    private static final String BILLS_FILE = "hotel_bills.dat";

    // ─── ROOM PERSISTENCE ────────────────────────────────────────────────────

    /**
     * Serialize all rooms to file (Week 6 - Serialization)
     */
    public static void saveRooms(List<Room> rooms) {
        try (FileOutputStream fos = new FileOutputStream(ROOMS_FILE);
             ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            oos.writeObject(rooms);
            System.out.println("Rooms serialized successfully.");
        } catch (IOException e) {
            System.out.println("Serialization error: " + e.getMessage());
        }
    }

    /**
     * Deserialize rooms from file (Week 6 - Deserialization)
     */
    @SuppressWarnings("unchecked")
    public static List<Room> loadRooms() {
        File file = new File(ROOMS_FILE);
        if (!file.exists()) return new ArrayList<>();

        try (FileInputStream fis = new FileInputStream(ROOMS_FILE);
             ObjectInputStream ois = new ObjectInputStream(fis)) {
            List<Room> rooms = (List<Room>) ois.readObject();
            System.out.println("Rooms deserialized successfully.");
            return rooms;
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Deserialization error: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    // ─── BILL PERSISTENCE ────────────────────────────────────────────────────

    /**
     * Serialize bill to file (Week 6 - Serialization)
     */
    @SuppressWarnings("unchecked")
    public static void saveBill(Bill bill) {
        List<Bill> bills = loadBills();
        bills.add(bill);
        try (FileOutputStream fos = new FileOutputStream(BILLS_FILE);
             ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            oos.writeObject(bills);
        } catch (IOException e) {
            System.out.println("Bill save error: " + e.getMessage());
        }
    }

    /**
     * Deserialize all bills from file
     */
    @SuppressWarnings("unchecked")
    public static List<Bill> loadBills() {
        File file = new File(BILLS_FILE);
        if (!file.exists()) return new ArrayList<>();

        try (FileInputStream fis = new FileInputStream(BILLS_FILE);
             ObjectInputStream ois = new ObjectInputStream(fis)) {
            return (List<Bill>) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            return new ArrayList<>();
        }
    }

    // ─── TEXT LOG (FileWriter / FileReader - Week 5) ─────────────────────────

    /**
     * Write activity log using FileWriter (Week 5 - Character Streams)
     */
    public static void writeLog(String message) {
        try (FileWriter fw = new FileWriter("hotel_activity.log", true)) {
            fw.write("[LOG] " + message + "\n");
        } catch (IOException e) {
            System.out.println("Log write error: " + e.getMessage());
        }
    }

    /**
     * Read activity log using FileReader (Week 5 - Character Streams)
     */
    public static String readLog() {
        StringBuilder sb = new StringBuilder();
        File file = new File("hotel_activity.log");
        if (!file.exists()) return "No activity log found.";

        try (FileReader fr = new FileReader("hotel_activity.log")) {
            int ch;
            while ((ch = fr.read()) != -1) {
                sb.append((char) ch);
            }
        } catch (IOException e) {
            return "Error reading log: " + e.getMessage();
        }
        return sb.toString();
    }

    /**
     * Copy log using byte streams (Week 5 - Byte Streams)
     */
    public static void copyLogToBackup() {
        try (FileInputStream fis = new FileInputStream("hotel_activity.log");
             FileOutputStream fos = new FileOutputStream("hotel_activity_backup.log")) {
            int data;
            while ((data = fis.read()) != -1) {
                fos.write(data);
            }
            System.out.println("Log backed up successfully.");
        } catch (IOException e) {
            System.out.println("Backup error: " + e.getMessage());
        }
    }

    /**
     * Delete bill file to reset earnings (Week 6)
     */
    public static void clearBills() {
        File file = new File(BILLS_FILE);
        if (file.exists()) file.delete();
    }

    /**
     * Truncate activity log (Week 5)
     */
    public static void clearLog() {
        try (FileWriter fw = new FileWriter("hotel_activity.log", false)) {
            fw.write("[LOG] System statistics reset. Starting fresh.\n");
        } catch (IOException e) {
            System.out.println("Log clear error: " + e.getMessage());
        }
    }
}
