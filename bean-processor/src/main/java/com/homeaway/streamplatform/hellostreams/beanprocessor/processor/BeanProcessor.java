package com.homeaway.streamplatform.hellostreams.beanprocessor.processor;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.homeaway.streamplatform.hellostreams.BeanSupplied;
import com.homeaway.streamplatform.hellostreams.BeanSupply;
import com.homeaway.streamplatform.hellostreams.BeanSupplyAccepted;
import com.homeaway.streamplatform.hellostreams.BeanSupplyRejected;
import com.homeaway.streamplatform.hellostreams.BeanSupplyRequested;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Aggregator;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.processor.ThreadMetadata;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;

@SuppressWarnings("unused")
@Component
@Slf4j
public class BeanProcessor {
    public static final String GLOBAL_KEY = "beans";
    private static final String BEAN_STORE_NAME="beans";

    private final StreamRegistry streamRegistry;

    @Value("${processor.id}")
    private String processorAppId;

    @Value("${processor.bootstrap.servers}")
    private String bootstrapServers;

    @Value("${processor.bean.commands.stream}")
    private String beanCommandsStream;

    @Value("${processor.beans.stream}")
    private String beansStream;

    @Value("${processor.beans.initialBeansAvailable}")
    private Integer initialBeansAvailable;

    private KafkaStreams streamProcessor = null;
    private ReadOnlyKeyValueStore<String, BeanSupply> readOnlyStore = null;
    private Map<String, Aggregator<String, SpecificRecord, BeanSupply>> functionMap = Maps.newHashMap();

    @Autowired
    public BeanProcessor(StreamRegistry streamRegistry) {
        Preconditions.checkNotNull(streamRegistry, "streamRegistry cannot be null");

        this.streamRegistry = streamRegistry;

        // setup functionMap
        functionMap.put(BeanSupplied.class.getName(), this::beanSupplied);
        functionMap.put(BeanSupplyAccepted.class.getName(), this::beanAccepted);
        functionMap.put(BeanSupplyRejected.class.getName(), this::beanRejected);
    }

    public BeanSupply getBeanSupply(String key) {
        return readOnlyStore.get(key);
    }
    
    @SuppressWarnings("unchecked")
    private StreamsBuilder getStreamsBuilder() {
        StreamsBuilder builder = new StreamsBuilder();

        // lookup beanCommandEvents and beanDomainEvents
        StreamRegistry.StreamMetadata beanCommandsMeta = streamRegistry.lookupStream(beanCommandsStream);
        StreamRegistry.StreamMetadata beansMeta = streamRegistry.lookupStream(beansStream);

        // setup input events - multi-type
        KStream<String, SpecificRecord> beanCommandEvents =
                builder.stream(beanCommandsMeta.getName(),
                        getConsumedWith(beanCommandsMeta));

        // handle beanSupplyRequested (results in accepted or rejected commands)
        beanCommandEvents
                .filter((k, v) -> v instanceof BeanSupplyRequested)
                .mapValues(this::beanRequested)
                .to(beanCommandsMeta.getName(),getProducedWith(beanCommandsMeta));

        // handle other command events
        beanCommandEvents
                // filter non requested command events
                .filter((k, v) -> !(v instanceof BeanSupplyRequested))
                // group by key
                .groupByKey()
                // aggregate state!
                .aggregate(this::initBeanSupply,
                        this::aggregateBeanCommandEvent,
                        getMaterializedWith(beansMeta, BEAN_STORE_NAME))
;
 /*               // write to domain events
                .toStream()
                .to(beansMeta.getName(),getProducedWith(beansMeta));
                */

        return builder;
    }

    /** called to initialize the bean supply */
    private BeanSupply initBeanSupply() {
        DateTime now = DateTime.now();
        return BeanSupply.newBuilder()
                .setId(UUID.randomUUID().toString())
                .setBeansAvailable(initialBeansAvailable) // set to initialBeansAvailable
                .setCreated(now)
                .setUpdated(now)
                .build();
    }

    private SpecificRecord beanRequested(SpecificRecord beanCommandEvent) {
        BeanSupply beanSupply = readOnlyStore.get(GLOBAL_KEY);
        Preconditions.checkState(beanSupply!=null, "Unexpected beanSupply==null");
        BeanSupplyRequested beanSupplyRequested = (BeanSupplyRequested) beanCommandEvent;
        int requestedBeans = beanSupplyRequested.getBeansRequested();
        if(beanSupply.getBeansAvailable() >= requestedBeans) {
            log.info("Accepting beanSupplyRequested event of {} beans", requestedBeans);
            return buildBeanSupplyAccepted(beanSupplyRequested);
        }
        log.info("Rejecting beanSupplyRequested event of {} beans", requestedBeans);
        return buildBeanSupplyRejected(beanSupplyRequested);
    }

    private BeanSupplyAccepted buildBeanSupplyAccepted(BeanSupplyRequested beanSupplyRequested) {
        return BeanSupplyAccepted.newBuilder()
                .setId(UUID.randomUUID().toString())
                .setOrderId(beanSupplyRequested.getOrderId())
                .setBeansAccepted(beanSupplyRequested.getBeansRequested())
                .setCreated(DateTime.now())
                .build();
    }

    private BeanSupplyRejected buildBeanSupplyRejected(BeanSupplyRequested beanSupplyRequested) {
        return BeanSupplyRejected.newBuilder()
                .setId(UUID.randomUUID().toString())
                .setOrderId(beanSupplyRequested.getOrderId())
                .setBeansRejected(beanSupplyRequested.getBeansRequested())
                .setCreated(DateTime.now())
                .build();
    }

    private BeanSupply aggregateBeanCommandEvent(String key, SpecificRecord beanCommandEvent, BeanSupply beanSupplyAggregate) {
        Aggregator<String, SpecificRecord, BeanSupply> eventFunction = functionMap.get(beanCommandEvent.getClass().getName());
        if(eventFunction==null) {
            log.error("Unexpected event={} received.", beanCommandEvent.getClass().getName());
            return beanSupplyAggregate;
        }
        return eventFunction.apply(key, beanCommandEvent, beanSupplyAggregate);
    }

    private BeanSupply beanSupplied(String key, SpecificRecord beanCommandEvent, BeanSupply beanSupply) {
        Preconditions.checkState(beanSupply!=null, "Unexpected beanSupply==null");
        BeanSupplied beanSupplied = (BeanSupplied) beanCommandEvent;
        beanSupply.setId(beanSupplied.getId());
        beanSupply.setBeansAvailable(beanSupply.getBeansAvailable() + beanSupplied.getBeansSupplied());
        beanSupply = updateBeanSupplyDate(beanSupply);

        log.info("Increasing beansAvailable by {} to {}", beanSupplied.getBeansSupplied(), beanSupply.getBeansAvailable());
        log.info("beanSupply={}", beanSupply);
        return beanSupply;
    }

    private BeanSupply beanAccepted(String key, SpecificRecord beanCommandEvent, BeanSupply beanSupply) {
        Preconditions.checkState(beanSupply!=null, "Unexpected beanSupply==null");
        BeanSupplyAccepted accepted = (BeanSupplyAccepted) beanCommandEvent;
        beanSupply.setId(accepted.getId());
        beanSupply.setBeansAvailable( beanSupply.getBeansAvailable() - accepted.getBeansAccepted() );
        beanSupply = updateBeanSupplyDate(beanSupply);

        log.info("Reducing beansAvailable by {} to {}", accepted.getBeansAccepted(), beanSupply.getBeansAvailable());
        log.info("beanSupply={}", beanSupply);
        return beanSupply;
    }

    private BeanSupply beanRejected(String key, SpecificRecord beanCommandEvent, BeanSupply beanSupply) {
        Preconditions.checkState(beanSupply!=null, "Unexpected beanSupply==null");
        BeanSupplyRejected rejected = (BeanSupplyRejected) beanCommandEvent;
        log.info("Rejecting bean requested event for {} beans", rejected.getBeansRejected());

        // no change
        return beanSupply;
    }

    private BeanSupply updateBeanSupplyDate(BeanSupply beanSupply) {
        beanSupply.setUpdated(DateTime.now());
        return beanSupply;
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
                readOnlyStore = streamProcessor.store(BEAN_STORE_NAME, QueryableStoreTypes.keyValueStore());
            } catch (Exception exception) {
                log.info("Could not load readOnlyStore. Retrying.", exception);
            }
        }

        Preconditions.checkState(readOnlyStore!=null, "Could not load readOnlyStore due to timeout!");
    }

    private Optional<String> getNotRunningThreads() {
        return streamProcessor.localThreadsMetadata().stream()
                .map(ThreadMetadata::threadState)
                .filter(s -> !"RUNNING".equals(s))
                .findAny();
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

    private Consumed<String, SpecificRecord> getConsumedWith(StreamRegistry.StreamMetadata<String,SpecificRecord> streamMetadata) {
        return Consumed.with(streamMetadata.getKeySerde(), streamMetadata.getValueSerde());
    }

    private Produced<String, SpecificRecord> getProducedWith(StreamRegistry.StreamMetadata<String,SpecificRecord> streamMetadata) {
        return Produced.with(streamMetadata.getKeySerde(), streamMetadata.getValueSerde());
    }

    @SuppressWarnings("SameParameterValue")
    private Materialized<String,BeanSupply, KeyValueStore<Bytes, byte[]>> getMaterializedWith(StreamRegistry.StreamMetadata<String,BeanSupply> streamMetadata, String storeName) {
        return Materialized.<String, BeanSupply, KeyValueStore<Bytes, byte[]>>as(storeName)
                .withKeySerde(streamMetadata.getKeySerde())
                .withValueSerde(streamMetadata.getValueSerde());
    }
}
