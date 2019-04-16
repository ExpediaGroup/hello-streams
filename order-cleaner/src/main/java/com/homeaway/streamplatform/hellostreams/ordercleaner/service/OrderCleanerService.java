package com.homeaway.streamplatform.hellostreams.ordercleaner.service;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.homeaway.streamplatform.hellostreams.ordercleaner.dao.OrderCleanerDao;
import com.homeaway.streamplatform.hellostreams.ordercleaner.model.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrderCleanerService {
    private OrderCleanerDao orderCleanerDao;

    public OrderCleanerService(@Autowired OrderCleanerDao orderCleanerDao) {
        Preconditions.checkNotNull(orderCleanerDao, "orderCleanerDao cannot be null");

        this.orderCleanerDao = orderCleanerDao;
    }

    public List<Order> cleanOrders() {
        List<Order> orders = orderCleanerDao.getOrders();
        List<Order> results = Lists.newArrayList();
        orders.forEach( order -> results.add(deleteOrder(order.getOrderId())));
        return results;
    }

    public Order deleteOrder(String orderId) {
        return orderCleanerDao.deleteOrder(orderId);
    }
}
