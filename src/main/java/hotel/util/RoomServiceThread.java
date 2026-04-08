package hotel.util;

/**
 * Room Service Thread demonstrating:
 * - Thread creation using Runnable interface (Week 3)
 * - sleep() method (Week 3)
 * - Inter-thread communication concept (Week 4)
 */
public class RoomServiceThread implements Runnable {

    private final String serviceName;
    private final int roomNumber;
    private final Runnable onComplete;
    private volatile boolean running = true;

    // Thread creation via Runnable interface (Week 3)
    public RoomServiceThread(String serviceName, int roomNumber, Runnable onComplete) {
        this.serviceName = serviceName;
        this.roomNumber = roomNumber;
        this.onComplete = onComplete;
    }

    @Override
    public void run() {
        System.out.println(serviceName + " started for Room " + roomNumber);
        try {
            // sleep() - Week 3
            for (int i = 1; i <= 3 && running; i++) {
                System.out.println(serviceName + " - Step " + i + " for Room " + roomNumber);
                Thread.sleep(300);
                Thread.yield(); // yield() - Week 3
            }
            if (running) {
                System.out.println(serviceName + " completed for Room " + roomNumber);
                if (onComplete != null) onComplete.run();
            }
        } catch (InterruptedException e) {
            System.out.println(serviceName + " interrupted.");
            Thread.currentThread().interrupt();
        }
    }

    public void stop() {
        running = false;
    }

    /**
     * Start multiple service threads simultaneously (Week 3)
     */
    public static void startRoomServices(int roomNumber) {
        RoomServiceThread cleaning = new RoomServiceThread("Room Cleaning", roomNumber, null);
        RoomServiceThread foodDelivery = new RoomServiceThread("Food Delivery", roomNumber, null);
        RoomServiceThread maintenance = new RoomServiceThread("Maintenance Check", roomNumber, null);

        Thread t1 = new Thread(cleaning, "Cleaning-Thread");
        Thread t2 = new Thread(foodDelivery, "Food-Thread");
        Thread t3 = new Thread(maintenance, "Maintenance-Thread");

        t1.start();
        t2.start();
        t3.start();

        try {
            t1.join(); // join() - Week 3
            t2.join();
            t3.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        System.out.println("All room services for Room " + roomNumber + " completed.");
    }
}
