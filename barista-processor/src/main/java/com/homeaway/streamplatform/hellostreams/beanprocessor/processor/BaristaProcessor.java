package com.homeaway.streamplatform.hellostreams.beanprocessor.processor;

import com.google.common.base.Preconditions;
import com.homeaway.streamplatform.hellostreams.Order;
import com.homeaway.streamplatform.hellostreams.OrderCompleted;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Produced;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.Duration;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings({"unused", "Duplicates"})
@Component
@Slf4j
public class BaristaProcessor {
    private final StreamRegistry streamRegistry;

    @Value("${processor.id}")
    private String processorAppId;

    @Value("${processor.bootstrap.servers}")
    private String bootstrapServers;

    @Value("${processor.order.commands.stream}")
    private String orderCommandsStream;

    @Value("${processor.orders.stream}")
    private String ordersStream;

    @Value("${processor.barista.wait.ms}")
    private long baristaWaitMs;

    private KafkaStreams streamProcessor = null;
    private AtomicBoolean isBrewing = new AtomicBoolean(false);

    @Autowired
    public BaristaProcessor(StreamRegistry streamRegistry) {
        Preconditions.checkNotNull(streamRegistry, "streamRegistry cannot be null");

        this.streamRegistry = streamRegistry;
    }

    @SuppressWarnings("unchecked")
    private StreamsBuilder getStreamsBuilder() {
        StreamsBuilder builder = new StreamsBuilder();

        // lookup beanCommandEvents and beanDomainEvents
        StreamRegistry.StreamMetadata<String, SpecificRecord> orderCommandsMeta = streamRegistry.lookupStream(orderCommandsStream);
        StreamRegistry.StreamMetadata<String, Order> ordersMeta = streamRegistry.lookupStream(ordersStream);

        // filter accepted orders
        builder
                .stream(ordersMeta.getName(), Consumed.with(ordersMeta.getKeySerde(), ordersMeta.getValueSerde()))
                .filter( (k,v) -> (v!=null && "ACCEPTED".equals(v.getState())))
                // process orders
                .mapValues(this::processOrder)
                .to(orderCommandsMeta.getName(), Produced.with(orderCommandsMeta.getKeySerde(), orderCommandsMeta.getValueSerde()));

        return builder;
    }

    private SpecificRecord processOrder(Order order) {
        log.info("Brewing order={}", order);

        try {
            startBrewing();
            Thread.sleep(baristaWaitMs);  // brew time
            stopBrewing();

            Thread.sleep(baristaWaitMs/4);  // sleep time = brew time / 4
        } catch (InterruptedException ignored) {}

        OrderCompleted orderCompleted = OrderCompleted.newBuilder()
                .setId(UUID.randomUUID().toString())
                .setOrderId(order.getOrderId())
                .setCreated(DateTime.now())
                .build();
        log.info("Sending orderCompleted={}", orderCompleted);
        return orderCompleted;
    }

    private void startBrewing() {
        isBrewing.set(true);
    }

    public boolean isProcessing() {
        return isBrewing.get();
    }

    private void stopBrewing() {
        isBrewing.set(false);
    }


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
    }

    @PreDestroy
    public synchronized void stop() {
        log.info("Shutting down streamProcessor");
        streamProcessor.close(Duration.ofSeconds(30));
        // free the resources for garbage collection
        streamProcessor = null;
    }

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
