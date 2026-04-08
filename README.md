# Hotel Management System (HMS-OSDL)

A professional-grade JavaFX application for modern hotel management, featuring a "Bleisure" (Business + Leisure) aesthetic and high-performance room booking logic.

## 🏨 Key Features

- **Professional Dashboard**: Real-time business metrics including Total Earnings, Occupancy rates, and Room Overview.
- **Calendar-Based Booking**: Interactive date pickers for precise Check-in/Check-out management with automatic stay duration calculation.
- **Structural Sidebar Design**: Modern vertical navigation sidebar for efficient workspace management.
- **Dynamic Billing**: Automated bill generation with tax calculations and service charge integration.
- **Multi-Role Access**: Dedicated views for Admins, Receptionists, and Guests.
- **System Activity & Reports**: Comprehensive action logging and data backup/reset capabilities for presentation readiness.

## 🛠️ Technology Stack

- **Lanuage**: Java 17+
- **Framework**: JavaFX
- **Build Tool**: Maven
- **Persistence**: File-based Serialization (for portability)
- **Styling**: Vanilla CSS (Custom modern theme)

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

## 👩‍💻 Default Credentials

| Role | Username | Password |
| :--- | :--- | :--- |
| **Admin** | `admin` | `admin123` |
| **Receptionist** | `staff123` | `staff123` |
| **Guest** | `guest` | `guest123` |

## 📁 Project Structure

- `src/main/java/hotel/controller`: UI Logic and Event Handling.
- `src/main/java/hotel/model`: Data Models (Room, User, Bill).
- `src/main/java/hotel/dao`: Data Access and Services (Auth, Room Management).
- `src/main/resources/hotel/css`: Custom styling and themes.

---
*Developed as a high-performance, presentation-ready solution.*
