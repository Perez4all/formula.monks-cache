package monks.formula.resource;

import monks.formula.model.Order;

import java.util.ArrayList;
import java.util.List;

public final class SampleOrderGenerator {

    public static List<Order> generateSpecificSampleOrders() {
        List<Order> orders = new ArrayList<>();
        orders.add(new Order("OrdId1", "SecId1", "Buy", 1000, "User1", "CompanyA"));
        orders.add(new Order("OrdId2", "SecId2", "Sell", 3000, "User2", "CompanyB"));
        orders.add(new Order("OrdId3", "SecId1", "Sell", 500, "User3", "CompanyA"));
        orders.add(new Order("OrdId4", "SecId2", "Buy", 600, "User4", "CompanyC"));
        orders.add(new Order("OrdId5", "SecId2", "Buy", 100, "User5", "CompanyB"));
        orders.add(new Order("OrdId6", "SecId3", "Buy", 1000, "User6", "CompanyD"));
        orders.add(new Order("OrdId7", "SecId2", "Buy", 2000, "User7", "CompanyE"));
        orders.add(new Order("OrdId8", "SecId2", "Sell", 5000, "User8", "CompanyE"));
        return orders;
    }

    public static List<Order> generateOrdersExample2() {
        List<Order> orders = new ArrayList<>();

        orders.add(new Order("OrdId1", "SecId1", "Sell", 100, "User10", "Company2"));
        orders.add(new Order("OrdId2", "SecId3", "Sell", 200, "User8", "Company2"));
        orders.add(new Order("OrdId3", "SecId1", "Buy", 300, "User13", "Company2"));
        orders.add(new Order("OrdId4", "SecId2", "Sell", 400, "User12", "Company2"));
        orders.add(new Order("OrdId5", "SecId3", "Sell", 500, "User7", "Company2"));
        orders.add(new Order("OrdId6", "SecId3", "Buy", 600, "User3", "Company1"));
        orders.add(new Order("OrdId7", "SecId1", "Sell", 700, "User10", "Company2"));
        orders.add(new Order("OrdId8", "SecId1", "Sell", 800, "User2", "Company1"));
        orders.add(new Order("OrdId9", "SecId2", "Buy", 900, "User6", "Company2"));
        orders.add(new Order("OrdId10", "SecId2", "Sell", 1000, "User5", "Company1"));
        orders.add(new Order("OrdId11", "SecId1", "Sell", 1100, "User13", "Company2"));
        orders.add(new Order("OrdId12", "SecId2", "Buy", 1200, "User9", "Company2"));
        orders.add(new Order("OrdId13", "SecId1", "Sell", 1300, "User1", "Company1"));

        return orders;
    }

    public static List<Order> generateOrdersExample3() {
        List<Order> orders = new ArrayList<>();

        orders.add(new Order("OrdId1", "SecId3", "Sell", 100, "User1", "Company1"));
        orders.add(new Order("OrdId2", "SecId3", "Sell", 200, "User3", "Company2"));
        orders.add(new Order("OrdId3", "SecId1", "Buy", 300, "User2", "Company1"));
        orders.add(new Order("OrdId4", "SecId3", "Sell", 400, "User5", "Company2"));
        orders.add(new Order("OrdId5", "SecId2", "Sell", 500, "User2", "Company1"));
        orders.add(new Order("OrdId6", "SecId2", "Buy", 600, "User3", "Company2"));
        orders.add(new Order("OrdId7", "SecId2", "Sell", 700, "User1", "Company1"));
        orders.add(new Order("OrdId8", "SecId1", "Sell", 800, "User2", "Company1"));
        orders.add(new Order("OrdId9", "SecId1", "Buy", 900, "User5", "Company2"));
        orders.add(new Order("OrdId10", "SecId1", "Sell", 1000, "User1", "Company1"));
        orders.add(new Order("OrdId11", "SecId2", "Sell", 1100, "User6", "Company2"));

        return orders;
    }

}