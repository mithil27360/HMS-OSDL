package hotel.model;

import java.io.Serializable;


public class Room implements Serializable {
    private static final long serialVersionUID = 1L;

    private int roomNumber;
    private RoomType roomType;
    private double pricePerNight;

    public enum RoomType {
        STANDARD(1500, "Standard Room"),
        DOUBLE(2500, "Double Room"),
        DELUXE(3500, "Deluxe Room"),
        SUITE(6000, "Suite");

        private final double basePrice;
        private final String displayName;

        RoomType(double basePrice, String displayName) {
            this.basePrice = basePrice;
            this.displayName = displayName;
        }

        public double getBasePrice() { return basePrice; }
        public String getDisplayName() { return displayName; }

        public double calculateCost(int nights) {
            return basePrice * nights;
        }
    }

    public Room() {}

    public Room(int roomNumber, RoomType roomType) {
        setRoomNumber(roomNumber);
        this.roomType = roomType;
        this.pricePerNight = roomType.getBasePrice();
    }

    public int getRoomNumber() { return roomNumber; }
    public void setRoomNumber(int roomNumber) {
        if (roomNumber < 100 || roomNumber > 999) {
            throw new IllegalArgumentException("Room number must be 3 digits (100-999)");
        }
        this.roomNumber = roomNumber;
    }

    public RoomType getRoomType() { return roomType; }
    public void setRoomType(RoomType roomType) { this.roomType = roomType; }

    public double getPricePerNight() { return pricePerNight; }
    public void setPricePerNight(double price) {
        if (price <= 0) {
            throw new IllegalArgumentException("Price per night must be positive and non-zero.");
        }
        this.pricePerNight = price; 
    }

    @Override
    public String toString() {
        return String.format("Room[%d | %s | ₹%.0f/night]",
                roomNumber, roomType.getDisplayName(), pricePerNight);
    }
}
