package hotel.exception;

/**
 * Exception thrown for authentication failures (wrong password, inactive account).
 */
public class AuthException extends HMSException {
    public AuthException(String message) {
        super(message);
    }
}
