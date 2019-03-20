package com.homeaway.streamplatform.hellostreams.orderprocessor.graphql;

import com.coxautodev.graphql.tools.GraphQLResolver;
import com.google.common.base.Preconditions;
import com.homeaway.streamplatform.hellostreams.orderprocessor.model.Customer;
import com.homeaway.streamplatform.hellostreams.orderprocessor.model.Order;
import com.homeaway.streamplatform.hellostreams.orderprocessor.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * GraphQL Resolver for Orders.
 */
@SuppressWarnings("unused")
@Component
public class CustomerResolver implements GraphQLResolver<Customer> {
    private OrderService orderService;

    public CustomerResolver(@Autowired OrderService orderService) {
        Preconditions.checkNotNull(orderService, "orderService cannot be null");

        this.orderService = orderService;
    }

    public List<Order> getOrders(Customer customer) {
        return orderService.getOrdersByCustomerId(customer.getId());
    }
}
