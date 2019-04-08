package com.homeaway.streamplatform.hellostreams.orderprocessor.processor;

import com.homeaway.streamplatform.hellostreams.Order;
import com.homeaway.streamplatform.hellostreams.OrderAccepted;
import com.homeaway.streamplatform.hellostreams.OrderPlaced;
import com.homeaway.streamplatform.hellostreams.OrderRejected;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.ValueTransformer;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.state.KeyValueBytesStoreSupplier;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.StoreBuilder;
import org.apache.kafka.streams.state.Stores;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
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

    public StreamsBuilder buildStream() {
        StreamsBuilder builder = new StreamsBuilder();

        // setup order state
        StreamRegistry.StreamMetadata<String, SpecificRecord> ordersMeta = streamRegistry.lookupStream(ordersStream);
        KeyValueBytesStoreSupplier orderKeyValueStore = Stores.persistentKeyValueStore(ORDER_STORE_NAME);
        StoreBuilder orderStoreBuilder =
                Stores.keyValueStoreBuilder(orderKeyValueStore,
                        ordersMeta.getKeySerde(), ordersMeta.getValueSerde());
        builder.addStateStore(orderStoreBuilder);

        // setup input events
        StreamRegistry.StreamMetadata<String, SpecificRecord> orderCommandsMeta = streamRegistry.lookupStream(orderCommandsStream);
        KStream<String, SpecificRecord> orderCommandEvents =
                builder.stream(orderCommandsMeta.getName(),
                        Consumed.with(orderCommandsMeta.getKeySerde(), orderCommandsMeta.getValueSerde()));

        // order placed
        orderCommandEvents.filter((k,event) -> event instanceof OrderPlaced)
                .map((k,event) -> KeyValue.pair(k, (OrderPlaced)event))
                .selectKey((k,event) -> event.getOrderId())
                .transformValues(OrderPlacedTransformer::new, ORDER_STORE_NAME);

        // order rejected
        orderCommandEvents.filter((k,event) -> event instanceof OrderRejected)
                .map((k,event) -> KeyValue.pair(k, (OrderRejected)event))
                .selectKey((k,event) -> event.getOrderId())
                .transformValues(OrderRejectedTransformer::new, ORDER_STORE_NAME);

        // order rejected
        orderCommandEvents.filter((k,event) -> event instanceof OrderAccepted)
                .map((k,event) -> KeyValue.pair(k, (OrderAccepted)event))
                .selectKey((k,event) -> event.getOrderId())
                .transformValues(OrderAcceptedTransformer::new, ORDER_STORE_NAME);

        orderCommandEvents.filter((k,event) -> notCommandEvent(event))
                .foreach((k,event) -> warnAndIgnore(event));

        return builder;
    }

    private void warnAndIgnore(SpecificRecord record) {
        log.warn("Unexpected type={} for record={}. Ignoring.", record.getClass().getName(), record.toString());
    }

    private boolean notCommandEvent(SpecificRecord record) {
        return !(record instanceof OrderPlaced) &&
                !(record instanceof OrderRejected) &&
                !(record instanceof OrderAccepted);
    }

    @PostConstruct
    public void start() {
        // kstreams config
        Properties props = new Properties();
        props.setProperty(StreamsConfig.APPLICATION_ID_CONFIG, orderProcessorAppId);
        props.setProperty(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.setProperty(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());

        // build the topology
        Topology topology = buildStream().build();
        log.info("Topology");
        log.info("{}", topology.describe());

        // create our processor
        orderStreamProcessor = new KafkaStreams(topology, props);

        // start your engines
        orderStreamProcessor.start();
    }

    @PreDestroy
    public void stop() {
        orderStreamProcessor.close();
    }

    public static abstract class OrderCommandEventTransformer<V> implements ValueTransformer<V,Order> {
        protected KeyValueStore<String, Order> orderStore;

        @Override
        public void init(ProcessorContext context) {
            orderStore = (KeyValueStore<String, Order>) context.getStateStore(ORDER_STORE_NAME);
        }

        @Override
        public void close() { }
    }

    public static class OrderPlacedTransformer extends OrderCommandEventTransformer<OrderPlaced> {
        @Override
        public Order transform(OrderPlaced orderPlaced) {
            log.info("Looking up Order key={}", orderPlaced.getOrderId());
            Order oldOrder = orderStore.get(orderPlaced.getOrderId());
            if(oldOrder != null) {
                log.warn("Received orderPlaced event={} for an existing order={}. Ignoring orderPlaced.");
                // return old order
                return oldOrder;
            }
            // did not exist. return new Order
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
            orderStore.put(order.getOrderId(), order);

            return order;
        }
    }

    public static class OrderRejectedTransformer extends OrderCommandEventTransformer<OrderRejected> {

        @Override
        public Order transform(OrderRejected orderRejected) {
            log.info("Looking up Order key={}", orderRejected.getOrderId());
            Order order = orderStore.get(orderRejected.getOrderId());
            if(order == null) {
                log.error("Could not find order with orderId={}. Dropping orderRejected={}", orderRejected.getOrderId(), orderRejected);
                return null;
            }
            if(!order.getState().equals("PLACED")) {
                log.error("Received orderRejected event={} but existing order has state={}, expected state=\"PLACED\". Ignoring orderRejected.",
                        orderRejected, order.getState());
                return null;
            }

            order.setId(orderRejected.getId());
            order.setState("REJECTED");
            order.setUpdated(DateTime.now());
            log.info("Received orderRejected={}, order rejected={}", orderRejected, order);
            orderStore.put(order.getOrderId(), order);

            return order;
        }
    }

    public static class OrderAcceptedTransformer extends OrderCommandEventTransformer<OrderAccepted> {
        @Override
        public Order transform(OrderAccepted orderAccepted) {
            log.info("Looking up Order key={}", orderAccepted.getOrderId());
            Order order = orderStore.get(orderAccepted.getOrderId());
            if(order == null) {
                log.error("Could not find order with orderId={}. Dropping orderAccepted={}", orderAccepted.getOrderId(), orderAccepted);
                return null;
            }
            if(!order.getState().equals("PLACED")) {
                log.error("Received orderAccepted event={} but existing order has state={}, expected state=\"PLACED\". Ignoring orderAccepted.",
                        orderAccepted, order.getState());
                return null;
            }

            order.setId(orderAccepted.getId());
            order.setState("ACCEPTED");
            order.setUpdated(DateTime.now());
            log.info("Received orderAccepted={}, order accepted={}", orderAccepted, order);
            orderStore.put(order.getOrderId(), order);

            return order;
        }
    }
}
