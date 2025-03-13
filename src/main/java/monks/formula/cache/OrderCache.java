package monks.formula.cache;

import monks.formula.model.Order;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
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
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class OrderCache implements OrderCacheInterface {

    private static final Logger log = Logger.getLogger(OrderCache.class.getName());

    private final Map<String, ConcurrentSkipListMap<Order, CopyOnWriteArrayList<Order>>> securityKeyWithOrdersMap = new ConcurrentHashMap<>();

    private final Map<String, AtomicInteger> matchingSizes = new ConcurrentHashMap<>();

    private final Map<String, AbstractMap.SimpleEntry<Order, AtomicBoolean>> orders = new ConcurrentHashMap<>();

    private final Comparator<Order> orderComparator;

    private final AtomicBoolean requiresUpdate = new AtomicBoolean(Boolean.FALSE);

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
        requiresUpdate.set(Boolean.TRUE);
    }

    @Override
    public synchronized List<Order> getAllOrders() {
        /* return Immutable list*/
        return orders.values().stream().map(AbstractMap.SimpleEntry::getKey).collect(Collectors.toList());
    }

    @Override
    public synchronized void cancelOrder(String orderId) {
        removeOrder(orderId);
        requiresUpdate.set(true);
    }

    @Override
    public synchronized void cancelOrdersForUser(String user) {
        orders.entrySet().removeIf(e -> e.getValue().getKey().getUser().equals(user));
        requiresUpdate.set(true);
    }

    @Override
    public synchronized void cancelOrdersForSecIdWithMinimumQty(String securityId, int minQty) {
        orders.entrySet().removeIf(e -> e.getValue().getKey().getUser().equals(securityId)
        && e.getValue().getKey().getQty() >= minQty);
        requiresUpdate.set(true);
    }

    @Override
    public synchronized int getMatchingSizeForSecurity(String securityId) {

        ConcurrentSkipListMap<Order, CopyOnWriteArrayList<Order>> orderLinkedListTreeMap = securityKeyWithOrdersMap.get(securityId);

        int matchingSize = 0;
        if(orderLinkedListTreeMap != null) {
            //Requires Update
            boolean cacheUpdated = this.verifyAndUpdateOrdersMatchCache();

            //Cache contains key
            if (matchingSizes.containsKey(securityId)) {
                //But was updated
                if(cacheUpdated){
                    //Recalculate
                    matchingSize = calculateMatchingSize(securityId, orderLinkedListTreeMap);
                } else {
                    //Return from cache
                    matchingSize = matchingSizes.get(securityId).get();
                }
            } else {
                //Calculate new
                matchingSize = calculateMatchingSize(securityId, orderLinkedListTreeMap);
            }
        }
        return matchingSize;
    }

    private int calculateMatchingSize(String securityId, ConcurrentSkipListMap<Order, CopyOnWriteArrayList<Order>> orderLinkedListTreeMap) {
        int sum;
        Stream<Order> mergedMatchedOrders = getOrderStream(orderLinkedListTreeMap);

        final Map<String, Integer> matchesMap = new HashMap<>();
        matchesMap.put("matchedQty", 0);
        matchesMap.put("remainingBuyQty", 0);
        matchesMap.put("remainingSellQty", 0);

        sum = mergedMatchedOrders
                .reduce(matchesMap, matchOrdersReducer(), (a, b) -> a)
                .get("matchedQty");

        matchingSizes.put(securityId, new AtomicInteger(sum));
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

            this.updateOrdersIfDeleted();

            if(order.getSide().equalsIgnoreCase("buy")){
                ConcurrentNavigableMap<Order, CopyOnWriteArrayList<Order>> buyLowerThaSellTreeView = orderLinkedListTreeMap.headMap(order, false);
                addMatchedOrdersToTree(buyLowerThaSellTreeView, order, "sell");
            } else if (order.getSide().equalsIgnoreCase("sell")) {
                ConcurrentNavigableMap<Order, CopyOnWriteArrayList<Order>> sellLowerThanBuyTreeView = orderLinkedListTreeMap.headMap(order, false);
                addMatchedOrdersToTree(sellLowerThanBuyTreeView, order, "buy");
            }
        }

    }

    private void updateOrdersIfDeleted() {
        List<Order> ordersToRemove = securityKeyWithOrdersMap.values().stream().map(ConcurrentSkipListMap::keySet)
                .flatMap(Collection::stream).collect(Collectors.toList());
        ordersToRemove.removeAll(orders.values().stream().map(AbstractMap.SimpleEntry::getKey).toList());

        securityKeyWithOrdersMap.values()
                .parallelStream()
                .forEach(tree -> ordersToRemove.forEach(tree.keySet()::remove));
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

    private boolean verifyAndUpdateOrdersMatchCache() {
        if(requiresUpdate.get()) {
            orders.values().stream()
                    .filter(orderBooleanSimpleEntry -> !orderBooleanSimpleEntry.getValue().get())
                    .map(AbstractMap.SimpleEntry::getKey)
                    .forEach(this::matchOrdersAndCache);
            return true;
        }
        return false;
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

    private synchronized void removeOrder(String orderId){
        orders.remove(orderId);
        requiresUpdate.set(true);
    }
}