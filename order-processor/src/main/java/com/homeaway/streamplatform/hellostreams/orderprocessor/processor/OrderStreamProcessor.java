package com.homeaway.streamplatform.hellostreams.orderprocessor.processor;

import com.homeaway.streamplatform.hellostreams.Order;
import com.homeaway.streamplatform.hellostreams.OrderAccepted;
import com.homeaway.streamplatform.hellostreams.OrderDeleted;
import com.homeaway.streamplatform.hellostreams.OrderPlaced;
import com.homeaway.streamplatform.hellostreams.OrderRejected;
import com.homeaway.streamplatform.hellostreams.orderprocessor.processor.StreamRegistry.StreamMetadata;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.time.Duration;
import java.util.Properties;

@Component
@Slf4j
public class OrderStreamProcessor {
    private static final String ORDER_STORE_NAME = "orders";

    @Resource
    private StreamRegistry streamRegistry;

    @Value("${order-processor.id}")
    private String orderProcessorAppId;

    @Value("${order-processor.bootstrap.servers}")
    private String bootstrapServers;

    @Value("${order-processor.order.commands.stream}")
    private String orderCommandsStream;

    @Value("${order-processor.orders.stream}")
    private String ordersStream;

    private KafkaStreams orderStreamProcessor;

    public OrderStreamProcessor() {}

    @SuppressWarnings({"WeakerAccess", "unchecked"})
    public StreamsBuilder getStreamsBuilder() {
        StreamsBuilder builder = new StreamsBuilder();

        // lookup command (orderCommands) and domain (orders) events
        StreamMetadata<String, SpecificRecord> orderCommandsMeta = streamRegistry.lookupStream(orderCommandsStream);
        StreamMetadata<String, Order> ordersMeta = streamRegistry.lookupStream(ordersStream);

        // setup input events - multi-type
        KStream<String, SpecificRecord> orderCommandEvents =
                builder.stream(orderCommandsMeta.getName(),
                        Consumed.with(orderCommandsMeta.getKeySerde(),
                                orderCommandsMeta.getValueSerde()));

        // send all the command events (grouped by orderId) to the aggregate function
        orderCommandEvents.groupByKey()
                .aggregate(() -> null,
                        (aggKey, orderCommandEvent, aggregate) -> aggregateOrderCommandEvent(orderCommandEvent, aggregate),
                        Materialized.<String, Order, KeyValueStore<Bytes, byte[]>>as(ORDER_STORE_NAME)
                                .withKeySerde(ordersMeta.getKeySerde())
                                .withValueSerde(ordersMeta.getValueSerde()));

        return builder;
    }

    private Order aggregateOrderCommandEvent(SpecificRecord orderCommandEvent, Order aggregate) {
        if(orderCommandEvent instanceof OrderPlaced) {
            return aggregateOrderPlaced((OrderPlaced)orderCommandEvent, aggregate);
        }
        if(orderCommandEvent instanceof OrderRejected) {
            return aggregateOrderRejected((OrderRejected)orderCommandEvent, aggregate);
        }
        if(orderCommandEvent instanceof OrderAccepted) {
            return aggregateOrderAccepted((OrderAccepted)orderCommandEvent, aggregate);
        }
        if(orderCommandEvent instanceof OrderDeleted) {
            return aggregateOrderDeleted((OrderDeleted)orderCommandEvent, aggregate);
        }
        // else not expected
        log.warn("Unexpected commandEvent={} of type={}", orderCommandEvent, orderCommandEvent == null ? null : orderCommandEvent.getClass().getName());
        return aggregate;
    }

    private Order aggregateOrderPlaced(OrderPlaced orderPlaced, Order oldOrder) {
        // check to see if aggregate already created
        if(oldOrder != null) {
            log.warn("Received duplicate orderPlaced event={} for an existing order={}. Ignoring orderPlaced.",
                    orderPlaced, oldOrder);
            // return old order
            return oldOrder;
        }

        // aggregate did not exist. return new Order
        Order order = createOrder(orderPlaced);
        log.info("Received orderPlaced={}", orderPlaced);
        log.info("Created order={}", order);
        return order;
    }

    private Order aggregateOrderRejected(OrderRejected orderRejected, Order oldOrder) {
        // check to see if aggregate doesn't exist
        if( oldOrder == null ) {
            log.warn("Received orderRejected event={} for a non-existent orderId. Ignoring orderRejected.", orderRejected);
            // return null
            return null;
        }

        // aggregate does exist, verify state
        if( !"PLACED".equals(oldOrder.getState()) ) {
            log.warn("Received orderRejected event={} for order in unexpected state={}. Ignoring orderRejected.", orderRejected, oldOrder.getState());
            return oldOrder;
        }

        // we got here, oldOrder.state === "PLACED" ... safely change state
        Order order = updateOrderState(oldOrder, "REJECTED");
        log.info("Received orderRejected={}", orderRejected);
        log.info("Updated order={}", order);
        return order;
    }

    private Order aggregateOrderAccepted(OrderAccepted orderAccepted, Order oldOrder) {
        // check to see if aggregate doesn't exist
        if( oldOrder == null ) {
            log.warn("Received orderAccepted event={} for a non-existent orderId. Ignoring orderAccepted.", orderAccepted);
            // return null
            return null;
        }

        // aggregate does exist, verify state
        if( !"PLACED".equals(oldOrder.getState()) ) {
            log.warn("Received orderAccepted event={} for order in unexpected state={}. Ignoring orderAccepted.", orderAccepted, oldOrder.getState());
            return oldOrder;
        }

        // we got here, oldOrder.state === "PLACED" ... safely change state
        Order order = updateOrderState(oldOrder, "ACCEPTED");
        log.info("Received orderAccepted={}", orderAccepted);
        log.info("Updated order={}", order);
        return order;
    }

    private Order aggregateOrderDeleted(OrderDeleted orderDeleted, Order oldOrder) {
        // check to see if aggregate does exist
        if( oldOrder == null) {
            log.warn("Received orderDeleted event={} for a non-existent orderId. Ignoring orderDeleted.", orderDeleted);
            // return null
            return null;
        }

        // aggregate does exist, delete it!
        log.info("Deleting order={}", oldOrder);
        return null;
    }



    private Order createOrder(OrderPlaced orderPlaced) {
        DateTime now = DateTime.now();
        Order order = Order.newBuilder()
                .setOrderId(orderPlaced.getOrderId())
                .setId(orderPlaced.getId())
                .setCustomerId(orderPlaced.getCustomerId())
                .setItem(orderPlaced.getItem())
                .setState("PLACED")
                .setCreated(now)
                .setUpdated(now)
                .build();
        log.info("Received orderPlaced={}, order created={}", orderPlaced, order);
        return order;
    }

    private Order updateOrderState(Order order, String state) {
        DateTime now = DateTime.now();
        order.setState(state);
        order.setUpdated(now);
        return order;
    }

    @PostConstruct
    public void start() {
        // build the topology
        Topology topology = getStreamsBuilder().build();
        log.info("Starting {} kstreams", orderProcessorAppId);
        log.info("{}", topology.describe());

        // create our processor
        orderStreamProcessor = new KafkaStreams(topology, getKStreamConfig());

        // start your engines
        orderStreamProcessor.start();

        waitForStart();
    }

    public void waitForStart() {
        // hardcode wait for 60 seconds
        log.info("Waiting for orderStreamProcessor to start");
        long timeout = System.currentTimeMillis() + 60000;
        while(!orderStreamProcessor.state().isRunning() && System.currentTimeMillis() <= timeout) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException interruptedException) {
                throw new IllegalStateException("Interruped", interruptedException);
            }
        }
    }

    public ReadOnlyKeyValueStore<String, Order> getOrderStore() {
        return orderStreamProcessor.store(ORDER_STORE_NAME, QueryableStoreTypes.keyValueStore());
    }

    private Properties getKStreamConfig() {
        Properties props = new Properties();
        props.setProperty(StreamsConfig.APPLICATION_ID_CONFIG, orderProcessorAppId);
        props.setProperty(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.setProperty(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        props.setProperty(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, "100");
        return props;
    }

    @PreDestroy
    public void stop() {
        log.info("Shutting down orderStreamProcessor");
        orderStreamProcessor.close(Duration.ofSeconds(30));
    }
}
