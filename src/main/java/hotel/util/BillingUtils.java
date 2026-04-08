package hotel.util;

/**
 * Billing utility for consistent calculations across checkouts and UI.
 * Provides centralized tax and calculation logic.
 */
public class BillingUtils {
    
    // Tax rates as constants for easy adjustment
    public static final double SERVICE_CHARGE_RATE = 0.10;    // 10%
    public static final double GST_RATE = 0.18;               // 18%

    /**
     * Calculate total bill including service charge and GST.
     * Formula: roomCharge + 10% service = subtotal → subtotal + 18% GST on subtotal = total
     * Simplifies to: roomCharge * 1.1 * 1.18 = roomCharge * 1.298
     * 
     * @param pricePerNight Base room rate per night
     * @param days Number of nights
     * @return Total bill amount including taxes
     */
    public static double calculateTotal(double pricePerNight, int days) {
        double roomCharge = pricePerNight * days;
        double serviceCharge = roomCharge * SERVICE_CHARGE_RATE;
        double subtotal = roomCharge + serviceCharge;
        double gst = subtotal * GST_RATE;
        return subtotal + gst;
    }

    /**
     * Calculate just the multiplier for convenience (useful for UI previews).
     * Returns 1.298 (= 1.1 * 1.18)
     */
    public static double getTotalMultiplier() {
        return (1.0 + SERVICE_CHARGE_RATE) * (1.0 + GST_RATE);
    }

    /**
     * Break down bill for display purposes.
     */
    public static class BillBreakdown {
        public double roomCharges;
        public double serviceCharge;
        public double gst;
        public double total;

        public BillBreakdown(double pricePerNight, int days) {
            this.roomCharges = pricePerNight * days;
            this.serviceCharge = this.roomCharges * SERVICE_CHARGE_RATE;
            this.gst = (this.roomCharges + this.serviceCharge) * GST_RATE;
            this.total = this.roomCharges + this.serviceCharge + this.gst;
        }
    }
}
