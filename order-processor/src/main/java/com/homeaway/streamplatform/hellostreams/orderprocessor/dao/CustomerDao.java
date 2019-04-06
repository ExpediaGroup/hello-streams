package com.homeaway.streamplatform.hellostreams.orderprocessor.dao;

import com.google.common.base.Preconditions;
import com.homeaway.streamplatform.hellostreams.orderprocessor.OrderProcessorUtils;
import com.homeaway.streamplatform.hellostreams.orderprocessor.model.Customer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

@Repository
public class CustomerDao {
    public static final String CUSTOMER_TEST_ID = "1819916e-0fd2-411b-9832-a0eb62348c17";

    private static final Map<String,Customer> TEST_CUSTOMER_CACHE = new HashMap<>(){
        {
            put(CUSTOMER_TEST_ID, createTestCustomer(CUSTOMER_TEST_ID, "neoword")); // seed the database with a user
        }
    };


    public Customer getCustomer(String customerId) { return TEST_CUSTOMER_CACHE.get(customerId); }

    public void insertCustomer(String customerId, String username) {
        TEST_CUSTOMER_CACHE.put(customerId, createTestCustomer(customerId, username));
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

}

