package hotel.util;


public class GenericUtils {

    
    public static String formatRupees(double amount) {
        return String.format("₹%.2f", amount);
    }
}
