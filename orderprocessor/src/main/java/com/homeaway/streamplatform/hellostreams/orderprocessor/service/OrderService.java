package com.homeaway.streamplatform.hellostreams.orderprocessor.service;

import com.google.common.base.Preconditions;
import com.homeaway.streamplatform.hellostreams.orderprocessor.dao.OrderDao;
import com.homeaway.streamplatform.hellostreams.orderprocessor.model.Order;
import com.homeaway.streamplatform.hellostreams.orderprocessor.model.OrderPlaced;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrderService {
    private OrderDao orderDao;

    public OrderService(OrderDao orderDao) {
        Preconditions.checkNotNull(orderDao, "orderDao cannot be null");
        this.orderDao = orderDao;
    }

    public List<Order> getOrdersByCustomerId(String customerId) {
        return orderDao.findOrderByCustomerId(customerId);
    }

    public List<Order> getOrders() {
        return orderDao.findAllOrders();
    }

    public OrderPlaced placeOrder(String customerId, String item) {
        return orderDao.placeOrder(customerId, item);
    }
}
