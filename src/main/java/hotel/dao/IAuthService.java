package hotel.dao;

import hotel.exception.AuthException;
import hotel.exception.UserNotFoundException;
import hotel.model.User;
import java.util.List;


public interface IAuthService {
    
    
    User login(String username, String password) throws AuthException, UserNotFoundException;
    
    void logout();
    
    User getCurrentUser();
    
    
    boolean addUser(User user);
    
    boolean deleteUser(String username);
    
    List<User> getAllUsers();
    
    List<User> getUsersByRole(User.Role role);
}
