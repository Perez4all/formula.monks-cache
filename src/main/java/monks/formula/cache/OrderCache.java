package monks.formula.cache;

import monks.formula.model.Order;

import java.util.AbstractMap;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class OrderCache implements OrderCacheInterface {

    private static final Logger log = Logger.getLogger(OrderCache.class.getName());

    private final Map<String, ConcurrentSkipListMap<Order, CopyOnWriteArrayList<Order>>> securityKeyWithOrdersMap = new ConcurrentHashMap<>();

    private final Map<String, AtomicInteger> matchingSizes = new ConcurrentHashMap<>();

    private final Map<String, AbstractMap.SimpleEntry<Order, AtomicBoolean>> orders = new ConcurrentHashMap<>();

    private final Comparator<Order> orderComparator; 

    public OrderCache(){
        this.orderComparator = Comparator.comparingInt(Order::getQty);
    }

    @Override
    public synchronized void addOrder(Order order) {
        orders.put(order.getOrderId(), new AbstractMap.SimpleEntry<>(order, new AtomicBoolean(false)));
        if(securityKeyWithOrdersMap.containsKey(order.getSecurityId())){
            ConcurrentSkipListMap<Order, CopyOnWriteArrayList<Order>> orderCopyOnWriteArrayListConcurrentSkipListMap = securityKeyWithOrdersMap.get(order.getSecurityId());
            orderCopyOnWriteArrayListConcurrentSkipListMap.put(order, new CopyOnWriteArrayList<>());
        } else {
            ConcurrentSkipListMap<Order, CopyOnWriteArrayList<Order>> value = new ConcurrentSkipListMap<>(orderComparator);
            securityKeyWithOrdersMap.put(order.getSecurityId(), value);
            value.put(order, new CopyOnWriteArrayList<>());
        }
    }

    @Override
    public synchronized List<Order> getAllOrders() {
        /* return Immutable list*/
        return orders.values().stream().map(AbstractMap.SimpleEntry::getKey).collect(Collectors.toList());
    }

    @Override
    public synchronized void cancelOrder(String orderId) {
        cancelOrderWithPropertyMatch(order -> order.getOrderId().equalsIgnoreCase(orderId));
        removeFromSecurityKeyWithOrdersMap(o -> o.getUser().equalsIgnoreCase(orderId));
    }

    @Override
    public synchronized void cancelOrdersForUser(String user) {
        cancelOrderWithPropertyMatch(order -> order.getUser().equalsIgnoreCase(user));
        removeFromSecurityKeyWithOrdersMap(o -> o.getUser().equalsIgnoreCase(user));
    }

    @Override
    public synchronized void cancelOrdersForSecIdWithMinimumQty(String securityId, int minQty) {
        cancelOrderWithPropertyMatch(order -> order.getSecurityId().equalsIgnoreCase(securityId) && order.getQty() <= minQty);
    }

    @Override
    public synchronized int getMatchingSizeForSecurity(String securityId) {

        int sum = 0;
        this.verifyAndPerformCacheUpdate();

        ConcurrentSkipListMap<Order, CopyOnWriteArrayList<Order>> orderLinkedListTreeMap = securityKeyWithOrdersMap.get(securityId);
        if(orderLinkedListTreeMap != null) {
            if (matchingSizes.containsKey(securityId)) {
                sum = matchingSizes.get(securityId).get();
            } else {

                Stream<Order> mergedMatchedOrders = getOrderStream(orderLinkedListTreeMap);

                sum = mergedMatchedOrders
                        .reduce(Map.of(
                                "matchedQty", 0,
                                "remainingBuyQty", 0,
                                "remainingSellQty", 0
                        ), matchOrdersReducer(), (a, b) -> a)
                        .get("matchedQty");

                matchingSizes.put(securityId, new AtomicInteger(sum));
            }
        }
        return sum;
    }

    private static Stream<Order> getOrderStream(ConcurrentSkipListMap<Order, CopyOnWriteArrayList<Order>> orderLinkedListTreeMap) {

        Function<Map.Entry<Order, CopyOnWriteArrayList<Order>>, Stream<Order>> mergeOrders = e -> {
            e.getValue().add(e.getKey());
            return e.getValue().stream();
        };

        Stream<Map.Entry<Order, CopyOnWriteArrayList<Order>>> notMatchedOrdersSkipped = orderLinkedListTreeMap
                .entrySet()
                .stream()
                .dropWhile(entry -> entry.getValue().isEmpty());

        return notMatchedOrdersSkipped
                .flatMap(mergeOrders)
                .distinct();
    }

    private void matchOrdersAndCache(Order order) {

        ConcurrentSkipListMap<Order, CopyOnWriteArrayList<Order>> orderLinkedListTreeMap = securityKeyWithOrdersMap.get(order.getSecurityId());

        if (orderLinkedListTreeMap != null) {
            if(order.getSide().equalsIgnoreCase("buy")){
                ConcurrentNavigableMap<Order, CopyOnWriteArrayList<Order>> buyLowerThaSellTreeView = orderLinkedListTreeMap.headMap(order, false);
                addMatchedOrdersToTree(buyLowerThaSellTreeView, order, "sell");
            } else if (order.getSide().equalsIgnoreCase("sell")) {
                ConcurrentNavigableMap<Order, CopyOnWriteArrayList<Order>> sellLowerThanBuyTreeView = orderLinkedListTreeMap.headMap(order, false);
                addMatchedOrdersToTree(sellLowerThanBuyTreeView, order, "buy");
            }
        }

    }

    private void addMatchedOrdersToTree(ConcurrentNavigableMap<Order, CopyOnWriteArrayList<Order>> sellLowerThanBuyTreeView, Order order, String type) {
        List<Order> ordersMatch = sellLowerThanBuyTreeView.keySet().stream()
                .filter(orderCopyOnWriteArrayList -> !orderCopyOnWriteArrayList.getCompany()
                        .equalsIgnoreCase(order.getCompany()) && orderCopyOnWriteArrayList.getSide().equalsIgnoreCase(type))
                .toList();
        ordersMatch.forEach(o -> {
            if (!orders.get(o.getOrderId()).getValue().get()) {
                CopyOnWriteArrayList<Order> linkedOrders = securityKeyWithOrdersMap.get(order.getSecurityId())
                        .get(order);
                if(linkedOrders != null) {
                    linkedOrders.add(o);
                }
                setOrderProcessMatched(o);
            }
        });
    }

    private void setOrderProcessMatched(Order order) {
        orders.get(order.getOrderId()).getValue().set(true);
    }

    private void verifyAndPerformCacheUpdate() {
        orders.values().stream()
                .filter(orderBooleanSimpleEntry -> !orderBooleanSimpleEntry.getValue().get())
                .map(AbstractMap.SimpleEntry::getKey)
                .forEach(this::matchOrdersAndCache);
    }

    private BiFunction<Map<String, Integer>, Order, Map<String, Integer>> matchOrdersReducer(){
        return (matchesMap, order) -> {
            if (order.getSide().equalsIgnoreCase("Buy")) {
                int matched = Math.min(matchesMap.get("remainingSellQty"), order.getQty());
                matchesMap.put("matchedQty", matchesMap.get("matchedQty") + matched);
                matchesMap.put("remainingSellQty", matchesMap.get("remainingSellQty") - matched);
                matchesMap.put("remainingBuyQty", matchesMap.get("remainingBuyQty") + order.getQty() - matched);
            } else if (order.getSide().equalsIgnoreCase("Sell")) {
                int matched = Math.min(matchesMap.get("remainingBuyQty"), order.getQty());
                matchesMap.put("matchedQty", matchesMap.get("matchedQty") + matched);
                matchesMap.put("remainingBuyQty", matchesMap.get("remainingBuyQty") - matched);
                matchesMap.put("remainingSellQty", matchesMap.get("remainingSellQty") + order.getQty() - matched);
            }
            return matchesMap;
        };
    }

    private void removeFromSecurityKeyWithOrdersMap(Predicate<Order> orderPredicate) {
        securityKeyWithOrdersMap.values().forEach(treeMap -> {
            treeMap.entrySet().removeIf(r -> orderPredicate.test(r.getKey()));
        });
        securityKeyWithOrdersMap.values().forEach(treeMap -> {
            treeMap.values().forEach(list -> list.removeIf(orderPredicate));
        });
    }

    private synchronized void cancelOrderWithPropertyMatch(Predicate<Order> predicate){
        orders.entrySet().removeIf(entry -> predicate.test(entry.getValue().getKey()));
    }
}