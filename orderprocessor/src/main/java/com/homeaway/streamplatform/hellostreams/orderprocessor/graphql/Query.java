package com.homeaway.streamplatform.hellostreams.orderprocessor.graphql;

import com.coxautodev.graphql.tools.GraphQLQueryResolver;
import com.google.common.base.Preconditions;
import com.homeaway.streamplatform.hellostreams.orderprocessor.model.Customer;
import com.homeaway.streamplatform.hellostreams.orderprocessor.model.Order;
import com.homeaway.streamplatform.hellostreams.orderprocessor.service.CustomerService;
import com.homeaway.streamplatform.hellostreams.orderprocessor.service.OrderService;
import com.homeaway.streamplatform.hellostreams.orderprocessor.service.SupplyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@SuppressWarnings("unused")
@Component
public class Query implements GraphQLQueryResolver {
    private OrderService orderService;
    private CustomerService customerService;
    private SupplyService supplyService;

    public Query(@Autowired OrderService orderService,
                 @Autowired CustomerService customerService,
                 @Autowired SupplyService supplyService) {
        Preconditions.checkNotNull(orderService, "orderService cannot be null");
        Preconditions.checkNotNull(customerService, "customerService cannot be null");
        Preconditions.checkNotNull(supplyService, "supplyService cannot be null");

        this.orderService = orderService;
        this.customerService = customerService;
        this.supplyService = supplyService;
    }

    public List<Order> getOrders() {
        return orderService.getOrders();
    }

    public Optional<Customer> getCustomer(String customerId) {
        return Optional.ofNullable(customerService.getCustomer(customerId));
    }

    public int getAvailableBeans() {
        return supplyService.getAvailableBeans();
    }
}
