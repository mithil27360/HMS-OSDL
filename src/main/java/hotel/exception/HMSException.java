package hotel.exception;

/**
 * Base exception for the Hotel Management System.
 */
public class HMSException extends Exception {
    public HMSException(String message) {
        super(message);
    }

    public HMSException(String message, Throwable cause) {
        super(message, cause);
    }
}
