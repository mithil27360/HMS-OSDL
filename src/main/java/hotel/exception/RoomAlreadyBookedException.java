package hotel.exception;

/**
 * Exception thrown when a booking is attempted on an already occupied room.
 */
public class RoomAlreadyBookedException extends HMSException {
    
    public RoomAlreadyBookedException(String message) {
        super(message);
    }

    public RoomAlreadyBookedException(int roomNumber) {
        super("Room " + roomNumber + " is already occupied.");
    }
}
