package com.homeaway.streamplatform.hellostreams.orderprocessor.processor;

import avro.shaded.com.google.common.collect.Lists;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.homeaway.streamplatform.hellostreams.BeanSupplyAccepted;
import com.homeaway.streamplatform.hellostreams.BeanSupplyRejected;
import com.homeaway.streamplatform.hellostreams.BeanSupplyRequested;
import com.homeaway.streamplatform.hellostreams.Order;
import com.homeaway.streamplatform.hellostreams.OrderAccepted;
import com.homeaway.streamplatform.hellostreams.OrderDeleted;
import com.homeaway.streamplatform.hellostreams.OrderPlaced;
import com.homeaway.streamplatform.hellostreams.OrderRejected;
import com.homeaway.streamplatform.hellostreams.orderprocessor.OrderProcessorUtils;
import com.homeaway.streamplatform.hellostreams.orderprocessor.processor.StreamRegistry.StreamMetadata;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.processor.ThreadMetadata;
import org.apache.kafka.streams.state.KeyValueIterator;
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
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;

@SuppressWarnings("Duplicates")
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

    @Value("${order-processor.bean.commands.stream}")
    private String beanCommandsStream;

    private Map<String,Integer> itemBeanMap = Maps.newHashMap();
    private KafkaStreams orderStreamProcessor;
    private ReadOnlyKeyValueStore<String, com.homeaway.streamplatform.hellostreams.Order> readOnlyStore;

    public OrderStreamProcessor() {
        itemBeanMap.put("Brewed Coffee", 5);
        itemBeanMap.put("Espresso", 8);
        itemBeanMap.put("Latte", 6);
        itemBeanMap.put("Ice Coffee", 4);
    }

    @SuppressWarnings({"WeakerAccess", "unchecked"})
    public StreamsBuilder getStreamsBuilder() {
        StreamsBuilder builder = new StreamsBuilder();

        // lookup command (orderCommands) and domain (orders) events
        StreamMetadata<String, SpecificRecord> orderCommandsMeta = streamRegistry.lookupStream(orderCommandsStream);
        StreamMetadata<String, Order> ordersMeta = streamRegistry.lookupStream(ordersStream);
        StreamMetadata<String, SpecificRecord> beanCommandsMeta = streamRegistry.lookupStream(beanCommandsStream);

        // setup input events - multi-type
        builder
                .stream(orderCommandsMeta.getName(),
                        Consumed.with(orderCommandsMeta.getKeySerde(),
                                orderCommandsMeta.getValueSerde()))
                 // send all the command events (grouped by orderId) to the aggregate function
                .groupByKey()
                .aggregate(() -> null,
                        (aggKey, orderCommandEvent, aggregate) -> aggregateOrderCommandEvent(orderCommandEvent, aggregate),
                        Materialized.<String, Order, KeyValueStore<Bytes, byte[]>>as(ORDER_STORE_NAME)
                                .withKeySerde(ordersMeta.getKeySerde())
                                .withValueSerde(ordersMeta.getValueSerde()))
                // send to orders stream
                .toStream()
                .through(ordersMeta.getName(), Produced.with(ordersMeta.getKeySerde(), ordersMeta.getValueSerde()))
                // filter orders in placed state
                .filter((k,v)-> v!=null && "PLACED".equals(v.getState()))
                // create bean supply requested command event
                .map(this::getBeanSupplyRequested)
                .to(beanCommandsMeta.getName(), Produced.with(beanCommandsMeta.getKeySerde(), beanCommandsMeta.getValueSerde()));

        // setup input events - bean command events
        builder
                .stream(beanCommandsMeta.getName(), Consumed.with(beanCommandsMeta.getKeySerde(), beanCommandsMeta.getValueSerde()))
                // filter accepted or rejected bean events
                .filter(this::beanSupplyAcceptedOrRejected)
                // map to order accepted or rejected bean events
                .map(this::mapOrderAcceptedOrRejected)
                .to(orderCommandsMeta.getName(), Produced.with(orderCommandsMeta.getKeySerde(), orderCommandsMeta.getValueSerde()));

        return builder;
    }

    private boolean beanSupplyAcceptedOrRejected(String key, SpecificRecord beanCommandEvent) {
        return (beanCommandEvent instanceof BeanSupplyAccepted)
                || (beanCommandEvent instanceof BeanSupplyRejected);
    }
    
    private KeyValue<String,SpecificRecord> mapOrderAcceptedOrRejected(String key, SpecificRecord beanCommandEvent) {
        if(beanCommandEvent instanceof BeanSupplyAccepted) {
            return beanSupplyAccepted(key, (BeanSupplyAccepted)beanCommandEvent);
        }
        return beanSupplyRejected(key, (BeanSupplyRejected)beanCommandEvent);
    }

    private KeyValue<String, SpecificRecord> beanSupplyAccepted(String key, BeanSupplyAccepted beanCommandEvent) {
        OrderAccepted orderAccepted = OrderAccepted.newBuilder()
                .setId(UUID.randomUUID().toString())
                .setOrderId(beanCommandEvent.getOrderId())
                .setCreated(DateTime.now())
                .build();
        log.info("Accepting order={}", orderAccepted);
        // order by orderId
        return KeyValue.pair(orderAccepted.getOrderId(), orderAccepted);
    }

    private KeyValue<String, SpecificRecord> beanSupplyRejected(String key, BeanSupplyRejected beanCommandEvent) {
        OrderRejected orderRejected = OrderRejected.newBuilder()
                .setId(UUID.randomUUID().toString())
                .setOrderId(beanCommandEvent.getOrderId())
                .setCreated(DateTime.now())
                .build();
        log.info("Rejecting order={}", orderRejected);
        // order by orderId
        return KeyValue.pair(orderRejected.getOrderId(), orderRejected);
    }

    private KeyValue<String, SpecificRecord> getBeanSupplyRequested(String key, Order order) {
        BeanSupplyRequested beanSupplyRequested = BeanSupplyRequested.newBuilder()
                .setId(UUID.randomUUID().toString())
                .setOrderId(order.getOrderId())
                .setBeansRequested(lookupBeans(order.getItem()))
                .setCreated(DateTime.now())
                .build();
        log.info("Requesting {}", beanSupplyRequested);
        return KeyValue.pair("beans", beanSupplyRequested);
    }

    private int lookupBeans(String item) {
        if(!itemBeanMap.containsKey(item)) {
            log.error("Unexpected item {} received. Returning 0 beans requested", item);
            return 0;
        }
        return itemBeanMap.get(item);
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

    private void waitForStart() {
        Optional<String> notRunning = getNotRunningThreads();
        if(notRunning.isEmpty() && readOnlyStore!=null) {
            return;
        }

        log.info("Waiting for streamProcessor to start");
        // hardcode wait for 60 seconds
        long timeout = System.currentTimeMillis() + 60000;
        while(notRunning.isPresent() && readOnlyStore==null && System.currentTimeMillis() <= timeout) {
            try { Thread.sleep(100); } catch (InterruptedException ignored) { }
            notRunning = getNotRunningThreads();

            // load up store
            try {
                readOnlyStore = orderStreamProcessor.store(ORDER_STORE_NAME, QueryableStoreTypes.keyValueStore());
            } catch (Exception exception) {
                log.info("Could not load readOnlyStore. Retrying.", exception);
            }
        }

        Preconditions.checkState(readOnlyStore!=null, "Could not load readOnlyStore due to timeout!");
    }

    private Optional<String> getNotRunningThreads() {
        return orderStreamProcessor.localThreadsMetadata().stream()
                .map(ThreadMetadata::threadState)
                .filter(s -> !"RUNNING".equals(s))
                .findAny();
    }

    private Properties getKStreamConfig() {
        Properties props = new Properties();
        props.setProperty(StreamsConfig.APPLICATION_ID_CONFIG, orderProcessorAppId);
        props.setProperty(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.setProperty(StreamsConfig.NUM_STREAM_THREADS_CONFIG, "1");
        props.setProperty(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        props.setProperty(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, "100");
        return props;
    }

    @PreDestroy
    public void stop() {
        log.info("Shutting down orderStreamProcessor");
        orderStreamProcessor.close(Duration.ofSeconds(30));
    }

    public List<com.homeaway.streamplatform.hellostreams.orderprocessor.model.Order> findAllOrders() {
        List<com.homeaway.streamplatform.hellostreams.orderprocessor.model.Order> orders = Lists.newArrayList();
        try ( KeyValueIterator<String, Order> rawOrders
                      = readOnlyStore.all() ) {
            rawOrders.forEachRemaining( keyValue -> orders.add(toDTO(keyValue.value)) );
        }
        return orders;
    }

    public com.homeaway.streamplatform.hellostreams.orderprocessor.model.Order getOrder(String orderId) {
        return toDTO(readOnlyStore.get(orderId));
    }

    private com.homeaway.streamplatform.hellostreams.orderprocessor.model.Order toDTO(com.homeaway.streamplatform.hellostreams.Order orderAvro) {
        if(orderAvro == null) {
            return null;
        }
        com.homeaway.streamplatform.hellostreams.orderprocessor.model.Order order = new com.homeaway.streamplatform.hellostreams.orderprocessor.model.Order();

        order.setOrderId(orderAvro.getOrderId());
        order.setId(orderAvro.getId());
        order.setCustomerId(orderAvro.getCustomerId());
        order.setItem(orderAvro.getItem());
        order.setState(orderAvro.getState());
        order.setCreated(toDTOTime(orderAvro.getCreated()));
        order.setUpdated(toDTOTime(orderAvro.getUpdated()));

        return order;
    }

    public ZonedDateTime toDTOTime(DateTime avroTime) {
        return ZonedDateTime.ofInstant(Instant.ofEpochMilli(avroTime.getMillis()),
                OrderProcessorUtils.UTC_ZONE_ID);
    }
}
