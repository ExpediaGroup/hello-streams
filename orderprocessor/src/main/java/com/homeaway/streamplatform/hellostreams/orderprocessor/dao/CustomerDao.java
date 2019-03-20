package com.homeaway.streamplatform.hellostreams.orderprocessor.dao;

import com.homeaway.streamplatform.hellostreams.orderprocessor.OrderProcessorUtils;
import com.homeaway.streamplatform.hellostreams.orderprocessor.model.Customer;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.Map;

@Repository
public class CustomerDao {
    public static final String CUSTOMER_TEST_ID = "1819916e-0fd2-411b-9832-a0eb62348c17";
    private static final Map<String,Customer> TEST_CUSTOMER_CACHE = Map.of(CUSTOMER_TEST_ID, createTestCustomer());

    public Customer getCustomer(String customerId) {
        return TEST_CUSTOMER_CACHE.get(customerId);
    }

    private static Customer createTestCustomer() {
        Customer customer = new Customer();
        customer.setId(CUSTOMER_TEST_ID);
        customer.setUsername("neoword");
        customer.setState("CREATED");
        customer.setUpdated(ZonedDateTime.now(OrderProcessorUtils.UTC_ZONE_ID));
        customer.setCreated(customer.getUpdated());
        return customer;
    }
}

