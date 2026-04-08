package hotel.exception;


public class UserNotFoundException extends HMSException {
    public UserNotFoundException(String username) {
        super("User with username '" + username + "' was not found.");
    }
}
