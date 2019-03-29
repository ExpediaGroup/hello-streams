package com.homeaway.streamplatform.hellostreams.orderprocessor.resolvers;

import com.coxautodev.graphql.tools.GraphQLMutationResolver;
import com.google.common.base.Preconditions;
import com.homeaway.streamplatform.hellostreams.orderprocessor.model.BeansSupplied;
import com.homeaway.streamplatform.hellostreams.orderprocessor.model.OrderPlaced;
import com.homeaway.streamplatform.hellostreams.orderprocessor.service.OrderService;
import com.homeaway.streamplatform.hellostreams.orderprocessor.service.SupplyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@SuppressWarnings("unused")
public class Mutation implements GraphQLMutationResolver {
    private final OrderService orderService;
    private final SupplyService supplyService;

    public Mutation(@Autowired OrderService orderService,
                    @Autowired SupplyService supplyService) {
        Preconditions.checkNotNull(orderService, "orderService cannot be null");
        Preconditions.checkNotNull(supplyService, "supplyService cannobt be null");
        this.orderService = orderService;
        this.supplyService = supplyService;
    }

    public OrderPlaced placeOrder(String customerId, String item) {
    return orderService.placeOrder(customerId, item);
  }

    public BeansSupplied supplyBeans(int numBeans) {
    return supplyService.supplyBeans(numBeans);
  }
}
