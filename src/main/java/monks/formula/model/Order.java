package monks.formula.model;

import java.lang.reflect.Field;
import java.util.ArrayList;

public class Order {

    private final String orderId;
    private final String securityId;
    private final String side;
    private final int qty;
    private final String user;
    private final String company;

    // Do not alter the signature of this constructor
    public Order(String orderId, String securityId, String side, int qty, String user, String company) {
        this.orderId = orderId;
        this.securityId = securityId;
        this.side = side;
        this.qty = qty;
        this.user = user;
        this.company = company;
    }

    // Do not alter these accessor methods
    public String getOrderId() { return orderId; }
    public String getSecurityId() { return securityId; }
    public String getSide() { return side; }
    public int getQty() { return qty; }
    public String getUser() { return user; }
    public String getCompany() { return company; }

    @Override
    public String toString() {
        ArrayList<String> attributes = new ArrayList<>();
        Field[] fields = this.getClass().getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);
            try {
                attributes.add(field.getName() + ": " + field.get(this));
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        return "{ " + String.join(", ", attributes)+ " }";
    }
}

