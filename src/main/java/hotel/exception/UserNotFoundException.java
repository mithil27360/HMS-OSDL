package hotel.exception;

/**
 * Exception thrown when a user is not found in the system.
 */
public class UserNotFoundException extends HMSException {
    public UserNotFoundException(String username) {
        super("User with username '" + username + "' was not found.");
    }
}
