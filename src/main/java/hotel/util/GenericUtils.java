package hotel.util;

/**
 * Utility methods for formatting and business logic.
 */
public class GenericUtils {

    /**
     * Formats a double amount into a Rupee currency string.
     */
    public static String formatRupees(double amount) {
        return String.format("₹%.2f", amount);
    }
}
