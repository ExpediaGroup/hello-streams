package com.homeaway.streamplatform.hellostreams.orderprocessor.service;

import com.google.common.base.Preconditions;
import com.homeaway.streamplatform.hellostreams.orderprocessor.dao.CustomerDao;
import com.homeaway.streamplatform.hellostreams.orderprocessor.model.Customer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CustomerService {
    private final CustomerDao customerDao;

    public CustomerService(@Autowired CustomerDao customerDao) {
        Preconditions.checkNotNull(customerDao, "customerDao cannot be null");
        this.customerDao = customerDao;
    }

    public Customer getCustomer(String customerId) {
        return customerDao.getCustomer(customerId);
    }
}
