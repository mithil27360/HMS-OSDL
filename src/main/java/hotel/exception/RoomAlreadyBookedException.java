package hotel.exception;


public class RoomAlreadyBookedException extends HMSException {
    
    public RoomAlreadyBookedException(String message) {
        super(message);
    }

    public RoomAlreadyBookedException(int roomNumber) {
        super("Room " + roomNumber + " is already occupied.");
    }
}
