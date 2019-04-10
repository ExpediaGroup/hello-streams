package com.homeaway.streamplatform.hellostreams.orderprocessor.resolvers;

import com.coxautodev.graphql.tools.GraphQLQueryResolver;
import com.google.common.base.Preconditions;
import com.homeaway.streamplatform.hellostreams.orderprocessor.model.Order;
import com.homeaway.streamplatform.hellostreams.orderprocessor.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@SuppressWarnings("unused")
@Component
public class Query implements GraphQLQueryResolver {
    private OrderService orderService;

    public Query(@Autowired OrderService orderService) {
        Preconditions.checkNotNull(orderService, "orderService cannot be null");

        this.orderService = orderService;
    }

    public List<Order> getOrders() { return orderService.getOrders(); }
}
