package com.homeaway.streamplatform.hellostreams.usersessionprocessor.service;

import com.google.common.base.Preconditions;
import com.homeaway.streamplatform.hellostreams.usersessionprocessor.dao.CustomerDao;
import com.homeaway.streamplatform.hellostreams.usersessionprocessor.model.Customer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class CustomerService {
    private final CustomerDao customerDao;

    public CustomerService(@Autowired CustomerDao customerDao) {
        Preconditions.checkNotNull(customerDao, "customerDao cannot be null");
        this.customerDao = customerDao;
    }

    public Customer getCustomer(String customerId) { return customerDao.getCustomer(customerId); }

    public Customer lookupCustomer(String username) {
        Customer customer = customerDao.lookupCustomer(username);
        if (customer == null) {
            UUID uuid = UUID.randomUUID();
            String id = uuid.toString();
            customerDao.insertCustomer(id, username);
            customer = customerDao.lookupCustomer(username);
        }
        return customer;
    }
}






