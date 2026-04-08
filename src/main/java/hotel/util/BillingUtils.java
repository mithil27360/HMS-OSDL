package hotel.util;


public class BillingUtils {
    
    
    public static final double SERVICE_CHARGE_RATE = 0.10;    
    public static final double GST_RATE = 0.18;               

    
    public static double calculateTotal(double pricePerNight, int days) {
        double roomCharge = pricePerNight * days;
        double serviceCharge = roomCharge * SERVICE_CHARGE_RATE;
        double subtotal = roomCharge + serviceCharge;
        double gst = subtotal * GST_RATE;
        return subtotal + gst;
    }

    
    public static double getTotalMultiplier() {
        return (1.0 + SERVICE_CHARGE_RATE) * (1.0 + GST_RATE);
    }

    
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
