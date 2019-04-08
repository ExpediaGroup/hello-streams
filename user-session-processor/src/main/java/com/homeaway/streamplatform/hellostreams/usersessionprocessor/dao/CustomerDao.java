package com.homeaway.streamplatform.hellostreams.usersessionprocessor.dao;

import com.homeaway.streamplatform.hellostreams.usersessionprocessor.UserSessionProcessorUtils;
import com.homeaway.streamplatform.hellostreams.usersessionprocessor.model.Customer;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class CustomerDao {
    public static final String CUSTOMER_TEST_ID = "1819916e-0fd2-411b-9832-a0eb62348c17";

    private static final Map<String, Customer> TEST_CUSTOMER_CACHE = new ConcurrentHashMap<>(){
        {
            put(CUSTOMER_TEST_ID, createTestCustomer(CUSTOMER_TEST_ID, "neoword")); // seed the database with a user
        }
    };

    public Customer getCustomer(String customerId) { return TEST_CUSTOMER_CACHE.get(customerId); }

    public Customer lookupCustomer(String username) {
        for (Customer c : TEST_CUSTOMER_CACHE.values()) {
            if (c.getUsername().equals(username)) {
                return c;
            }
        }
        return null;
    }

    public void insertCustomer(String customerId, String username) {
        TEST_CUSTOMER_CACHE.put(customerId, createTestCustomer(customerId, username));
    }

    private static Customer createTestCustomer(String customerId, String username) {
        Customer customer = new Customer();
        customer.setId(customerId);
        customer.setUsername(username);
        customer.setState("CREATED");
        customer.setUpdated(ZonedDateTime.now(UserSessionProcessorUtils.UTC_ZONE_ID));
        customer.setCreated(customer.getUpdated());
        return customer;
    }

}

