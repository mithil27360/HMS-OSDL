package hotel.dao;

import hotel.model.Bill;
import hotel.model.Booking;
import hotel.model.Room;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import java.util.Properties;
import java.util.Objects;


public class FileStorage {

    private static final String CONFIG_FILE = "/config.properties";
    private static final Properties props = new Properties();
    
    private static String DATA_DIR;
    private static String ROOMS_FILE;
    private static String BILLS_FILE;
    private static String USERS_FILE;
    private static String BOOKINGS_FILE;
    private static String LOG_FILE;
    private static int MAX_LOG_LINES;

    
    private static final Set<String> COMMON_ALLOWED_CLASSES = Set.of(
        "java.util.ArrayList",
        "java.lang.String",
        "java.lang.Integer",
        "java.lang.Long",
        "java.lang.Double",
        "java.lang.Float",
        "java.lang.Boolean",
        "java.lang.Object",
        "[Ljava.lang.Object;",
        "java.time.LocalDate",
        "java.time.Ser"
    );

    private static final Set<String> ROOMS_ALLOWED_CLASSES = buildAllowedClasses(
        "hotel.model.Room",
        "hotel.model.Room$RoomType"
    );

    private static final Set<String> BOOKINGS_ALLOWED_CLASSES = buildAllowedClasses(
        "hotel.model.Booking"
    );

    private static final Set<String> BILLS_ALLOWED_CLASSES = buildAllowedClasses(
        "hotel.model.Bill"
    );

    static {
        try (InputStream is = FileStorage.class.getResourceAsStream(CONFIG_FILE)) {
            props.load(Objects.requireNonNull(is, "Config file not found: " + CONFIG_FILE));
            
            
            DATA_DIR = System.getProperty("user.home") + "/HMS_Data/";
            Files.createDirectories(Paths.get(DATA_DIR));

            ROOMS_FILE = DATA_DIR + props.getProperty("rooms.path", "rooms.dat");
            BILLS_FILE = DATA_DIR + props.getProperty("bills.path", "bills.dat");
            USERS_FILE = DATA_DIR + props.getProperty("users.path", "users.dat");
            BOOKINGS_FILE = DATA_DIR + props.getProperty("bookings.path", "bookings.dat");
            LOG_FILE   = DATA_DIR + props.getProperty("logs.path", "activity.log");
            MAX_LOG_LINES = Integer.parseInt(props.getProperty("max.log.lines", "10000"));

            
            Files.createDirectories(Paths.get(ROOMS_FILE).getParent());
            Files.createDirectories(Paths.get(BILLS_FILE).getParent());
            Files.createDirectories(Paths.get(USERS_FILE).getParent());
            Files.createDirectories(Paths.get(BOOKINGS_FILE).getParent());
            Files.createDirectories(Paths.get(LOG_FILE).getParent());

        } catch (Exception e) {
            System.err.println("CRITICAL: HMS Configuration failure. Using defaults.");
            e.printStackTrace();
            DATA_DIR = System.getProperty("user.home") + "/HMS_Data/";
            ROOMS_FILE = DATA_DIR + "rooms.dat";
            BILLS_FILE = DATA_DIR + "bills.dat";
            USERS_FILE = DATA_DIR + "users.dat";
            BOOKINGS_FILE = DATA_DIR + "bookings.dat";
            LOG_FILE = DATA_DIR + "activity.log";
            MAX_LOG_LINES = 10000;
        }
    }

    public static String getUsersFile() { return USERS_FILE; }


    public static void saveRooms(List<Room> rooms) {
        Path tmp = Paths.get(ROOMS_FILE + ".tmp");
        Path real = Paths.get(ROOMS_FILE);
        try (ObjectOutputStream oos = new ObjectOutputStream(
                new BufferedOutputStream(Files.newOutputStream(tmp)))) {
            oos.writeObject(rooms);
            Files.move(tmp, real, java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                                  java.nio.file.StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            writeLog("Persistence Error [Rooms]", e);
            try { Files.deleteIfExists(tmp); } catch (IOException ignored) {}
        }
    }

    public static List<Room> loadRooms() {
        if (!Files.exists(Paths.get(ROOMS_FILE))) return new ArrayList<>();
        try {
            return readValidatedList(Paths.get(ROOMS_FILE), ROOMS_ALLOWED_CLASSES, Room.class);
        } catch (IOException | ClassNotFoundException | SecurityException e) {
            writeLog("Persistence Error [LoadRooms]", e);
            return new ArrayList<>();
        }
    }

    public static void saveBill(Bill bill) {
        List<Bill> bills = loadBills();  
        bills.add(bill);
        saveBills(bills);
    }

    public static void saveBills(List<Bill> bills) {
        Path tmp = Paths.get(BILLS_FILE + ".tmp");
        Path real = Paths.get(BILLS_FILE);
        try (ObjectOutputStream oos = new ObjectOutputStream(
                new BufferedOutputStream(Files.newOutputStream(tmp)))) {
            oos.writeObject(bills);
            
            Files.move(tmp, real, java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                                  java.nio.file.StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            writeLog("Persistence Error [SaveBills]", e);
            try { Files.deleteIfExists(tmp); } catch (IOException ignored) {}
        }
    }


    public static void saveBookings(List<hotel.model.Booking> bookings) {
        Path tmp = Paths.get(BOOKINGS_FILE + ".tmp");
        Path real = Paths.get(BOOKINGS_FILE);
        try (ObjectOutputStream oos = new ObjectOutputStream(
                new BufferedOutputStream(Files.newOutputStream(tmp)))) {
            oos.writeObject(bookings);
            Files.move(tmp, real, java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                                  java.nio.file.StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            writeLog("Persistence Error [Bookings]", e);
            try { Files.deleteIfExists(tmp); } catch (IOException ignored) {}
        }
    }

    public static List<Booking> loadBookings() {
        if (!Files.exists(Paths.get(BOOKINGS_FILE))) return new ArrayList<>();
        try {
            return readValidatedList(Paths.get(BOOKINGS_FILE), BOOKINGS_ALLOWED_CLASSES, Booking.class);
        } catch (IOException | ClassNotFoundException | SecurityException e) {
            writeLog("Persistence Error [LoadBookings]", e);
            return new ArrayList<>();
        }
    }


    public static List<Bill> loadBills() {
        if (!Files.exists(Paths.get(BILLS_FILE))) return new ArrayList<>();
        try {
            return readValidatedList(Paths.get(BILLS_FILE), BILLS_ALLOWED_CLASSES, Bill.class);
        } catch (IOException | ClassNotFoundException | SecurityException e) {
            System.err.println("Load Error [Bills]: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    private static Set<String> buildAllowedClasses(String... additional) {
        Set<String> allowed = new HashSet<>(COMMON_ALLOWED_CLASSES);
        for (String cls : additional) {
            allowed.add(cls);
        }
        return allowed;
    }

    private static ObjectInputStream createFilteredInputStream(InputStream input, Set<String> allowedClasses) throws IOException {
        ObjectInputStream ois = new ObjectInputStream(input);
        ois.setObjectInputFilter(info -> {
            if (info.depth() > 32 || info.references() > 50_000 || info.arrayLength() > 100_000) {
                return ObjectInputFilter.Status.REJECTED;
            }

            Class<?> clazz = info.serialClass();
            if (clazz == null) {
                return ObjectInputFilter.Status.UNDECIDED;
            }

            if (clazz.isPrimitive()) {
                return ObjectInputFilter.Status.ALLOWED;
            }

            if (clazz.isArray()) {
                Class<?> component = clazz;
                while (component.isArray()) {
                    component = component.getComponentType();
                }
                if (component.isPrimitive()) {
                    return ObjectInputFilter.Status.ALLOWED;
                }
                return isClassAllowed(component, allowedClasses)
                    ? ObjectInputFilter.Status.ALLOWED
                    : ObjectInputFilter.Status.REJECTED;
            }

            return isClassAllowed(clazz, allowedClasses)
                ? ObjectInputFilter.Status.ALLOWED
                : ObjectInputFilter.Status.REJECTED;
        });
        return ois;
    }

    private static boolean isClassAllowed(Class<?> clazz, Set<String> allowedClasses) {
        return allowedClasses.contains(clazz.getName());
    }

    private static <T> List<T> readValidatedList(Path filePath, Set<String> allowedClasses, Class<T> elementType)
            throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = createFilteredInputStream(
                new BufferedInputStream(Files.newInputStream(filePath)), allowedClasses)) {
            Object data = ois.readObject();
            if (!(data instanceof List<?> rawList)) {
                throw new InvalidObjectException("Unexpected persisted format: expected List");
            }

            List<T> validated = new ArrayList<>(rawList.size());
            for (Object item : rawList) {
                if (!elementType.isInstance(item)) {
                    throw new InvalidObjectException("Unexpected list element type: " +
                        (item == null ? "null" : item.getClass().getName()));
                }
                validated.add(elementType.cast(item));
            }
            return validated;
        }
    }

    public static void writeLog(String message) {
        writeLog(message, null);
    }

    public static void writeLog(String message, Throwable throwable) {
        
        try {
            Path logPath = Paths.get(LOG_FILE);
            if (Files.exists(logPath) && Files.size(logPath) > 5 * 1024 * 1024) { 
                Deque<String> tail = new ArrayDeque<>(2000);
                int lineCount = 0;
                try (BufferedReader br = Files.newBufferedReader(logPath)) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        lineCount++;
                        if (tail.size() == 2000) {
                            tail.removeFirst();
                        }
                        tail.addLast(line);
                    }
                }
                if (lineCount >= MAX_LOG_LINES) {
                    List<String> truncated = new ArrayList<>(tail.size() + 1);
                    truncated.add("[LOG] System statistics reset due to rotation. Continuing log.");
                    truncated.addAll(tail);
                    Files.write(logPath, truncated, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
                }
            }
        } catch (IOException e) {
            System.err.println("Log Rotation failed: " + e.getMessage());
        }

        try (PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(LOG_FILE, true)))) {
            pw.printf("[%tF %<tT] %s%n", System.currentTimeMillis(), message);
            if (throwable != null) {
                pw.println("STACK TRACE:");
                throwable.printStackTrace(pw);
                pw.println("--------------------------------------------------");
            }
        } catch (IOException e) {
            System.err.println("FATAL: Logging failed: " + e.getMessage());
        }
    }

    public static String readLog() {
        if (!Files.exists(Paths.get(LOG_FILE))) return "No activity log found.";
        try {
            return Files.readString(Paths.get(LOG_FILE));
        } catch (IOException e) {
            return "Error reading log: " + e.getMessage();
        }
    }

    public static void clearBills() {
        try { Files.deleteIfExists(Paths.get(BILLS_FILE)); } catch (IOException ignored) {}
    }

    public static void clearLog() {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(LOG_FILE, false))) {
            bw.write("[LOG] System statistics reset. Starting fresh.\n");
        } catch (IOException e) {
            System.err.println("Log clear error: " + e.getMessage());
        }
    }
}

