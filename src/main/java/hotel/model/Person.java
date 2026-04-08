package hotel.model;

import java.io.Serializable;

/**
 * Abstract class representing a person in the system.
 */
public abstract class Person implements Serializable {
    private static final long serialVersionUID = 1L;

    protected String fullName;
    protected String email;
    protected String phone;

    public Person() {}

    public Person(String fullName, String email, String phone) {
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
    }

    // Abstract method to be implemented by subclasses
    public abstract String getRoleDescription();

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { 
        if (fullName == null || fullName.trim().isEmpty()) {
            throw new IllegalArgumentException("Full name cannot be empty");
        }
        this.fullName = fullName; 
    }

    public String getEmail() { return email; }
    public void setEmail(String email) { 
        if (email != null && !email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new IllegalArgumentException("Invalid email format");
        }
        this.email = email; 
    }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { 
        if (phone != null && !phone.matches("^\\d{10}$")) {
            throw new IllegalArgumentException("Phone must be a 10-digit number");
        }
        this.phone = phone; 
    }
}
