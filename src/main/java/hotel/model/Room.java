package hotel.model;

import java.io.Serializable;

/**
 * Room model class demonstrating:
 * - Encapsulation (private fields, getters/setters)
 * - Serialization (implements Serializable for file persistence)
 */
public class Room implements Serializable {
    private static final long serialVersionUID = 1L;

    // Encapsulated private fields
    private int roomNumber;
    private RoomType roomType;
    private double pricePerNight;
    private boolean isBooked;
    private String guestName;
    private String guestContact;
    private int daysBooked;
    private String checkInDate;
    private String checkOutDate;

    // Enum for room types (Week 2 - Enumeration with constructor and methods)
    public enum RoomType {
        STANDARD(1500, "Standard Room"),
        DOUBLE(2500, "Double Room"),
        DELUXE(3500, "Deluxe Room"),
        SUITE(6000, "Suite");

        private final double basePrice;
        private final String displayName;

        // Enum constructor
        RoomType(double basePrice, String displayName) {
            this.basePrice = basePrice;
            this.displayName = displayName;
        }

        // Enum methods
        public double getBasePrice() { return basePrice; }
        public String getDisplayName() { return displayName; }

        public double calculateCost(int nights) {
            return basePrice * nights;
        }
    }

    // Constructor - no args
    public Room() {}

    // Constructor with all fields
    public Room(int roomNumber, RoomType roomType) {
        this.roomNumber = roomNumber;
        this.roomType = roomType;
        this.pricePerNight = roomType.getBasePrice();
        this.isBooked = false;
        this.guestName = "";
        this.guestContact = "";
        this.daysBooked = 0;
        this.checkInDate = "";
        this.checkOutDate = "";
    }

    // Getters and Setters (Encapsulation)
    public int getRoomNumber() { return roomNumber; }
    public void setRoomNumber(int roomNumber) {
        if (roomNumber > 0) this.roomNumber = roomNumber;
    }

    public RoomType getRoomType() { return roomType; }
    public void setRoomType(RoomType roomType) { this.roomType = roomType; }

    public double getPricePerNight() { return pricePerNight; }
    public void setPricePerNight(double price) {
        if (price > 0) this.pricePerNight = price;  // Validation
        else throw new IllegalArgumentException("Price must be positive");
    }

    public boolean isBooked() { return isBooked; }
    public void setBooked(boolean booked) { isBooked = booked; }

    public String getGuestName() { return guestName; }
    public void setGuestName(String guestName) { this.guestName = guestName; }

    public String getGuestContact() { return guestContact; }
    public void setGuestContact(String guestContact) { this.guestContact = guestContact; }

    public int getDaysBooked() { return daysBooked; }
    public void setDaysBooked(int daysBooked) { this.daysBooked = daysBooked; }

    public String getCheckInDate() { return checkInDate; }
    public void setCheckInDate(String checkInDate) { this.checkInDate = checkInDate; }

    public String getCheckOutDate() { return checkOutDate; }
    public void setCheckOutDate(String checkOutDate) { this.checkOutDate = checkOutDate; }

    public String getAvailabilityStatus() {
        return isBooked ? "OCCUPIED" : "AVAILABLE";
    }

    @Override
    public String toString() {
        return String.format("Room[%d | %s | ₹%.0f/night | %s]",
                roomNumber, roomType.getDisplayName(), pricePerNight, getAvailabilityStatus());
    }
}
