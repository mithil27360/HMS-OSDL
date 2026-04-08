package hotel.dao;

import hotel.exception.RoomAlreadyBookedException;
import hotel.model.Bill;
import hotel.model.Room;
import hotel.model.Booking;
import hotel.util.Pair;
import hotel.util.RoomServiceThread;
import hotel.util.BillingUtils;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;


public class HotelService implements IHotelService {

    private final List<Room> rooms;
    private final Map<Integer, Room> roomMap;
    private final List<Bill> bills;
    private final List<Booking> bookings;
    private final Set<Integer> cleaningRooms;
    
    private final BlockingQueue<Pair<Integer, String>> bookingLogQueue;
    private final AtomicInteger billCounter;
    private final AtomicInteger bookingCounter;
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

        int maxBookingId = bookings.stream()
                .mapToInt(Booking::getBookingId)
                .max()
                .orElse(0);
        bookingCounter = new AtomicInteger(maxBookingId + 1);

    
    
    roomServiceExecutor = Executors.newCachedThreadPool();
        
        
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
                         java.time.LocalDate checkIn, java.time.LocalDate checkOut, String bookedByUsername) throws RoomAlreadyBookedException {
        Room room = roomMap.get(roomNumber);
        if (room == null) throw new IllegalArgumentException("Room no longer exists.");

        synchronized (bookings) {
            if (!isRoomAvailableForDates(roomNumber, checkIn, checkOut)) {
                throw new RoomAlreadyBookedException("Room " + roomNumber + " is already reserved for these dates.");
            }

            Booking booking = new Booking(
                bookingCounter.getAndIncrement(),
                roomNumber, guestName, contact, bookedByUsername, checkIn, checkOut
            );

            try {
                bookings.add(booking);
                FileStorage.saveBookings(bookings);
                bookingLogQueue.offer(new Pair<>(roomNumber, guestName));
                FileStorage.writeLog("Room " + roomNumber + " reserved for " + checkIn + " to " + checkOut + " by " + bookedByUsername);
            } catch (Exception e) {
                bookings.remove(booking);
                bookingCounter.decrementAndGet();
                FileStorage.writeLog("Booking rollback for room " + roomNumber + ": " + e.getMessage());
                throw new RuntimeException("Booking failed.", e);
            }
        }
    }

    
    private boolean _isRoomAvailableForDatesUnsafe(int roomNumber, java.time.LocalDate checkIn, java.time.LocalDate checkOut) {
        
        if (cleaningRooms.contains(roomNumber) && !checkIn.isAfter(java.time.LocalDate.now())) {
            return false;
        }

        
        boolean hasOverlap = bookings.stream()
            .filter(b -> b.getRoomNumber() == roomNumber)
            .anyMatch(b -> b.overlaps(checkIn, checkOut));
        
        if (hasOverlap) return false;

        
        
        if (checkIn.isEqual(java.time.LocalDate.now())) {
            boolean hasUncheckedOutGuest = bookings.stream()
                .filter(b -> b.getRoomNumber() == roomNumber && !b.isCheckedOut())
                .anyMatch(b -> b.getCheckOut().isEqual(checkIn));
            
            if (hasUncheckedOutGuest) return false;
        }

        return true;
    }

    @Override
    public boolean isRoomAvailableForDates(int roomNumber, java.time.LocalDate checkIn, java.time.LocalDate checkOut) {
        synchronized (bookings) {
            return _isRoomAvailableForDatesUnsafe(roomNumber, checkIn, checkOut);
        }
    }

    
    @Override
    public boolean isOccupiedToday(int roomNumber) {
        java.time.LocalDate today = java.time.LocalDate.now();
        synchronized (bookings) {
            return bookings.stream()
                .filter(b -> b.getRoomNumber() == roomNumber && !b.isCheckedOut())
                .anyMatch(b -> (today.isEqual(b.getCheckIn()) || today.isAfter(b.getCheckIn())) && 
                               (today.isBefore(b.getCheckOut()) || today.isEqual(b.getCheckOut())));
        }
    }


    private void startBackgroundServices(int roomNumber) {
        cleaningRooms.add(roomNumber);
        
        Runnable onComplete = () -> {
            
            
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

            return performCheckout(activeBooking);
        }
    }

    
    public Bill checkoutBooking(int bookingId) {
        synchronized (bookings) {
            Booking activeBooking = bookings.stream()
                .filter(b -> b.getBookingId() == bookingId && !b.isCheckedOut())
                .findFirst()
                .orElse(null);

            if (activeBooking == null) return null;

            return performCheckout(activeBooking);
        }
    }

    
    private Bill performCheckout(Booking activeBooking) {
        int roomNumber = activeBooking.getRoomNumber();
        Room room = roomMap.get(roomNumber);
        if (room == null) return null;

        Bill bill = new Bill(billCounter.getAndIncrement(), room, activeBooking);

        try {
            activeBooking.setCheckedOut(true);
            bills.add(bill);
            
            
            FileStorage.saveBills(new ArrayList<>(bills));
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

    @Override

    public List<Room> getAllRooms() {
        return new ArrayList<>(rooms);
    }

    @Override
    public List<Room> getAvailableRooms(java.time.LocalDate start, java.time.LocalDate end) {
        return rooms.stream()
            .filter(r -> isRoomAvailableForDates(r.getRoomNumber(), start, end))
            .collect(Collectors.toList());
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
                    
                    return r.getPricePerNight() * days * BillingUtils.getTotalMultiplier();
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
                .filter(b -> b.getBookingId() == bookingId && !b.isCheckedOut())
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
        
        bookingCounter.set(1);
        saveData();
        FileStorage.writeLog("System hard reset performed.");
    }

    private void startLogConsumer() {
        Thread consumerThread = new Thread(() -> {
            while (true) {
                try {
                    
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
