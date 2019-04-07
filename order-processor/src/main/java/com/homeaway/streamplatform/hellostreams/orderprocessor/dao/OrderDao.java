package com.homeaway.streamplatform.hellostreams.orderprocessor.dao;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.homeaway.streamplatform.hellostreams.orderprocessor.OrderProcessorUtils;
import com.homeaway.streamplatform.hellostreams.orderprocessor.model.Order;
import com.homeaway.streamplatform.hellostreams.orderprocessor.model.OrderPlaced;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class OrderDao {
    // TODO - remove when tied to kafka persistence
    private static final List<Order> TEST_ORDERS = Lists.newArrayList();

    private KafkaProducer<String, SpecificRecord> kafkaOrderEventProducer;

    @Value("${order-processor.order.commands.stream}")
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
        com.homeaway.streamplatform.hellostreams.OrderPlaced orderPlacedEvent = createOrderPlaced(customerId, item);
        send(orderPlacedEvent);

        // TODO - remove when orderDao is wired up
        Order order = createDummyOrder(orderPlacedEvent.getId(), orderPlacedEvent.getOrderId(),
                customerId, item,"PLACED");
        log.info("Adding order to mock dao. order={}", order.toString());
        TEST_ORDERS.add(order);
        return toDTO(orderPlacedEvent);
    }

    private OrderPlaced toDTO(com.homeaway.streamplatform.hellostreams.OrderPlaced orderPlaced) {
        OrderPlaced orderPlacedDTO = new OrderPlaced();
        orderPlacedDTO.setId(orderPlaced.getId());
        orderPlacedDTO.setOrderId(orderPlaced.getOrderId());
        orderPlacedDTO.setCustomerId(orderPlaced.getCustomerId());
        orderPlacedDTO.setItem(orderPlaced.getItem());
        orderPlacedDTO.setCreated(ZonedDateTime.ofInstant(Instant.ofEpochMilli(orderPlaced.getCreated().getMillis()),
                OrderProcessorUtils.UTC_ZONE_ID));
        return orderPlacedDTO;
    }

    private void send(com.homeaway.streamplatform.hellostreams.OrderPlaced orderPlacedEvent) {
        try {
            log.info("Writing {} to kafka.", orderPlacedEvent.toString());
            Future<RecordMetadata> resultFuture = kafkaOrderEventProducer.send(new ProducerRecord<>(
                    orderCommandsStream, null,
                    orderPlacedEvent.getCreated().getMillis(),
                    orderPlacedEvent.getId(),
                    orderPlacedEvent));
            // sync wait for response
            resultFuture.get(writeWaitTimeout, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            throw new IllegalStateException("Could not write to kafka.", e);
        }
    }

    private com.homeaway.streamplatform.hellostreams.OrderPlaced createOrderPlaced(String customerId, String item) {
        return com.homeaway.streamplatform.hellostreams.OrderPlaced.newBuilder()
                .setId(UUID.randomUUID().toString())
                .setOrderId(UUID.randomUUID().toString())
                .setCustomerId(customerId)
                .setItem(item)
                .setCreated(DateTime.now())
                .build();
    }

    /** Used temporarily until we have persistence wired up */
    @SuppressWarnings("SameParameterValue")
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

    public void clearDB() {
        // TODO - remove when tied to kafka query mechanism
        TEST_ORDERS.clear();;
    }
}
