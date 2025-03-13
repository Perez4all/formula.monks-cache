package monks.formula;

import monks.formula.cache.OrderCache;
import monks.formula.model.Order;
import monks.formula.resource.SampleOrderGenerator;

import java.util.List;

public class Main {
    public static void main(String[] args) {
        OrderCache orderCache = new OrderCache();
        List<Order> orders = SampleOrderGenerator.generateSpecificSampleOrders();
        for(Order o : orders) {
            orderCache.addOrder(o);
        }

        System.out.println("\n --- EXAMPLE 1 ---\n -SUCCESS-");

        System.out.println("SecId1: " + orderCache.getMatchingSizeForSecurity("SecId1"));

        System.out.println("SecId2: " + orderCache.getMatchingSizeForSecurity("SecId2"));

        System.out.println("SecId3: " + orderCache.getMatchingSizeForSecurity("SecId3"));

        System.out.println("\n --- CANCELING ORDERS cancelOrdersForSecIdWithMinimumQty ---\n");

        orderCache.cancelOrdersForSecIdWithMinimumQty("SecId2", 1000);

        System.out.println("SecId1: " + orderCache.getMatchingSizeForSecurity("SecId1"));

        System.out.println("SecId2: " + orderCache.getMatchingSizeForSecurity("SecId2"));

        System.out.println("SecId3: " + orderCache.getMatchingSizeForSecurity("SecId3"));


        /* Order Cache 2 - Sample 2 */

        OrderCache orderCache2 = new OrderCache();
        List<Order> orders2 = SampleOrderGenerator.generateOrdersExample2();
        for(Order o : orders2) {
            orderCache2.addOrder(o);
        }

        System.out.println("\n --- EXAMPLE 2 ---\n -SUCCESS-");

        System.out.println("SecId1: " + orderCache2.getMatchingSizeForSecurity("SecId1"));

        System.out.println("SecId2: " + orderCache2.getMatchingSizeForSecurity("SecId2"));

        System.out.println("SecId3: " + orderCache2.getMatchingSizeForSecurity("SecId3"));

        orderCache2.cancelOrdersForUser("User5");
        orderCache2.cancelOrdersForUser("User1");
        orderCache2.cancelOrdersForUser("User3");

        System.out.println("\n --- CANCELING ORDERS FOR USER ---\n");

        System.out.println("SecId1: " + orderCache2.getMatchingSizeForSecurity("SecId1"));

        System.out.println("SecId2: " + orderCache2.getMatchingSizeForSecurity("SecId2"));

        System.out.println("SecId3: " + orderCache2.getMatchingSizeForSecurity("SecId3"));

        /* Order Cache 3 - Sample 3 */

        OrderCache orderCache3 = new OrderCache();
        List<Order> orders3 = SampleOrderGenerator.generateOrdersExample3();
        for(Order o : orders3) {
            orderCache3.addOrder(o);
        }

        System.out.println("\n --- EXAMPLE 3 ---\n -SUCCESS-");

        System.out.println("SecId1: " + orderCache3.getMatchingSizeForSecurity("SecId1"));

        System.out.println("SecId2: " + orderCache3.getMatchingSizeForSecurity("SecId2"));

        System.out.println("SecId3: " + orderCache3.getMatchingSizeForSecurity("SecId3"));

        System.out.println("\n --- CANCELING ORDERS ---\n");

        orderCache3.cancelOrder("OrdId1");
        orderCache3.cancelOrder("OrdId6");
        orderCache3.cancelOrder("OrdId7");


        System.out.println("SecId1: " + orderCache3.getMatchingSizeForSecurity("SecId1"));

        System.out.println("SecId2: " + orderCache3.getMatchingSizeForSecurity("SecId2"));

        System.out.println("SecId3: " + orderCache3.getMatchingSizeForSecurity("SecId3"));

    }
}