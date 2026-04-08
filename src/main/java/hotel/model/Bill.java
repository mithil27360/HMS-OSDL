package hotel.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Bill model for billing management feature.
 * Uses Wrapper classes (Integer, Double) and autoboxing concepts.
 */
public class Bill implements Serializable {
    private static final long serialVersionUID = 2L;

    private int billId;
    private int roomNumber;
    private String guestName;
    private String roomType;
    private Double pricePerNight;    // Wrapper class (Week 2)
    private Integer numberOfDays;    // Wrapper class (Week 2)
    private Double roomCharge;
    private Double serviceCharge;    // 10% service charge
    private Double taxAmount;        // 18% GST
    private Double totalAmount;
    private String checkInDate;
    private String checkOutDate;

    public Bill() {}

    public Bill(int billId, Room room, int days) {
        this.billId = billId;
        this.roomNumber = room.getRoomNumber();
        this.guestName = room.getGuestName();
        this.roomType = room.getRoomType().getDisplayName();

        // Autoboxing: primitive to wrapper
        this.pricePerNight = room.getPricePerNight();
        this.numberOfDays = days;

        // Unboxing: wrapper to primitive for arithmetic
        double price = pricePerNight;  // unboxing
        int numDays = numberOfDays;    // unboxing

        this.roomCharge = price * numDays;
        this.serviceCharge = roomCharge * 0.10;
        this.taxAmount = (roomCharge + serviceCharge) * 0.18;
        this.totalAmount = roomCharge + serviceCharge + taxAmount;

        this.checkOutDate = room.getCheckOutDate();
        this.checkInDate = room.getCheckInDate();
    }

    // Getters
    public int getBillId() { return billId; }
    public int getRoomNumber() { return roomNumber; }
    public String getGuestName() { return guestName; }
    public String getRoomType() { return roomType; }
    public Double getPricePerNight() { return pricePerNight; }
    public Integer getNumberOfDays() { return numberOfDays; }
    public Double getRoomCharge() { return roomCharge; }
    public Double getServiceCharge() { return serviceCharge; }
    public Double getTaxAmount() { return taxAmount; }
    public Double getTotalAmount() { return totalAmount; }
    public String getCheckInDate() { return checkInDate; }
    public String getCheckOutDate() { return checkOutDate; }

    public String generateBillText() {
        return String.format(
            "╔══════════════════════════════════════╗\n" +
            "║             HOTEL BILL               ║\n" +
            "╠══════════════════════════════════════╣\n" +
            "║ Bill ID     : %-23d║\n" +
            "║ Guest       : %-23s║\n" +
            "║ Room No     : %-23d║\n" +
            "║ Room Type   : %-23s║\n" +
            "║ Check-In    : %-23s║\n" +
            "║ Check-Out   : %-23s║\n" +
            "║ Days Stayed : %-23d║\n" +
            "╠══════════════════════════════════════╣\n" +
            "║ Rate/Night  : ₹%-22.2f║\n" +
            "║ Room Charge : ₹%-22.2f║\n" +
            "║ Service(10%%): ₹%-22.2f║\n" +
            "║ GST (18%%)   : ₹%-22.2f║\n" +
            "╠══════════════════════════════════════╣\n" +
            "║ TOTAL       : ₹%-22.2f║\n" +
            "╚══════════════════════════════════════╝\n",
            billId, guestName, roomNumber, roomType,
            checkInDate, checkOutDate, numberOfDays,
            pricePerNight, roomCharge, serviceCharge, taxAmount, totalAmount
        );
    }
}
