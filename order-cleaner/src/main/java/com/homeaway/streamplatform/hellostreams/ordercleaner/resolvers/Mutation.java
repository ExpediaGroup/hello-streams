package com.homeaway.streamplatform.hellostreams.ordercleaner.resolvers;

import com.coxautodev.graphql.tools.GraphQLMutationResolver;
import com.google.common.base.Preconditions;
import com.homeaway.streamplatform.hellostreams.ordercleaner.model.Order;
import com.homeaway.streamplatform.hellostreams.ordercleaner.service.OrderCleanerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@SuppressWarnings("unused")
public class Mutation implements GraphQLMutationResolver {
    private final OrderCleanerService orderCleanerService;

    public Mutation(@Autowired OrderCleanerService orderCleanerService) {
        Preconditions.checkNotNull(orderCleanerService, "orderCleanerService cannot be null");
        this.orderCleanerService = orderCleanerService;
    }

    public List<Order> cleanOrders() {
        return orderCleanerService.cleanOrders();
    }
    public Order deleteOrder(String orderId) { return orderCleanerService.deleteOrder(orderId); }
}
