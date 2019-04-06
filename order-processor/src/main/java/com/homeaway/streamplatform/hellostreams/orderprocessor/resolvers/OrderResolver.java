package com.homeaway.streamplatform.hellostreams.orderprocessor.resolvers;

import com.coxautodev.graphql.tools.GraphQLResolver;
import com.google.common.base.Preconditions;
import com.homeaway.streamplatform.hellostreams.orderprocessor.model.Customer;
import com.homeaway.streamplatform.hellostreams.orderprocessor.model.Order;
import com.homeaway.streamplatform.hellostreams.orderprocessor.service.CustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * GraphQL Resolver for Orders.
 */
@SuppressWarnings("unused")
@Component
public class OrderResolver implements GraphQLResolver<Order> {
    private CustomerService customerService;

    public OrderResolver(@Autowired CustomerService customerService) {
        Preconditions.checkNotNull(customerService, "customerService cannot be null");

        this.customerService = customerService;
    }

    public Customer getCustomer(Order order) {return customerService.getCustomer(order.getCustomerId()); }
}
