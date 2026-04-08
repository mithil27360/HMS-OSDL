package hotel.model;

import java.io.Serializable;
import hotel.util.BillingUtils;

/**
 * Bill model for billing management feature.
 */
public class Bill implements Serializable {
    private static final long serialVersionUID = 2L;

    private int billId;
    private int roomNumber;
    private String guestName;
    private String roomType;
    private double pricePerNight;
    private int numberOfDays;
    private double roomCharge;
    private double serviceCharge;
    private double taxAmount;
    private double totalAmount;
    private String checkInDate;
    private String checkOutDate;

    public Bill() {}

    public Bill(int billId, Room room, Booking booking) {
        this.billId = billId;
        this.roomNumber = room.getRoomNumber();
        this.guestName = booking.getGuestName();
        this.roomType = room.getRoomType().getDisplayName();
        this.pricePerNight = room.getPricePerNight();
        
        java.time.temporal.ChronoUnit unit = java.time.temporal.ChronoUnit.DAYS;
        this.numberOfDays = (int) unit.between(booking.getCheckIn(), booking.getCheckOut());
        if (this.numberOfDays <= 0) this.numberOfDays = 1;

        // Use centralized BillingUtils for consistent calculations across the system
        this.roomCharge = pricePerNight * numberOfDays;
        this.serviceCharge = roomCharge * BillingUtils.SERVICE_CHARGE_RATE;
        this.taxAmount = (roomCharge + serviceCharge) * BillingUtils.GST_RATE;
        this.totalAmount = roomCharge + serviceCharge + taxAmount;

        this.checkOutDate = booking.getCheckOut().toString();
        this.checkInDate = booking.getCheckIn().toString();
    }

    public int getBillId() { return billId; }
    public int getRoomNumber() { return roomNumber; }
    public String getGuestName() { return guestName; }
    public String getRoomType() { return roomType; }
    public double getPricePerNight() { return pricePerNight; }
    public int getNumberOfDays() { return numberOfDays; }
    public double getRoomCharge() { return roomCharge; }
    public double getServiceCharge() { return serviceCharge; }
    public double getTaxAmount() { return taxAmount; }
    public double getTotalAmount() { return totalAmount; }
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
