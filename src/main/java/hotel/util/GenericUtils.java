package hotel.util;

/**
 * Generic utility methods (Week 7 - Generic methods, bounded types).
 */
public class GenericUtils {

    // Generic method to display any value
    public static <T> void display(T value) {
        System.out.println("Value: " + value);
    }

    // Generic method to print array (Week 7 - Generic method with array)
    public static <T> void printArray(T[] array) {
        for (T element : array) {
            System.out.print(element + " ");
        }
        System.out.println();
    }

    // Bounded generic method - only numeric types (Week 7 - Bounded types)
    public static <T extends Number> double calculateDiscountedPrice(T price, T discountPercent) {
        double p = price.doubleValue();       // unboxing via doubleValue
        double d = discountPercent.doubleValue();
        return p - (p * d / 100);
    }

    // Bounded generic sum
    public static <T extends Number> double sum(T a, T b) {
        return a.doubleValue() + b.doubleValue();
    }
}
