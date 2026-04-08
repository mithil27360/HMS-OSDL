package hotel.util;

import hotel.dao.FileStorage;


public class RoomServiceThread implements Runnable {

    private final String serviceName;
    private final int roomNumber;
    private final Runnable onComplete;
    private volatile boolean running = true;

    public RoomServiceThread(String serviceName, int roomNumber, Runnable onComplete) {
        this.serviceName = serviceName;
        this.roomNumber = roomNumber;
        this.onComplete = onComplete;
    }

    @Override
    public void run() {
        FileStorage.writeLog("Room service [" + serviceName + "] started for room " + roomNumber);
        try {
            
            Thread.sleep(2000); 
            
            if (running) {
                FileStorage.writeLog("Room service [" + serviceName + "] completed for room " + roomNumber);
            }
        } catch (InterruptedException e) {
            System.err.println(serviceName + " interrupted for Room " + roomNumber);
            Thread.currentThread().interrupt();
            FileStorage.writeLog("Room service [" + serviceName + "] interrupted for room " + roomNumber);
        } finally {
            if (onComplete != null) onComplete.run();
        }
    }

    public void stop() {
        running = false;
    }
}
