package hotel.dao;

import hotel.exception.AuthException;
import hotel.exception.UserNotFoundException;
import hotel.model.User;
import java.util.List;

/**
 * Interface defining authentication and user management services.
 */
public interface IAuthService {
    
    /**
     * Authenticates a user based on credentials.
     * @throws AuthException if authentication fails.
     * @throws UserNotFoundException if username doesn't exist.
     */
    User login(String username, String password) throws AuthException, UserNotFoundException;
    
    void logout();
    
    User getCurrentUser();
    
    /**
     * Adds a new user to the system.
     * @return true if successful, false if username exists.
     */
    boolean addUser(User user);
    
    boolean deleteUser(String username);
    
    List<User> getAllUsers();
    
    List<User> getUsersByRole(User.Role role);
}
