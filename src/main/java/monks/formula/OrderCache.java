package monks.formula;

import java.util.List;

class Order {

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
}

// Provide an implementation for the OrderCacheInterface interface.
// Your implementation class should hold all relevant data structures you think
// are needed.
interface OrderCacheInterface {

    // Implement the 6 methods below, do not alter signatures

    // Add order to the cache
    void addOrder(Order order);

    // Remove order with this unique order id from the cache
    void cancelOrder(String orderId);

    // Remove all orders in the cache for this user
    void cancelOrdersForUser(String user);

    // Remove all orders in the cache for this security with qty >= minQty
    void cancelOrdersForSecIdWithMinimumQty(String securityId, int minQty);

    // Return the total qty that can match for the security id
    int getMatchingSizeForSecurity(String securityId);

    // Return all orders in cache as a list
    List<Order> getAllOrders();
}
