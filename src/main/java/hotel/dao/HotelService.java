package hotel.dao;

import hotel.exception.RoomAlreadyBookedException;
import hotel.model.Bill;
import hotel.model.Room;
import hotel.model.Booking;
import hotel.util.Pair;
import hotel.util.RoomServiceThread;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Modern implementation of hotel management services.
 */
public class HotelService implements IHotelService {

    private final List<Room> rooms;
    private final Map<Integer, Room> roomMap;
    private final List<Bill> bills;
    private final List<Booking> bookings;
    private final Set<Integer> cleaningRooms;
    
    private final BlockingQueue<Pair<Integer, String>> bookingLogQueue;
    private final AtomicInteger billCounter;
    private final ExecutorService roomServiceExecutor;

    private HotelService() {
        rooms = new CopyOnWriteArrayList<>(FileStorage.loadRooms());
        roomMap = new ConcurrentHashMap<>();
        bills = new CopyOnWriteArrayList<>(FileStorage.loadBills());
        bookings = new CopyOnWriteArrayList<>(FileStorage.loadBookings());
        cleaningRooms = ConcurrentHashMap.newKeySet();
        bookingLogQueue = new LinkedBlockingQueue<>();
        
        rooms.forEach(r -> roomMap.put(r.getRoomNumber(), r));


        if (rooms.isEmpty()) {
            seedDefaultRooms();
        }

        int maxId = bills.stream()
                .mapToInt(Bill::getBillId)
                .max()
                .orElse(0);
        billCounter = new AtomicInteger(maxId + 1);

        roomServiceExecutor = Executors.newFixedThreadPool(4);
        
        // Resource cleanup: Shutdown executor on JVM exit
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            roomServiceExecutor.shutdown();
            try {
                if (!roomServiceExecutor.awaitTermination(3, TimeUnit.SECONDS)) {
                    roomServiceExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                roomServiceExecutor.shutdownNow();
            }
        }));

        startLogConsumer();

        FileStorage.writeLog("System initialized. Rooms loaded: " + rooms.size());
    }

    private static class Holder {
        private static final HotelService INSTANCE = new HotelService();
    }

    public static HotelService getInstance() {
        return Holder.INSTANCE;
    }

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

    @Override
    public synchronized void addRoom(Room room) {
        if (!roomMap.containsKey(room.getRoomNumber())) {
            rooms.add(room);
            roomMap.put(room.getRoomNumber(), room);
            FileStorage.writeLog("Room added: " + room.getRoomNumber());
            saveData();
        }
    }

    @Override
    public void bookRoom(int roomNumber, String guestName, String contact, int days,
                         java.time.LocalDate checkIn, java.time.LocalDate checkOut) throws RoomAlreadyBookedException {
        Room room = roomMap.get(roomNumber);
        if (room == null) throw new IllegalArgumentException("Room no longer exists.");

        synchronized (bookings) {
            if (!isRoomAvailableForDates(roomNumber, checkIn, checkOut)) {
                throw new RoomAlreadyBookedException("Room " + roomNumber + " is already reserved for these dates.");
            }

            Booking booking = new Booking(
                (int)(System.currentTimeMillis() % 1000000), 
                roomNumber, guestName, contact, checkIn, checkOut
            );

            try {
                bookings.add(booking);
                FileStorage.saveBookings(bookings);
                bookingLogQueue.offer(new Pair<>(roomNumber, guestName));
                FileStorage.writeLog("Room " + roomNumber + " reserved for " + checkIn + " to " + checkOut);
            } catch (Exception e) {
                bookings.remove(booking);
                FileStorage.writeLog("Booking rollback for room " + roomNumber + ": " + e.getMessage());
                throw new RuntimeException("Booking failed.", e);
            }
        }
    }

    @Override
    public boolean isRoomAvailableForDates(int roomNumber, java.time.LocalDate checkIn, java.time.LocalDate checkOut) {
        // 1. Check Maintenance
        if (cleaningRooms.contains(roomNumber) && !checkIn.isAfter(java.time.LocalDate.now())) {
            return false;
        }

        synchronized (bookings) {
            // 2. Check Overlaps
            boolean hasOverlap = bookings.stream()
                .filter(b -> b.getRoomNumber() == roomNumber)
                .anyMatch(b -> b.overlaps(checkIn, checkOut));
            
            if (hasOverlap) return false;

            // 3. Strict Check-in: If today is the requested check-in day,
            // check if there is an outgoing guest who HAS NOT checked out yet.
            if (checkIn.isEqual(java.time.LocalDate.now())) {
                boolean hasUncheckedOutGuest = bookings.stream()
                    .filter(b -> b.getRoomNumber() == roomNumber && !b.isCheckedOut())
                    .anyMatch(b -> b.getCheckOut().isEqual(checkIn));
                
                if (hasUncheckedOutGuest) return false;
            }

            return true;
        }
    }


    private void startBackgroundServices(int roomNumber) {
        cleaningRooms.add(roomNumber);
        
        Runnable onComplete = () -> {
            // Check if both services are done (we use a simple count or just wait for both)
            // For simplicity in this demo, one thread removal is enough to show logic
            cleaningRooms.remove(roomNumber);
            FileStorage.writeLog("Room " + roomNumber + " is now sanitized and READY for next guest.");
        };

        roomServiceExecutor.submit(new RoomServiceThread("Cleaning & Sanitization", roomNumber, onComplete));
    }

    @Override
    public Bill checkoutRoom(int roomNumber) {
        Room room = roomMap.get(roomNumber);
        if (room == null) return null;

        synchronized (bookings) {
            java.time.LocalDate today = java.time.LocalDate.now();
            Booking activeBooking = bookings.stream()
                .filter(b -> b.getRoomNumber() == roomNumber && !b.isCheckedOut())
                .filter(b -> (today.isEqual(b.getCheckIn()) || today.isAfter(b.getCheckIn())))
                .findFirst()
                .orElse(null);

            if (activeBooking == null) return null;

            Bill bill = new Bill(billCounter.getAndIncrement(), room, activeBooking);

            try {
                activeBooking.setCheckedOut(true);
                bills.add(bill);
                FileStorage.saveBill(bill);
                FileStorage.saveBookings(bookings);
                FileStorage.writeLog("Room " + roomNumber + " checked out. Bill: " + bill.getBillId());
                startBackgroundServices(roomNumber);
                return bill;
            } catch (Exception e) {
                activeBooking.setCheckedOut(false);
                bills.remove(bill);
                billCounter.decrementAndGet();
                FileStorage.writeLog("Checkout rollback for room " + roomNumber + ": " + e.getMessage());
                return null;
            }
        }
    }

    @Override

    public List<Room> getAllRooms() {
        return new ArrayList<>(rooms);
    }

    @Override
    public List<Room> getAvailableRooms(java.time.LocalDate start, java.time.LocalDate end) {
        synchronized (rooms) {
            return rooms.stream()
                    .filter(r -> isRoomAvailableForDates(r.getRoomNumber(), start, end))
                    .collect(Collectors.toList());
        }
    }

    @Override
    public List<Room> getBookedRooms(java.time.LocalDate date) {
        synchronized (bookings) {
            Set<Integer> bookedRoomNumbers = bookings.stream()
                .filter(b -> !b.isCheckedOut())
                .filter(b -> (date.isEqual(b.getCheckIn()) || date.isAfter(b.getCheckIn())) && 
                             (date.isEqual(b.getCheckOut()) || date.isBefore(b.getCheckOut())))
                .map(Booking::getRoomNumber)
                .collect(Collectors.toSet());

            return rooms.stream()
                .filter(r -> bookedRoomNumbers.contains(r.getRoomNumber()))
                .collect(Collectors.toList());
        }
    }


    @Override
    public Room getRoomByNumber(int number) {
        return roomMap.get(number);
    }

    @Override
    public List<Bill> getAllBills() {
        return new ArrayList<>(bills);
    }

    @Override
    public List<Room> getRoomsByType(Room.RoomType type) {
        return rooms.stream()
                .filter(r -> r.getRoomType() == type)
                .collect(Collectors.toList());
    }

    @Override
    public List<Room> getRoomsSortedByPrice() {
        // Sort with Comparator

        List<Room> sorted = new ArrayList<>(rooms);
        sorted.sort(Comparator.comparingDouble(Room::getPricePerNight));
        return sorted;
    }

    @Override
    public List<Room> getRoomsSortedByNumber() {
        List<Room> sorted = new ArrayList<>(rooms);
        sorted.sort(Comparator.comparingInt(Room::getRoomNumber));
        return sorted;
    }

    @Override
    public double getCollectedRevenue() {
        return bills.stream()
                .mapToDouble(Bill::getTotalAmount)
                .sum();
    }

    @Override
    public double getProjectedRevenue() {
        synchronized (bookings) {
            return bookings.stream()
                .filter(b -> !b.isCheckedOut())
                .mapToDouble(b -> {
                    Room r = getRoomByNumber(b.getRoomNumber());
                    if (r == null) return 0.0;
                    long days = java.time.temporal.ChronoUnit.DAYS.between(b.getCheckIn(), b.getCheckOut());
                    if (days <= 0) days = 1;
                    return r.getPricePerNight() * days * 1.298; // Sync with BookingController multiplier
                })
                .sum();
        }
    }

    @Override
    public double getTotalRevenue() {
        return getCollectedRevenue() + getProjectedRevenue();
    }

    @Override
    public boolean deleteRoom(int roomNumber) {
        Room room = roomMap.get(roomNumber);
        if (room == null) {
            FileStorage.writeLog("Delete failed: room " + roomNumber + " not found.");
            return false;
        }
        if (isRoomAvailableForDates(roomNumber, java.time.LocalDate.now(), java.time.LocalDate.now().plusDays(1))) {
            rooms.remove(room);
            roomMap.remove(roomNumber);
            saveData();
            FileStorage.writeLog("Room " + roomNumber + " removed.");
            return true;
        }
        FileStorage.writeLog("Delete rejected: room " + roomNumber + " has active bookings.");
        return false;
    }


    @Override
    public boolean isRoomCleaning(int roomNumber) {
        return cleaningRooms.contains(roomNumber);
    }

    @Override
    public boolean cancelBooking(int bookingId) {
        synchronized (bookings) {
            Booking found = bookings.stream()
                .filter(b -> b.getBookingId() == bookingId)
                .findFirst()
                .orElse(null);

            if (found != null) {
                bookings.remove(found);
                FileStorage.saveBookings(bookings);
                FileStorage.writeLog("Booking CANCELLED: #" + bookingId + " for room " + found.getRoomNumber());
                return true;
            }
            return false;
        }
    }

    @Override
    public synchronized void resetSystemData() {
        FileStorage.clearBills();
        FileStorage.clearLog();
        bills.clear();
        bookings.clear();
        FileStorage.saveBookings(bookings);
        bookingLogQueue.clear();
        billCounter.set(1);
        saveData();
        FileStorage.writeLog("System hard reset performed.");
    }

    private void startLogConsumer() {
        Thread consumerThread = new Thread(() -> {
            while (true) {
                try {
                    // Consumer: Take entry from queue, blocking if empty
                    Pair<Integer, String> entry = bookingLogQueue.take();
                    System.out.println("[AUDIT] Processing booking log: Room " + entry.getFirst() + " by " + entry.getSecond());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        consumerThread.setDaemon(true);
        consumerThread.start();
    }

    private void saveData() {
        FileStorage.saveRooms(rooms);
    }

    @Override
    public String getActivityLog() {
        return FileStorage.readLog();
    }

    @Override
    public List<Booking> getAllBookings() {
        return new ArrayList<>(bookings);
    }
}
