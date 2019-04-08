package com.homeaway.streamplatform.hellostreams.usersessionprocessor.resolvers;

import com.coxautodev.graphql.tools.GraphQLQueryResolver;
import com.google.common.base.Preconditions;
import com.homeaway.streamplatform.hellostreams.usersessionprocessor.model.Customer;
import com.homeaway.streamplatform.hellostreams.usersessionprocessor.service.CustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

@SuppressWarnings("unused")
@Component
public class Query implements GraphQLQueryResolver {
    private CustomerService customerService;

    public Query(@Autowired CustomerService customerService) {
        Preconditions.checkNotNull(customerService, "customerService cannot be null");
        this.customerService = customerService;
    }

    public Optional<Customer> getCustomer(String customerId) {
        return Optional.ofNullable(customerService.getCustomer(customerId));
    }

    public Optional<Customer> lookupCustomer(String username) {
        return Optional.ofNullable(customerService.lookupCustomer(username));
    }
}
