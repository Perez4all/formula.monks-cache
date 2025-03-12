package monks.formula.cache;

import monks.formula.model.Order;

import java.util.List;

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
