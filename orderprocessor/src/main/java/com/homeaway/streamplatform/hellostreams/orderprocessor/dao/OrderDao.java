package com.homeaway.streamplatform.hellostreams.orderprocessor.dao;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.homeaway.streamplatform.hellostreams.OrderPlacedEvent;
import com.homeaway.streamplatform.hellostreams.orderprocessor.OrderProcessorUtils;
import com.homeaway.streamplatform.hellostreams.orderprocessor.model.Order;
import com.homeaway.streamplatform.hellostreams.orderprocessor.model.OrderPlaced;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Repository
public class OrderDao {
    private static final String FIRST_ORDER_ID = "fa7ea064-77f4-4191-ba8a-472fb0e98b03";
    private static final String SECOND_ORDER_ID = "6bed3d74-f56c-43b8-b099-d80425f2ea1c";
    private static final List<Order> TEST_ORDERS = Lists.newArrayList(
            createDummyOrder(FIRST_ORDER_ID, CustomerDao.CUSTOMER_TEST_ID, "coffee", "PLACED"),
            createDummyOrder(SECOND_ORDER_ID, CustomerDao.CUSTOMER_TEST_ID, "latte", "PLACED"));

    private KafkaProducer<String, SpecificRecord> kafkaOrderEventProducer;

    @Value("${order-processor.order.commands.stream")
    private String orderCommandsStream;

    @Value("${order-processor.write.wait.timeout}")
    private long writeWaitTimeout;

    public OrderDao(@Autowired KafkaProducer<String, SpecificRecord> kafkaOrderEventProducer) {
        Preconditions.checkNotNull(kafkaOrderEventProducer, "kafkaOrderEventProducer cannot be null");
        this.kafkaOrderEventProducer = kafkaOrderEventProducer;
    }

    public List<Order> findOrderByCustomerId(String customerId) {
        Preconditions.checkNotNull(customerId, "customerId cannot be null");
        return TEST_ORDERS.stream()
                .filter(order -> customerId.equals(order.getCustomerId()))
                .collect(Collectors.toList());
    }

    public List<Order> findAllOrders() {
        return TEST_ORDERS;
    }

    public OrderPlaced placeOrder(String customerId, String item) {
        Preconditions.checkNotNull(customerId, "customerId cannot be null");
        Preconditions.checkNotNull(item, "item cannot be null");

        // write to persistent location
        OrderPlacedEvent orderPlacedEvent = createOrderPlaced(customerId, item);
        send(orderPlacedEvent);

        Order order = createDummyOrder(orderPlacedEvent.getId().toString(), orderPlacedEvent.getOrderId().toString(), customerId, item,"PLACED");
        TEST_ORDERS.add(order);
        return toDTO(orderPlacedEvent);
    }

    private OrderPlaced toDTO(OrderPlacedEvent orderPlacedEvent) {
        OrderPlaced orderPlaced = new OrderPlaced();
        orderPlaced.setId(orderPlacedEvent.getId().toString());
        orderPlaced.setOrderId(orderPlacedEvent.getOrderId().toString());
        orderPlaced.setCustomerId(orderPlacedEvent.getCustomerId().toString());
        orderPlaced.setItem(orderPlacedEvent.getItem().toString());
        orderPlaced.setCreated(ZonedDateTime.ofInstant(Instant.ofEpochMilli(orderPlacedEvent.getCreated().getMillis()),
                OrderProcessorUtils.UTC_ZONE_ID));
        return orderPlaced;
    }

    private void send(OrderPlacedEvent orderPlacedEvent) {
        try {
            Future<RecordMetadata> resultFuture = kafkaOrderEventProducer.send(new ProducerRecord<>(
                    orderCommandsStream, null,
                    orderPlacedEvent.getCreated().getMillis(),
                    orderPlacedEvent.getId().toString(),
                    orderPlacedEvent));
            // sync wait for response
            RecordMetadata recordMetadata = resultFuture.get(writeWaitTimeout, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            throw new IllegalStateException("Could not write to kafka.", e);
        }
    }

    private OrderPlacedEvent createOrderPlaced(String customerId, String item) {
        return OrderPlacedEvent.newBuilder()
                .setId(UUID.randomUUID().toString())
                .setOrderId(UUID.randomUUID().toString())
                .setCustomerId(customerId)
                .setItem(item)
                .setCreated(DateTime.now())
                .build();
    }

    /** Used temporarily until we have persistence wired up */
    private static Order createDummyOrder(String orderId, String customerId, String item, String state) {
        return createDummyOrder(UUID.randomUUID().toString(), orderId,
                customerId, item, state);
    }

    /** Used temporarily until we have persistence wired up */
    private static Order createDummyOrder(String id, String orderId, String customerId, String item, String state) {
        Order order = new Order();
        order.setId(id);
        order.setOrderId(orderId);
        order.setCustomerId(customerId);
        order.setItem(item);
        order.setState(state);
        order.setUpdated(ZonedDateTime.now(OrderProcessorUtils.UTC_ZONE_ID));
        order.setCreated(order.getUpdated());
        return order;
    }
}
