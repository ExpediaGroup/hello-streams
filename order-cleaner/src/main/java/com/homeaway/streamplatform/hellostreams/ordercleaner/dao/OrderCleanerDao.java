package com.homeaway.streamplatform.hellostreams.ordercleaner.dao;

import com.homeaway.streamplatform.hellostreams.ordercleaner.model.Order;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class OrderCleanerDao {
    public List<Order> getOrders() {
        // TBD
        return null;
    }

    public Order deleteOrder(String orderId) {
        // TBD
        return null;
    }
}
