package hotel.dao;

import hotel.model.Bill;
import hotel.model.Room;
import hotel.util.GenericUtils;
import hotel.util.Pair;

import java.util.*;

/**
 * Hotel Service layer demonstrating:
 * - Collections Framework: ArrayList, HashMap, Iterator (Week 8)
 * - Synchronization (Week 4) - thread-safe booking
 * - Generics (Week 7)
 * - Wrapper classes + Autoboxing (Week 2)
 */
public class HotelService {

    // Collections Framework (Week 8)
    private ArrayList<Room> rooms;              // ArrayList for room objects
    private HashMap<Integer, Room> roomMap;     // HashMap: roomNumber -> Room
    private ArrayList<Bill> bills;              // ArrayList for bills
    private ArrayList<Pair<Integer, String>> bookingLog; // Generic Pair (Week 7)

    private int billCounter = 1;

    public HotelService() {
        // Load persisted data
        rooms = new ArrayList<>(FileStorage.loadRooms());
        roomMap = new HashMap<>();
        bills = new ArrayList<>(FileStorage.loadBills());
        bookingLog = new ArrayList<>();

        // Rebuild HashMap from list
        for (Room r : rooms) {
            roomMap.put(r.getRoomNumber(), r);
        }

        // If no data, seed default rooms
        if (rooms.isEmpty()) {
            seedDefaultRooms();
        }

        // Update bill counter
        billCounter = bills.size() + 1;

        FileStorage.writeLog("System initialized. Rooms loaded: " + rooms.size());
    }

    // ─── SEED DATA ───────────────────────────────────────────────────────────

    private void seedDefaultRooms() {
        addRoom(new Room(101, Room.RoomType.STANDARD));
        addRoom(new Room(102, Room.RoomType.STANDARD));
        addRoom(new Room(201, Room.RoomType.DOUBLE));
        addRoom(new Room(202, Room.RoomType.DOUBLE));
        addRoom(new Room(301, Room.RoomType.DELUXE));
        addRoom(new Room(302, Room.RoomType.DELUXE));
        addRoom(new Room(401, Room.RoomType.SUITE));
        addRoom(new Room(402, Room.RoomType.SUITE));
        saveData();
    }

    // ─── ROOM MANAGEMENT ─────────────────────────────────────────────────────

    public synchronized void addRoom(Room room) {
        if (!roomMap.containsKey(room.getRoomNumber())) {
            rooms.add(room);
            roomMap.put(room.getRoomNumber(), room);
            FileStorage.writeLog("Room added: " + room.getRoomNumber());
        }
    }

    public synchronized boolean bookRoom(int roomNumber, String guestName,
                                          String contact, int days,
                                          String checkIn, String checkOut) {
        Room room = roomMap.get(roomNumber);
        if (room == null) return false;

        synchronized (room) {
            if (room.isBooked()) return false;

            room.setBooked(true);
            room.setGuestName(guestName);
            room.setGuestContact(contact);
            room.setDaysBooked(days);
            room.setCheckInDate(checkIn);
            room.setCheckOutDate(checkOut);

            // Autoboxing: int -> Integer (Week 2)
            Integer roomNum = roomNumber;
            bookingLog.add(new Pair<>(roomNum, guestName)); // Generic Pair (Week 7)

            FileStorage.writeLog("Room " + roomNumber + " booked by " + guestName);
            saveData();
            return true;
        }
    }

    public synchronized Bill checkoutRoom(int roomNumber) {
        Room room = roomMap.get(roomNumber);
        if (room == null || !room.isBooked()) return null;

        // Wrapper class usage (Week 2)
        Integer days = room.getDaysBooked();  // Autoboxing

        // Create bill using unboxed values
        Bill bill = new Bill(billCounter++, room, days);
        bills.add(bill);
        FileStorage.saveBill(bill);

        // Generic display (Week 7)
        GenericUtils.display("Checkout - Room " + roomNumber + " | Total: ₹" + bill.getTotalAmount());

        room.setBooked(false);
        room.setGuestName("");
        room.setGuestContact("");
        room.setDaysBooked(0);
        room.setCheckInDate("");
        room.setCheckOutDate("");

        FileStorage.writeLog("Room " + roomNumber + " checked out. Bill: ₹" + bill.getTotalAmount());
        saveData();
        return bill;
    }

    // ─── QUERIES ─────────────────────────────────────────────────────────────

    public ArrayList<Room> getAllRooms() {
        return rooms;
    }

    public ArrayList<Room> getAvailableRooms() {
        ArrayList<Room> available = new ArrayList<>();
        // Iterator usage (Week 8)
        Iterator<Room> it = rooms.iterator();
        while (it.hasNext()) {
            Room r = it.next();
            if (!r.isBooked()) available.add(r);
        }
        return available;
    }

    public ArrayList<Room> getBookedRooms() {
        ArrayList<Room> booked = new ArrayList<>();
        for (Room r : rooms) {
            if (r.isBooked()) booked.add(r);
        }
        return booked;
    }

    public Room getRoomByNumber(int number) {
        return roomMap.get(number);
    }

    public ArrayList<Bill> getAllBills() {
        return bills;
    }

    public ArrayList<Room> getRoomsByType(Room.RoomType type) {
        ArrayList<Room> result = new ArrayList<>();
        for (Room r : rooms) {
            if (r.getRoomType() == type) result.add(r);
        }
        return result;
    }

    public ArrayList<Room> getRoomsSortedByPrice() {
        ArrayList<Room> sorted = new ArrayList<>(rooms);
        Collections.sort(sorted, Comparator.comparingDouble(Room::getPricePerNight));
        return sorted;
    }

    public ArrayList<Room> getRoomsSortedByNumber() {
        ArrayList<Room> sorted = new ArrayList<>(rooms);
        Collections.sort(sorted, Comparator.comparingInt(Room::getRoomNumber));
        return sorted;
    }

    public int getTotalRooms() { return rooms.size(); }
    public int getAvailableCount() { return getAvailableRooms().size(); }
    public int getOccupiedCount() { return getBookedRooms().size(); }

    public double getTotalRevenue() {
        Double total = 0.0;
        for (Bill b : bills) {
            total += b.getTotalAmount();
        }
        return total;
    }

    public double getDiscountedPrice(double price, double discountPercent) {
        return GenericUtils.calculateDiscountedPrice(price, discountPercent);
    }

    public boolean deleteRoom(int roomNumber) {
        Room room = roomMap.get(roomNumber);
        if (room == null || room.isBooked()) return false;
        rooms.remove(room);
        roomMap.remove(roomNumber);
        saveData();
        FileStorage.writeLog("Room " + roomNumber + " removed.");
        return true;
    }

    public synchronized void resetSystemData() {
        FileStorage.clearBills();
        FileStorage.clearLog();
        bills.clear();
        bookingLog.clear();
        billCounter = 1;
        // Also reset all rooms to available
        for (Room r : rooms) {
            r.setBooked(false);
            r.setGuestName("");
            r.setGuestContact("");
            r.setDaysBooked(0);
            r.setCheckInDate("");
            r.setCheckOutDate("");
        }
        saveData();
        FileStorage.writeLog("System hard reset performed by administrator.");
    }

    private void saveData() {
        FileStorage.saveRooms(rooms);
    }

    public String getActivityLog() {
        return FileStorage.readLog();
    }

    public List<Pair<Integer, String>> getBookingLog() {
        return bookingLog;
    }
}
