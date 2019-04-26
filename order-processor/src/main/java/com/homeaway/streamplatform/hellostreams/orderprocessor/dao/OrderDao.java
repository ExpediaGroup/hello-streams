package com.homeaway.streamplatform.hellostreams.orderprocessor.dao;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.homeaway.streamplatform.hellostreams.orderprocessor.OrderProcessorUtils;
import com.homeaway.streamplatform.hellostreams.orderprocessor.model.Order;
import com.homeaway.streamplatform.hellostreams.orderprocessor.model.OrderPlaced;
import com.homeaway.streamplatform.hellostreams.orderprocessor.processor.OrderStreamProcessor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

@Repository
@DependsOn("orderStreamProcessor")
@Slf4j
public class OrderDao {
    private KafkaProducer<String, SpecificRecord> kafkaOrderEventProducer;
    private final OrderStreamProcessor orderStreamProcessor;
    private ReadOnlyKeyValueStore<String, com.homeaway.streamplatform.hellostreams.Order> orderStore;

    @Value("${order-processor.order.commands.stream}")
    private String orderCommandsStream;

    @Value("${order-processor.write.wait.timeout}")
    private long writeWaitTimeout;

    public OrderDao(@Autowired KafkaProducer<String, SpecificRecord> kafkaOrderEventProducer,
                    @Autowired OrderStreamProcessor orderStreamProcessor) {
        Preconditions.checkNotNull(kafkaOrderEventProducer, "kafkaOrderEventProducer cannot be null");
        Preconditions.checkNotNull(orderStreamProcessor, "orderStreamProcessor cannot be null");
        this.kafkaOrderEventProducer = kafkaOrderEventProducer;
        this.orderStreamProcessor = orderStreamProcessor;
    }

    @PostConstruct
    public void init() {
        orderStore = orderStreamProcessor.getOrderStore();
    }

    @PreDestroy
    public void close() {
        log.info("Shutting down kafkaOrderEventProducer");
        kafkaOrderEventProducer.flush();
        kafkaOrderEventProducer.close(Duration.ofSeconds(30));
    }

    public List<Order> findAllOrders() {
        List<Order> orders = Lists.newArrayList();
        try ( KeyValueIterator<String, com.homeaway.streamplatform.hellostreams.Order> rawOrders
                      = orderStore.all() ) {
            rawOrders.forEachRemaining( keyValue -> orders.add(toDTO(keyValue.value)) );
        }
        return orders;
    }

    public OrderPlaced placeOrder(String customerId, String item) {
        Preconditions.checkNotNull(customerId, "customerId cannot be null");
        Preconditions.checkNotNull(item, "item cannot be null");

        // write to persistent location
        com.homeaway.streamplatform.hellostreams.OrderPlaced orderPlacedEvent = createOrderPlaced(customerId, item);
        send(orderPlacedEvent);

        return toDTO(orderPlacedEvent);
    }

    private void waitForWrite(String id, String orderId) {
        // TODO - move hardcode wait time into property
        long timeout = System.currentTimeMillis() + 30000;
        boolean found;
        do {
            try { Thread.sleep(10); } catch (InterruptedException ignored) {}
            com.homeaway.streamplatform.hellostreams.Order order = orderStore.get(orderId);
            found = order!=null
                    && order.getId().equals(id);
        } while(System.currentTimeMillis() < timeout && !found);
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

    private OrderPlaced toDTO(com.homeaway.streamplatform.hellostreams.OrderPlaced orderPlacedAvro) {
        OrderPlaced orderPlaced = new OrderPlaced();
        orderPlaced.setId(orderPlacedAvro.getId());
        orderPlaced.setOrderId(orderPlacedAvro.getOrderId());
        orderPlaced.setCustomerId(orderPlacedAvro.getCustomerId());
        orderPlaced.setItem(orderPlacedAvro.getItem());
        orderPlaced.setCreated(toDTOTime(orderPlacedAvro.getCreated()));
        return orderPlaced;
    }

    private ZonedDateTime toDTOTime(DateTime avroTime) {
        return ZonedDateTime.ofInstant(Instant.ofEpochMilli(avroTime.getMillis()),
                OrderProcessorUtils.UTC_ZONE_ID);
    }

    private void send(com.homeaway.streamplatform.hellostreams.OrderPlaced orderPlacedEvent) {
        try {
            log.info("Writing orderPlaced={} to kafka.", orderPlacedEvent);
            Future<RecordMetadata> resultFuture = kafkaOrderEventProducer.send(new ProducerRecord<>(
                    orderCommandsStream, null,
                    orderPlacedEvent.getCreated().getMillis(),
                    orderPlacedEvent.getOrderId(), // use order key to make order aggregate easier!!
                    orderPlacedEvent));
            // sync wait for response
            resultFuture.get(writeWaitTimeout, TimeUnit.MILLISECONDS);

            // read your writes!!
            waitForWrite(orderPlacedEvent.getId(), orderPlacedEvent.getOrderId());
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
}
