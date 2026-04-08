# Hotel Management System (HMS-OSDL)

A professional-grade JavaFX application for modern hotel management, featuring a "Bleisure" (Business + Leisure) aesthetic and high-performance room booking logic.

## 🏨 Key Features

- **Professional Dashboard**: Real-time business metrics including Total Earnings, Occupancy rates, and Room Overview.
- **Calendar-Based Booking**: Interactive date pickers for precise Check-in/Check-out management.
- **Secure Authentication**: SHA-256 password hashing and thread-safe singleton architecture.
- **Robust Persistence**: Centralized data management in `~/HMS_Data/` with buffered I/O.
- **Clean UI Architecture**: Cohesive CSS design system replacing inline styling for better maintainability.

## 🚀 Getting Started

### Prerequisites
- Java 17 or higher
- Maven 3.8+

### Installation & Run
1. Clone the repository:
   ```bash
   git clone git@github.com:mithil27360/HMS-OSDL.git
   ```
2. Navigate to the project directory:
   ```bash
   cd HotelManagement
   ```
3. Run the application:
   ```bash
   mvn clean javafx:run
   ```

## 🔐 Default Credentials (SHA-256 Secured)

| Role | Username | Password |
| :--- | :--- | :--- |
| **Admin** | `admin` | `admin123` |
| **Receptionist** | `receptionist` | `staff123` |
| **Guest** | `guest` | `guest123` |

---
*Developed as a high-performance, presentation-ready solution.*
