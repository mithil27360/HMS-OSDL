package hotel.dao;

import hotel.exception.RoomAlreadyBookedException;
import hotel.model.Bill;
import hotel.model.Room;
import java.util.List;

public interface IHotelService {

    
    void addRoom(Room room);
    
    /**
     * Reserves a room for a guest.
     * @throws RoomAlreadyBookedException if the room is already occupied.
     */
    void bookRoom(int roomNumber, String guestName, String contact, int days, 
                  java.time.LocalDate checkIn, java.time.LocalDate checkOut) throws RoomAlreadyBookedException;
    
    boolean isRoomAvailableForDates(int roomNumber, java.time.LocalDate checkIn, java.time.LocalDate checkOut);

    Bill checkoutRoom(int roomNumber);
    
    /**
     * Checks in a guest for an active booking.
     * Generates the bill immediately.
     */
    void checkInRoom(int roomNumber);

    
    List<Room> getAllRooms();
    
    List<Room> getAvailableRooms(java.time.LocalDate start, java.time.LocalDate end);
    
    List<Room> getBookedRooms(java.time.LocalDate date);
    
    // Legacy support (defaults to today)
    default List<Room> getAvailableRooms() { return getAvailableRooms(java.time.LocalDate.now(), java.time.LocalDate.now().plusDays(1)); }
    default List<Room> getBookedRooms() { return getBookedRooms(java.time.LocalDate.now()); }
    
    Room getRoomByNumber(int number);
    
    List<Bill> getAllBills();
    
    List<Room> getRoomsByType(Room.RoomType type);
    
    List<Room> getRoomsSortedByPrice();
    
    List<Room> getRoomsSortedByNumber();
    
    double getTotalRevenue();
    
    boolean deleteRoom(int roomNumber);
    
    boolean isRoomCleaning(int roomNumber);
    
    boolean cancelBooking(int bookingId);
    
    void resetSystemData();
    
    String getActivityLog();
    
    List<hotel.model.Booking> getAllBookings();
}
