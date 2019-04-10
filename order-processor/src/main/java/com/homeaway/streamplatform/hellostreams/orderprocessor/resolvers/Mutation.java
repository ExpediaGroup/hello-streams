package com.homeaway.streamplatform.hellostreams.orderprocessor.resolvers;

import com.coxautodev.graphql.tools.GraphQLMutationResolver;
import com.google.common.base.Preconditions;
import com.homeaway.streamplatform.hellostreams.orderprocessor.model.OrderPlaced;
import com.homeaway.streamplatform.hellostreams.orderprocessor.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@SuppressWarnings("unused")
public class Mutation implements GraphQLMutationResolver {
    private final OrderService orderService;

    public Mutation(@Autowired OrderService orderService) {
        Preconditions.checkNotNull(orderService, "orderService cannot be null");
        this.orderService = orderService;
    }

    public OrderPlaced placeOrder(String customerId, String item) {
        return orderService.placeOrder(customerId, item);
    }
}
