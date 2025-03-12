package monks.formula.cache;

import monks.formula.model.Order;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class OrderCache implements OrderCacheInterface {

    private static final Logger log = Logger.getLogger(OrderCache.class.getName());

    private final Map<String, ConcurrentSkipListMap<Order, CopyOnWriteArrayList<Order>>> securityKeyWithOrdersMap = new ConcurrentHashMap<>();

    private final Map<String, AtomicInteger> matchingSizes = new ConcurrentHashMap<>();

    private final Map<String, AbstractMap.SimpleEntry<Order, AtomicBoolean>> buyOrders = new ConcurrentHashMap<>();

    private final Comparator<Order> orderComparator; 

    public OrderCache(){
        this.orderComparator = (o1, o2) -> {
            if (o2.getQty() < o1.getQty()) {
                return -1;
            }
            if (Objects.equals(o1.getOrderId(), o2.getOrderId())) {
                return 0;
            }
            return 1;
        };
    }

    @Override
    public synchronized void addOrder(Order order) {
        if (order.getSide().equalsIgnoreCase("sell")) {
            addSellOrder(order);
        } else {
            buyOrders.put(order.getOrderId(), new AbstractMap.SimpleEntry<>(order, new AtomicBoolean(false)));
        }
    }

    private void addBuyOrder(Order order) {
        ConcurrentSkipListMap<Order, CopyOnWriteArrayList<Order>> orderLinkedListTreeMap = securityKeyWithOrdersMap.get(order.getSecurityId());

        if (orderLinkedListTreeMap != null) {

            SortedMap<Order, CopyOnWriteArrayList<Order>> x = orderLinkedListTreeMap.headMap(order);

            AtomicBoolean processed = new AtomicBoolean(false);

            x.entrySet().stream()
                    .sorted(Comparator.comparing(o -> o.getKey().getOrderId()))
                    .forEachOrdered(entry -> {
                        Order key = entry.getKey();
                        if (key != null && !processed.get()) {
                            orderLinkedListTreeMap.compute(key, (o, p) -> {
                                if (p == null) {
                                    p = new CopyOnWriteArrayList<>();
                                }
                                if (!key.getCompany().equalsIgnoreCase(order.getCompany())) {
                                    p.add(order);
                                    processed.set(true);
                                }
                                return p;
                            });
                        }
                    });
        }

    }

    private void addSellOrder(Order order) {
        ConcurrentSkipListMap<Order, CopyOnWriteArrayList<Order>> existingTreeMap = securityKeyWithOrdersMap.computeIfAbsent(order.getSecurityId(), k -> new ConcurrentSkipListMap<>(orderComparator));
        existingTreeMap.put(order, new CopyOnWriteArrayList<>());
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

    private void removeFromSecurityKeyWithOrdersMap(Predicate<Order> orderPredicate) {
        securityKeyWithOrdersMap.values().forEach(treeMap -> {
            treeMap.entrySet().removeIf(r -> orderPredicate.test(r.getKey()));
        });
        securityKeyWithOrdersMap.values().forEach(treeMap -> {
            treeMap.values().forEach(list -> list.removeIf(orderPredicate));
        });
    }

    @Override
    public synchronized void cancelOrdersForSecIdWithMinimumQty(String securityId, int minQty) {
        cancelOrderWithPropertyMatch(order -> order.getSecurityId().equalsIgnoreCase(securityId) && order.getQty() <= minQty);
    }

    @Override
    public synchronized int getMatchingSizeForSecurity(String securityId) {

        int sum = 0;
        buyOrders.values().stream()
                .filter(orderBooleanSimpleEntry -> !orderBooleanSimpleEntry.getValue().get())
                .map(AbstractMap.SimpleEntry::getKey)
                .peek(this::addBuyOrder)
                .forEach(o -> buyOrders.get(o.getOrderId()).getValue().set(true));

        ConcurrentSkipListMap<Order, CopyOnWriteArrayList<Order>> orderLinkedListTreeMap = securityKeyWithOrdersMap.get(securityId);
        if(orderLinkedListTreeMap != null) {
            if (matchingSizes.containsKey(securityId)) {
                sum = matchingSizes.get(securityId).get();
            } else {
                sum = orderLinkedListTreeMap
                        .values()
                        .stream()
                        .flatMap(Collection::stream)
                        .mapToInt(Order::getQty)
                        .sum();
                matchingSizes.put(securityId, new AtomicInteger(sum));
            }
        }
        return sum;
    }

    @Override
    public synchronized List<Order> getAllOrders() {
        /* return Immutable list*/
        return buyOrders.values().stream().map(AbstractMap.SimpleEntry::getKey).collect(Collectors.toList());
    }

    private synchronized void cancelOrderWithPropertyMatch(Predicate<Order> predicate){
        buyOrders.entrySet().removeIf(entry -> predicate.test(entry.getValue().getKey()));
    }
}