package com.homeaway.streamplatform.hellostreams.orderprocessor.service;

import com.google.common.base.Preconditions;
import com.homeaway.streamplatform.hellostreams.orderprocessor.dao.CustomerDao;
import com.homeaway.streamplatform.hellostreams.orderprocessor.dao.OrderDao;
import com.homeaway.streamplatform.hellostreams.orderprocessor.model.Customer;
import com.homeaway.streamplatform.hellostreams.orderprocessor.model.Order;
import com.homeaway.streamplatform.hellostreams.orderprocessor.model.OrderPlaced;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;

@Service
public class OrderService {
    private static HashMap<String,String> VALID_INVENTORY = new HashMap<>();
    private CustomerDao customerDao;
    private OrderDao orderDao;

    static {
        VALID_INVENTORY.put("Brewed Coffee", "Brewed Coffee");
        VALID_INVENTORY.put("Espresso", "Espresso");
        VALID_INVENTORY.put("Latte", "Latte");
        VALID_INVENTORY.put("Ice Coffee", "Ice Coffee");
    }

    public OrderService(@Autowired CustomerDao customerDao,
            @Autowired OrderDao orderDao) {
        Preconditions.checkNotNull(customerDao, "customerDao cannot be null");
        Preconditions.checkNotNull(orderDao, "orderDao cannot be null");

        this.customerDao = customerDao;
        this.orderDao = orderDao;
    }

    public List<Order> getOrdersByCustomerId(String customerId) {
        return orderDao.findOrderByCustomerId(customerId);
    }

    public List<Order> getOrders() {
        return orderDao.findAllOrders();
    }

    public OrderPlaced placeOrder(String customerId, String item) {
        // Business Logic goes here

        // verify customerId is correct
        Customer customer = customerDao.getCustomer(customerId);
        Preconditions.checkState(customer != null, "customerId=%s is non-existent", customerId);

        // verify item is correct
        Preconditions.checkState( VALID_INVENTORY.containsKey(item), "item=%s is invalid", item);

        return orderDao.placeOrder(customerId, item);
    }
}
