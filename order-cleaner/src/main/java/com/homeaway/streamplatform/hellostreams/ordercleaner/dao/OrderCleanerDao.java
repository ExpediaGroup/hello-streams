package com.homeaway.streamplatform.hellostreams.ordercleaner.dao;

import com.google.common.base.Preconditions;
import com.homeaway.streamplatform.hellostreams.OrderDeleted;
import com.homeaway.streamplatform.hellostreams.ordercleaner.OrderCleanerUtils;
import com.homeaway.streamplatform.hellostreams.ordercleaner.model.Order;
import com.homeaway.streamplatform.hellostreams.ordercleaner.processor.OrderCleaner;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
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
@DependsOn({"orderCleaner"})
public class OrderCleanerDao {
    @Value("${processor.order.commands.stream}")
    private String orderCommandsStream;

    @Value("${processor.write.wait.timeout}")
    private long writeWaitTimeout;

    private KafkaProducer<String, OrderDeleted> kafkaOrderEventProducer;
    private OrderCleaner orderCleaner;

    public OrderCleanerDao(@Autowired KafkaProducer<String, OrderDeleted> kafkaOrderEventProducer,
                           @Autowired OrderCleaner orderCleaner) {
        Preconditions.checkNotNull(kafkaOrderEventProducer, "kafkaOrderEventProducer cannot be null");
        Preconditions.checkNotNull(orderCleaner, "orderCleaner cannot be null");
        this.kafkaOrderEventProducer = kafkaOrderEventProducer;
        this.orderCleaner = orderCleaner;
    }

    public List<Order> getOrders() {
        return orderCleaner.getAllOrders().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @SuppressWarnings("Duplicates")
    private Order toDTO(com.homeaway.streamplatform.hellostreams.Order orderAvro) {
        Order order = new Order();

        order.setOrderId(orderAvro.getOrderId());
        order.setId(orderAvro.getId());
        order.setCustomerId(orderAvro.getCustomerId());
        order.setItem(orderAvro.getItem());
        order.setState(orderAvro.getState());
        order.setCreated(toDTOTime(orderAvro.getCreated()));
        order.setUpdated(toDTOTime(orderAvro.getUpdated()));

        return order;
    }

    private ZonedDateTime toDTOTime(DateTime avroTime) {
        return ZonedDateTime.ofInstant(Instant.ofEpochMilli(avroTime.getMillis()),
                OrderCleanerUtils.UTC_ZONE_ID);
    }

    public Order deleteOrder(String orderId) {
        com.homeaway.streamplatform.hellostreams.Order order = orderCleaner.getOrder(orderId);
        OrderDeleted orderDeletedEvent = createOrderDeleted(orderId);
        send(orderDeletedEvent);
        return toDTO(order);
    }

    private void send(com.homeaway.streamplatform.hellostreams.OrderDeleted orderDeletedEvent) {
        try {
            log.info("Writing orderDeleted={} to kafka.", orderDeletedEvent);
            Future<RecordMetadata> resultFuture = kafkaOrderEventProducer.send(createProducerRecord(orderDeletedEvent));
            // sync wait for response
            resultFuture.get(writeWaitTimeout, TimeUnit.MILLISECONDS);

            // read your writes!!
            waitForDelete(orderDeletedEvent.getOrderId());
        } catch (Exception e) {
            throw new IllegalStateException("Could not write to kafka.", e);
        }
    }

    private ProducerRecord<String, OrderDeleted> createProducerRecord(com.homeaway.streamplatform.hellostreams.OrderDeleted  orderDeletedEvent) {
        return new ProducerRecord<>(
                orderCommandsStream, null,
                orderDeletedEvent.getCreated().getMillis(),
                orderDeletedEvent.getOrderId(), // use order key to make order aggregate easier!!
                orderDeletedEvent);
    }

    private void waitForDelete(String orderId) {
        // TODO - move hardcode wait time into property
        long timeout = System.currentTimeMillis() + 30000;
        boolean found;
        do {
            try { Thread.sleep(10); } catch (InterruptedException ignored) {}
            com.homeaway.streamplatform.hellostreams.Order order = orderCleaner.getOrder(orderId);
            found = (order!=null);
        } while(System.currentTimeMillis() < timeout && found);
    }

    private com.homeaway.streamplatform.hellostreams.OrderDeleted createOrderDeleted(String orderId) {
        return com.homeaway.streamplatform.hellostreams.OrderDeleted.newBuilder()
                .setId(UUID.randomUUID().toString())
                .setOrderId(orderId)
                .setCreated(DateTime.now())
                .build();
    }
}
