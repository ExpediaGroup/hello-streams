package com.homeaway.streamplatform.hellostreams.ordercleaner.processor;

import com.google.common.collect.Lists;
import com.homeaway.streamplatform.hellostreams.Order;
import com.homeaway.streamplatform.hellostreams.ordercleaner.processor.StreamRegistry.StreamMetadata;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

@Component
@Slf4j
public class OrderCleaner {
    private static final String ORDER_STORE_NAME = "orders";

    @Autowired
    private StreamRegistry streamRegistry;

    @Value("${processor.id}")
    private String processorAppId;

    @Value("${processor.bootstrap.servers}")
    private String bootstrapServers;

    @Value("${processor.orders.stream}")
    private String ordersStream;

    private KafkaStreams streamProcessor = null;
    private ReadOnlyKeyValueStore<String, Order> readOnlyStore = null;

    public OrderCleaner() {}

    @SuppressWarnings("unchecked")
    private StreamsBuilder getStreamsBuilder() {
        StreamsBuilder builder = new StreamsBuilder();

        // lookup orderCommandEvents and orderDomainEvents
        StreamMetadata ordersMeta = streamRegistry.lookupStream(ordersStream);

        // setup state store
        builder.table(ordersMeta.getName(),
            getConsumedWith(ordersMeta),
            getMaterializedWith(ordersMeta, ORDER_STORE_NAME));

        return builder;
    }

    private <K,V> Consumed<K, V> getConsumedWith(StreamMetadata<K,V> streamMetadata) {
        return Consumed.with(streamMetadata.getKeySerde(), streamMetadata.getValueSerde());
    }

    @SuppressWarnings("SameParameterValue")
    private <K,V> Materialized<K,V, KeyValueStore<Bytes, byte[]>> getMaterializedWith(StreamMetadata<K,V> streamMetadata, String storeName) {
        return Materialized.<K,V, KeyValueStore<Bytes, byte[]>>as(storeName)
                .withKeySerde(streamMetadata.getKeySerde())
                .withValueSerde(streamMetadata.getValueSerde());
    }

    public List<Order> getAllOrders() {
        List<Order> orders = Lists.newArrayList();
        readOnlyStore.all().forEachRemaining( kv -> orders.add(kv.value) );
        return orders;
    }

    public Order getOrder(String orderId) {
        return readOnlyStore.get(orderId);
    }

    @SuppressWarnings("Duplicates")
    @PostConstruct
    public synchronized void start() {
        // build the topology
        Topology topology = getStreamsBuilder().build();
        log.info("Starting {} kstreams", processorAppId);
        log.info("{}", topology.describe());

        // create our processor
        streamProcessor = new KafkaStreams(topology, getKStreamConfig());

        // start your engines
        streamProcessor.start();

        waitForStart();
    }

    @SuppressWarnings("Duplicates")
    private void waitForStart() {
        Optional<String> notRunning = streamProcessor.localThreadsMetadata().stream()
                .map(m -> m.threadState())
                .filter(s -> !"RUNNING".equals(s))
                .findAny();
        if(notRunning.isEmpty() && readOnlyStore!=null) {
            return;
        }

        log.info("Waiting for streamProcessor to start");
        // hardcode wait for 60 seconds
        long timeout = System.currentTimeMillis() + 60000;
        while(notRunning.isPresent() && readOnlyStore==null && System.currentTimeMillis() <= timeout) {
            try { Thread.sleep(100); } catch (InterruptedException ignored) { }
            notRunning = streamProcessor.localThreadsMetadata().stream()
                    .map(m -> m.threadState())
                    .filter(s -> !"RUNNING".equals(s))
                    .findAny();

            // load up store
            try {
                readOnlyStore = streamProcessor.store(ORDER_STORE_NAME, QueryableStoreTypes.keyValueStore());
            } catch (Exception exception) {
                log.error("Could not load readOnlyStore. Retrying.", exception);
            }
        }
    }

    @PreDestroy
    public synchronized void stop() {
        log.info("Shutting down streamProcessor");
        streamProcessor.close(Duration.ofSeconds(30));
        // free the resources for garbage collection
        streamProcessor = null;
        readOnlyStore = null;
    }

    @SuppressWarnings("Duplicates")
    private Properties getKStreamConfig() {
        Properties props = new Properties();
        props.setProperty(StreamsConfig.APPLICATION_ID_CONFIG, processorAppId);
        props.setProperty(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.setProperty(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        props.setProperty(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, "100");
        props.setProperty(StreamsConfig.NUM_STREAM_THREADS_CONFIG, "1");
        return props;
    }
}
