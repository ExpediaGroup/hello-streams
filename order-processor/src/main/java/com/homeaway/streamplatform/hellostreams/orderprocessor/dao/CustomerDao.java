package com.homeaway.streamplatform.hellostreams.orderprocessor.dao;

import com.google.common.collect.Maps;
import com.homeaway.streamplatform.hellostreams.orderprocessor.OrderProcessorUtils;
import com.homeaway.streamplatform.hellostreams.orderprocessor.model.Customer;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.Map;

@Repository
public class CustomerDao {
    // TODO - replace with Customer Domain Events when available
    private static final Map<String,Customer> CUSTOMER_CACHE = Maps.newHashMap();

    public Customer getCustomer(String customerId) {
        // TODO - hack until customer domain event are available
        if(!CUSTOMER_CACHE.containsKey(customerId)) {
            // TODO - hack until customer domain event are available
            insertCustomer(customerId, customerId);
        }
        return CUSTOMER_CACHE.get(customerId);
    }

    // TODO - until customer domain event is available
    public void insertCustomer(String customerId, String username) {
        CUSTOMER_CACHE.put(customerId, createTestCustomer(customerId, username));
    }

    private static Customer createTestCustomer(String customerId, String username) {
        Customer customer = new Customer();
        customer.setId(customerId);
        customer.setUsername(username);
        customer.setState("CREATED");
        customer.setUpdated(ZonedDateTime.now(OrderProcessorUtils.UTC_ZONE_ID));
        customer.setCreated(customer.getUpdated());
        return customer;
    }

    public void clearDB() {
        CUSTOMER_CACHE.clear();
    }
}

