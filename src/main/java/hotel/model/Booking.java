package hotel.model;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * Booking model representing a room reservation.
 */
public class Booking implements Serializable {
    private static final long serialVersionUID = 4L;

    private int bookingId;
    private int roomNumber;
    private String guestName;
    private String guestContact;
    private LocalDate checkIn;
    private LocalDate checkOut;
    private boolean isCheckedOut;

    public Booking(int bookingId, int roomNumber, String guestName, String guestContact, 
                   LocalDate checkIn, LocalDate checkOut) {
        this.bookingId = bookingId;
        this.roomNumber = roomNumber;
        this.guestName = guestName;
        this.guestContact = guestContact;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.isCheckedOut = false;
    }

    public int getBookingId() { return bookingId; }
    public int getRoomNumber() { return roomNumber; }
    public String getGuestName() { return guestName; }
    public String getGuestContact() { return guestContact; }
    public LocalDate getCheckIn() { return checkIn; }
    public LocalDate getCheckOut() { return checkOut; }
    public boolean isCheckedOut() { return isCheckedOut; }
    public void setCheckedOut(boolean checkedOut) { isCheckedOut = checkedOut; }

    public boolean overlaps(LocalDate start, LocalDate end) {
        if (isCheckedOut) return false;
        // B1: checkIn to checkOut
        // B2: start to end
        // Conflict if: (StartA < EndB) and (EndA > StartB)
        return start.isBefore(checkOut) && end.isAfter(checkIn);
    }
}
