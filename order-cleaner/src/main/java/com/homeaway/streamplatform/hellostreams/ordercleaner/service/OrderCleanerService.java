package com.homeaway.streamplatform.hellostreams.ordercleaner.service;

import com.google.common.base.Preconditions;
import com.homeaway.streamplatform.hellostreams.ordercleaner.dao.OrderCleanerDao;
import com.homeaway.streamplatform.hellostreams.ordercleaner.model.Order;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OrderCleanerService {
    private final OrderCleanerDao orderCleanerDao;

    public OrderCleanerService(@Autowired OrderCleanerDao orderCleanerDao) {
        Preconditions.checkNotNull(orderCleanerDao, "orderCleanerDao cannot be null");
        this.orderCleanerDao = orderCleanerDao;
    }
    
    public List<Order> cleanOrders() {
        log.info("Cleaning orders...");
        List<Order> orders = orderCleanerDao.getOrders();

        // filter out the orders older than one minute and delete those
        List<Order> results = orders.stream()
                .filter(order -> order.getUpdated().plusSeconds(60).toInstant().toEpochMilli() <= System.currentTimeMillis())
                .map(order -> deleteOrder(order.getOrderId()))
                .collect(Collectors.toList());

        return results;
    }

    public Order deleteOrder(String orderId) {
        log.info("Deleting orderId={}", orderId);
        return orderCleanerDao.deleteOrder(orderId);
    }
}
