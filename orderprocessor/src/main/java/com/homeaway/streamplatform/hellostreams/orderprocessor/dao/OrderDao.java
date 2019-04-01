package com.homeaway.streamplatform.hellostreams.orderprocessor.dao;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.homeaway.streamplatform.hellostreams.orderprocessor.OrderProcessorUtils;
import com.homeaway.streamplatform.hellostreams.orderprocessor.model.Order;
import com.homeaway.streamplatform.hellostreams.orderprocessor.model.OrderPlaced;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
public class OrderDao {
    private static final String FIRST_ORDER_ID = "fa7ea064-77f4-4191-ba8a-472fb0e98b03";
    private static final String SECOND_ORDER_ID = "6bed3d74-f56c-43b8-b099-d80425f2ea1c";
    private static final List<Order> TEST_ORDERS = Lists.newArrayList(
            createOrder(FIRST_ORDER_ID, CustomerDao.CUSTOMER_TEST_ID, "coffee", "PLACED"),
            createOrder(SECOND_ORDER_ID, CustomerDao.CUSTOMER_TEST_ID, "latte", "PLACED"));

    public List<Order> findOrderByCustomerId(String customerId) {
        Preconditions.checkNotNull(customerId, "customerId cannot be null");
        return TEST_ORDERS.stream()
                .filter(order -> customerId.equals(order.getCustomerId()))
                .collect(Collectors.toList());
    }

    public List<Order> findAllOrders() {
        return TEST_ORDERS;
    }

    private static Order createOrder(String orderId, String customerId, String item, String state) {
        Order order = new Order();
        order.setId(UUID.randomUUID().toString());
        order.setOrderId(orderId);
        order.setCustomerId(customerId);
        order.setItem(item);
        order.setState(state);
        order.setUpdated(ZonedDateTime.now(OrderProcessorUtils.UTC_ZONE_ID));
        order.setCreated(order.getUpdated());
        return order;
    }

    public OrderPlaced placeOrder(String customerId, String item) {
        Preconditions.checkNotNull(customerId, "customerId cannot be null");
        Preconditions.checkNotNull(item, "item cannot be null");

        // write to persistent location
        OrderPlaced orderPlaced = createOrderPlaced(customerId, item);
        return orderPlaced;
    }

    private OrderPlaced createOrderPlaced(String customerId, String item) {
        OrderPlaced orderPlaced = new OrderPlaced();
        orderPlaced.setId(UUID.randomUUID().toString());
        orderPlaced.setOrderId(UUID.randomUUID().toString());
        orderPlaced.setCreated(ZonedDateTime.now());
        orderPlaced.setCustomerId(customerId);
        orderPlaced.setItem(item);
        orderPlaced.setCreated(ZonedDateTime.now());
        return orderPlaced;
    }
}
